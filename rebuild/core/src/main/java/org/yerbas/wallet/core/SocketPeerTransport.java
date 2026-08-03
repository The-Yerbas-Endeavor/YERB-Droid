package org.yerbas.wallet.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Blocking-socket transport with a dedicated reader thread and serialized writes. */
public final class SocketPeerTransport implements PeerTransport {
    private final NetworkParameters network;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ExecutorService io = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "yerbas-peer-io");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private volatile Listener listener;
    private volatile Socket socket;
    private volatile OutputStream output;

    public SocketPeerTransport(NetworkParameters network, int connectTimeoutMillis, int readTimeoutMillis) {
        this.network = Objects.requireNonNull(network);
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    @Override
    public CompletableFuture<Void> connect(String host, int port) {
        return CompletableFuture.runAsync(() -> {
            disconnect();
            try {
                Socket connected = new Socket();
                connected.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
                connected.setSoTimeout(readTimeoutMillis);
                connected.setTcpNoDelay(true);
                socket = connected;
                output = connected.getOutputStream();
                closed.set(false);
                Listener current = listener;
                if (current != null) current.onConnected();
                io.execute(() -> readLoop(connected));
            } catch (IOException e) {
                disconnectInternal(e);
                throw new IllegalStateException("Unable to connect to Yerbas peer " + host + ':' + port, e);
            }
        }, io);
    }

    @Override
    public CompletableFuture<Void> send(String command, byte[] payload) {
        return CompletableFuture.runAsync(() -> {
            OutputStream current = output;
            if (closed.get() || current == null) throw new IllegalStateException("Peer is not connected");
            byte[] bytes = new WireMessage(command, payload).serialize(network);
            synchronized (this) {
                try {
                    current.write(bytes);
                    current.flush();
                } catch (IOException e) {
                    disconnectInternal(e);
                    throw new IllegalStateException("Unable to write Yerbas peer message", e);
                }
            }
        }, io);
    }

    private void readLoop(Socket connected) {
        PeerMessageStream stream = new PeerMessageStream(network);
        byte[] chunk = new byte[64 * 1024];
        try (InputStream input = connected.getInputStream()) {
            while (!closed.get()) {
                int count = input.read(chunk);
                if (count < 0) throw new IOException("Yerbas peer closed the connection");
                for (WireMessage message : stream.append(chunk, 0, count)) {
                    Listener current = listener;
                    if (current != null) current.onMessage(message);
                }
            }
        } catch (Throwable failure) {
            disconnectInternal(failure);
        }
    }

    @Override
    public void disconnect() {
        disconnectInternal(null);
    }

    private void disconnectInternal(Throwable cause) {
        if (!closed.compareAndSet(false, true) && socket == null) return;
        Socket current = socket;
        socket = null;
        output = null;
        if (current != null) {
            try { current.close(); } catch (IOException ignored) { }
        }
        Listener currentListener = listener;
        if (currentListener != null) currentListener.onDisconnected(cause);
    }
}
