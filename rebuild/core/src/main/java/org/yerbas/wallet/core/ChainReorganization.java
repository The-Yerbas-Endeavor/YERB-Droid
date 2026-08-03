package org.yerbas.wallet.core;

import java.util.List;

/** Immutable description of a best-chain change for wallet and UTXO consumers. */
public record ChainReorganization(
        HeaderStore.StoredHeader commonAncestor,
        List<HeaderStore.StoredHeader> disconnected,
        List<HeaderStore.StoredHeader> connected) {

    public ChainReorganization {
        if (commonAncestor == null) throw new IllegalArgumentException("commonAncestor is required");
        disconnected = List.copyOf(disconnected);
        connected = List.copyOf(connected);
    }

    public interface Listener {
        void onReorganization(ChainReorganization event);
    }
}
