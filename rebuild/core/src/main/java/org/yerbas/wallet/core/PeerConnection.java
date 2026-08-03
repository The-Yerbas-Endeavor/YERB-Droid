package org.yerbas.wallet.core;

import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Coordinates transport, handshake, liveness and message dispatch for one peer. */
public final class PeerConnection implements PeerTransport.Listener, AutoCloseable {
    private final NetworkParameters network;
    private final PeerTransport transport;
    private final PeerSession session;
    private final CompletableFuture<VersionPayload> ready = new CompletableFuture<>();
    private final AtomicBoolean sentVerack = new AtomicBoolean();
    private volatile Consumer<WireMessage> messageConsumer = ignored -> { };
    private volatile VersionPayload remoteVersion;

    public PeerConnection(NetworkParameters network, PeerTransport transport) {
        this.network = Objects.requireNonNull(network);
        this.transport = Objects.requireNonNull(transport);
        this.session = new PeerSession(network);
        transport.setListener(this);
    }

    public void setMessageConsumer(Consumer<WireMessage> consumer) {
        messageConsumer = Objects.requireNonNull(consumer);
    }

    public CompletableFuture<VersionPayload> connect(InetSocketAddress address, int localHeight) {
        return transport.connect(address.getHostString(), address.getPort()).thenCompose(ignored -> {
            VersionPayload local = new VersionPayload(
                    network.protocolVersion(), 0, Instant.now().getEpochSecond(),
                    VersionPayload.unspecified(address.getPort()),
                    VersionPayload.unspecified(network.port()),
                    new SecureRandom().nextLong(), "/YERB-Droid:0.1.0/", localHeight, false);
            session.markVersionSent();
            return transport.send("version", local.encode()).thenCompose(v -> ready);
        });
    }

    @Override public void onConnected() { }

    @Override
    public void onMessage(WireMessage message) {
        try {
            switch (message.command()) {
                case "version" -> {
                    VersionPayload decoded = VersionPayload.decode(message.payload());
                    session.receiveVersion(decoded.protocolVersion());
                    remoteVersion = decoded;
                    if (sentVerack.compareAndSet(false, true)) transport.send("verack", new byte[0]);
                }
                case "verack" -> {
                    session.receiveVerack();
                    ready.complete(remoteVersion);
                }
                case "ping" -> transport.send("pong", message.payload());
                default -> messageConsumer.accept(message);
            }
        } catch (Throwable failure) {
            ready.completeExceptionally(failure);
            close();
        }
    }

    @Override
    public void onDisconnected(Throwable cause) {
        session.close();
        if (!ready.isDone()) {
            ready.completeExceptionally(cause == null ? new IllegalStateException("Peer disconnected") : cause);
        }
    }

    public CompletableFuture<Void> send(String command, byte[] payload) {
        if (session.state() != PeerSession.State.READY) {
            return CompletableFuture.failedFuture(new IllegalStateException("Peer handshake is not complete"));
        }
        return transport.send(command, payload);
    }

    public PeerSession.State state() { return session.state(); }

    @Override public void close() { transport.disconnect(); }
}
