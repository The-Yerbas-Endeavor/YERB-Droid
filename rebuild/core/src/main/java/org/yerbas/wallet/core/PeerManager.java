package org.yerbas.wallet.core;

import java.util.HashSet;
import java.util.Set;

/** Coordinates known peers without coupling to Android networking. */
public final class PeerManager {
    private final Set<String> peers = new HashSet<>();

    public void addPeer(String host) {
        if (host != null && !host.isBlank()) peers.add(host);
    }

    public Set<String> peers() {
        return Set.copyOf(peers);
    }
}
