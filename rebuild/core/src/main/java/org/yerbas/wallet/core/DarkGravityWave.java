package org.yerbas.wallet.core;

import java.math.BigInteger;
import java.util.List;

/** Exact Java implementation of Yerbas Core DarkGravityWave v3. */
public final class DarkGravityWave {
    public static final int WINDOW = 60;
    public static final int TARGET_SPACING_SECONDS = 120;
    public static final long POW_LIMIT_COMPACT = 0x20001fffL;

    private DarkGravityWave() {}

    /** Headers must be ordered newest first and contain at least 60 entries. */
    public static long nextTarget(List<BlockHeader> headers) {
        if (headers == null || headers.size() < WINDOW) return POW_LIMIT_COMPACT;

        BigInteger average = BigInteger.ZERO;
        for (int count = 1; count <= WINDOW; count++) {
            BigInteger target = CompactTarget.decode(headers.get(count - 1).compactTarget());
            if (count == 1) {
                average = target;
            } else {
                // Matches Yerbas Core exactly, including its historical non-standard averaging formula.
                average = average.multiply(BigInteger.valueOf(count))
                        .add(target)
                        .divide(BigInteger.valueOf(count + 1L));
            }
        }

        long actual = headers.get(0).timestamp() - headers.get(WINDOW - 1).timestamp();
        long expected = (long) WINDOW * TARGET_SPACING_SECONDS;
        actual = Math.max(expected / 3, Math.min(expected * 3, actual));

        BigInteger result = average.multiply(BigInteger.valueOf(actual))
                .divide(BigInteger.valueOf(expected));
        BigInteger powLimit = CompactTarget.decode(POW_LIMIT_COMPACT);
        if (result.signum() <= 0 || result.compareTo(powLimit) > 0) result = powLimit;
        return CompactTarget.encode(result);
    }

    public static boolean verify(long candidate, List<BlockHeader> history) {
        return candidate == nextTarget(history);
    }
}
