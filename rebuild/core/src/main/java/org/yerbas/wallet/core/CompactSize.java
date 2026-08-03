package org.yerbas.wallet.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Canonical Bitcoin-family CompactSize codec used by Yerbas wire messages. */
public final class CompactSize {
    private CompactSize() {}

    public record Decoded(long value, int encodedLength) {}

    public static Decoded read(byte[] bytes, int offset) {
        if (bytes == null || offset < 0 || offset >= bytes.length) {
            throw new IllegalArgumentException("CompactSize offset out of range");
        }
        int marker = Byte.toUnsignedInt(bytes[offset]);
        if (marker < 0xfd) return new Decoded(marker, 1);
        if (marker == 0xfd) {
            require(bytes, offset, 3);
            long value = Short.toUnsignedLong(ByteBuffer.wrap(bytes, offset + 1, 2)
                    .order(ByteOrder.LITTLE_ENDIAN).getShort());
            if (value < 0xfd) throw new IllegalArgumentException("Non-canonical CompactSize");
            return new Decoded(value, 3);
        }
        if (marker == 0xfe) {
            require(bytes, offset, 5);
            long value = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset + 1, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).getInt());
            if (value <= 0xffffL) throw new IllegalArgumentException("Non-canonical CompactSize");
            return new Decoded(value, 5);
        }
        require(bytes, offset, 9);
        long value = ByteBuffer.wrap(bytes, offset + 1, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        if (value < 0 || value <= 0xffff_ffffL) {
            throw new IllegalArgumentException("Unsupported or non-canonical CompactSize");
        }
        return new Decoded(value, 9);
    }

    public static byte[] write(long value) {
        if (value < 0) throw new IllegalArgumentException("CompactSize cannot be negative");
        if (value < 0xfd) return new byte[] {(byte) value};
        if (value <= 0xffffL) {
            return ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) 0xfd).putShort((short) value).array();
        }
        if (value <= 0xffff_ffffL) {
            return ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
                    .put((byte) 0xfe).putInt((int) value).array();
        }
        return ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN)
                .put((byte) 0xff).putLong(value).array();
    }

    private static void require(byte[] bytes, int offset, int length) {
        if (offset + length > bytes.length) throw new IllegalArgumentException("Truncated CompactSize");
    }
}
