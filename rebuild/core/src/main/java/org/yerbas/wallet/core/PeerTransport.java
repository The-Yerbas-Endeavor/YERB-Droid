package org.yerbas.wallet.core;

import java.util.concurrent.CompletableFuture;

/** Transport abstraction separating networking from chain logic. */
public interface PeerTransport {
    CompletableFuture<Void> connect(String host, int port);
    CompletableFuture<Void> send(String command, byte[] payload);
    void disconnect();
}
