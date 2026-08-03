package org.yerbas.wallet.core;

import java.util.concurrent.CompletableFuture;

/** Transport abstraction separating networking from peer and chain logic. */
public interface PeerTransport extends AutoCloseable {
    interface Listener {
        void onConnected();
        void onMessage(WireMessage message);
        void onDisconnected(Throwable cause);
    }

    void setListener(Listener listener);
    CompletableFuture<Void> connect(String host, int port);
    CompletableFuture<Void> send(String command, byte[] payload);
    void disconnect();

    @Override
    default void close() {
        disconnect();
    }
}
