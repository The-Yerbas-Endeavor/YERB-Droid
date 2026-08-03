package org.yerbas.wallet.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/** Validates and stores contiguous Yerbas header batches. */
public final class HeaderChain {
    private final HeaderStore store;

    public HeaderChain(HeaderStore store) { this.store = store; }

    public HeaderStore.StoredHeader connect(BlockHeader header) {
        byte[] id = header.blockId();
        HeaderStore.StoredHeader parent = store.find(header.previousBlock())
                .orElseThrow(() -> new IllegalArgumentException("missing parent header"));
        int height = Math.addExact(parent.height(), 1);
        YerbasCheckpoints.requireMatch(height, id);
        byte[] work = addWork(parent.chainWork(), blockWork(header.compactTarget()));
        HeaderStore.StoredHeader stored = new HeaderStore.StoredHeader(header, height, work);
        store.put(id, stored);
        if (store.best().isEmpty() || compareUnsigned(work, store.best().orElseThrow().chainWork()) > 0) {
            store.setBest(id);
        }
        return stored;
    }

    public void bootstrap(BlockHeader genesis) {
        byte[] id = genesis.blockId();
        YerbasCheckpoints.requireMatch(0, id);
        HeaderStore.StoredHeader stored = new HeaderStore.StoredHeader(genesis, 0, blockWork(genesis.compactTarget()));
        store.put(id, stored);
        store.setBest(id);
    }

    public void connectAll(List<BlockHeader> headers) {
        for (BlockHeader header : headers) connect(header);
    }

    private static byte[] blockWork(long compact) {
        BigInteger target = CompactTarget.decode(compact);
        if (target.signum() <= 0) throw new IllegalArgumentException("invalid target");
        return toUnsigned256(BigInteger.ONE.shiftLeft(256).divide(target.add(BigInteger.ONE)));
    }

    private static byte[] addWork(byte[] left, byte[] right) {
        return toUnsigned256(new BigInteger(1, left).add(new BigInteger(1, right)));
    }

    private static byte[] toUnsigned256(BigInteger value) {
        byte[] raw = value.toByteArray();
        if (raw.length > 32) raw = Arrays.copyOfRange(raw, raw.length - 32, raw.length);
        byte[] out = new byte[32];
        System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
    }

    private static int compareUnsigned(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            int cmp = Integer.compare(Byte.toUnsignedInt(a[i]), Byte.toUnsignedInt(b[i]));
            if (cmp != 0) return cmp;
        }
        return 0;
    }
}
