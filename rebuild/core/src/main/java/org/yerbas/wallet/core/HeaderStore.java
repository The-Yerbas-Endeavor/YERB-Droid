package org.yerbas.wallet.core;

import java.util.Optional;

/** Persistence boundary for validated headers. Android will implement this with Room. */
public interface HeaderStore {
    record StoredHeader(BlockHeader header, int height, byte[] chainWork) {
        public StoredHeader {
            chainWork = chainWork.clone();
        }
        @Override public byte[] chainWork() { return chainWork.clone(); }
    }

    Optional<StoredHeader> find(byte[] blockId);
    Optional<StoredHeader> best();
    void put(byte[] blockId, StoredHeader header);
    void setBest(byte[] blockId);
}
