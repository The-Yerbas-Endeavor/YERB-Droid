package org.yerbas.wallet.core;

import java.math.BigInteger;

/** Bitcoin-family compact difficulty target codec. */
public final class CompactTarget {
    private CompactTarget() {}

    public static BigInteger decode(long compactValue) {
        int compact = (int) compactValue;
        int size = compact >>> 24;
        int word = compact & 0x007fffff;
        if ((compact & 0x00800000) != 0) throw new IllegalArgumentException("negative compact target");
        BigInteger value = BigInteger.valueOf(word);
        return size <= 3 ? value.shiftRight(8 * (3 - size)) : value.shiftLeft(8 * (size - 3));
    }

    public static long encode(BigInteger value) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException("target must be non-negative");
        if (value.signum() == 0) return 0;

        int size = (value.bitLength() + 7) / 8;
        long compact;
        if (size <= 3) {
            compact = value.longValue() << (8 * (3 - size));
        } else {
            compact = value.shiftRight(8 * (size - 3)).longValue();
        }
        compact &= 0x00ff_ffffL;
        if ((compact & 0x0080_0000L) != 0) {
            compact >>= 8;
            size++;
        }
        return compact | ((long) size << 24);
    }
}
