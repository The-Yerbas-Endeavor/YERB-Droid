package org.yerbas.wallet.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Downloads and connects Yerbas headers in repeated batches of up to 2,000. */
public final class HeaderSynchronizer implements AutoCloseable {
    public record Progress(int localHeight, int remoteHeight, int batchSize, boolean complete) {}

    private final NetworkParameters network;
    private final PeerConnection peer;
    private final HeaderStore store;
    private final HeaderChain chain;
    private final CompletableFuture<HeaderStore.StoredHeader> completion = new CompletableFuture<>();
    private final AtomicBoolean requestOutstanding = new AtomicBoolean();
    private volatile Consumer<Progress> progressListener = ignored -> { };
    private volatile int remoteHeight;

    public HeaderSynchronizer(NetworkParameters network, PeerConnection peer,
                              HeaderStore store, HeaderChain chain) {
        this.network = Objects.requireNonNull(network);
        this.peer = Objects.requireNonNull(peer);
        this.store = Objects.requireNonNull(store);
        this.chain = Objects.requireNonNull(chain);
        peer.setMessageConsumer(this::onMessage);
    }

    public void setProgressListener(Consumer<Progress> listener) {
        progressListener = Objects.requireNonNull(listener);
    }

    public CompletableFuture<HeaderStore.StoredHeader> start(int announcedRemoteHeight) {
        if (announcedRemoteHeight < 0) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("negative remote height"));
        }
        remoteHeight = announcedRemoteHeight;
        requestNextBatch();
        return completion;
    }

    private void onMessage(WireMessage message) {
        try {
            switch (message.command()) {
                case "headers" -> handleHeaders(message.payload());
                case "reject" -> fail(new IllegalStateException("Peer rejected header request"));
                default -> { /* ignore unrelated messages while syncing headers */ }
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void handleHeaders(byte[] payload) {
        if (!requestOutstanding.compareAndSet(true, false)) {
            throw new IllegalStateException("unsolicited headers response");
        }
        List<BlockHeader> headers = HeaderMessages.decodeHeaders(payload);
        chain.connectAll(headers);
        HeaderStore.StoredHeader best = store.best()
                .orElseThrow(() -> new IllegalStateException("header store has no best tip"));
        boolean complete = headers.size() < 2_000 || best.height() >= remoteHeight;
        progressListener.accept(new Progress(best.height(), remoteHeight, headers.size(), complete));
        if (complete) {
            completion.complete(best);
        } else {
            requestNextBatch();
        }
    }

    private void requestNextBatch() {
        if (completion.isDone()) return;
        if (!requestOutstanding.compareAndSet(false, true)) {
            fail(new IllegalStateException("header request already outstanding"));
            return;
        }
        List<byte[]> locator = BlockLocator.build(store);
        byte[] payload = HeaderMessages.encodeGetHeaders(
                network.protocolVersion(), locator, new byte[32]);
        peer.send("getheaders", payload).whenComplete((ignored, failure) -> {
            if (failure != null) {
                requestOutstanding.set(false);
                fail(failure);
            }
        });
    }

    private void fail(Throwable failure) {
        completion.completeExceptionally(failure);
        close();
    }

    @Override
    public void close() {
        peer.close();
    }
}
