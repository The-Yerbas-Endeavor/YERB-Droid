package org.yerbas.wallet.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Resolves and deduplicates the configured Yerbas DNS seeds. */
public final class DnsSeedResolver {
    private final NetworkParameters network;

    public DnsSeedResolver(NetworkParameters network) {
        this.network = network;
    }

    public List<InetSocketAddress> resolve() {
        Set<String> seen = new LinkedHashSet<>();
        List<InetSocketAddress> peers = new ArrayList<>();
        List<String> seeds = new ArrayList<>(network.dnsSeeds());
        Collections.shuffle(seeds);
        for (String seed : seeds) {
            try {
                for (InetAddress address : InetAddress.getAllByName(seed)) {
                    String key = address.getHostAddress();
                    if (seen.add(key)) peers.add(new InetSocketAddress(address, network.port()));
                }
            } catch (UnknownHostException ignored) {
                // One unavailable seed must not prevent bootstrap from the others.
            }
        }
        if (peers.isEmpty()) throw new IllegalStateException("No Yerbas DNS seed resolved");
        Collections.shuffle(peers);
        return List.copyOf(peers);
    }
}
