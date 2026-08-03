package org.yerbas.wallet.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Test and desktop implementation of the header store. */
public final class InMemoryHeaderStore implements HeaderStore {
    private final Map<String, StoredHeader> headers = new HashMap<>();
    private String best;

    @Override public Optional<StoredHeader> find(byte[] blockId) {
        return Optional.ofNullable(headers.get(hex(blockId)));
    }

    @Override public Optional<StoredHeader> best() {
        return best == null ? Optional.empty() : Optional.ofNullable(headers.get(best));
    }

    @Override public void put(byte[] blockId, StoredHeader header) {
        headers.put(hex(blockId), header);
    }

    @Override public void setBest(byte[] blockId) {
        String key = hex(blockId);
        if (!headers.containsKey(key)) throw new IllegalArgumentException("unknown best header");
        best = key;
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format("%02x", b));
        return out.toString();
    }
}
