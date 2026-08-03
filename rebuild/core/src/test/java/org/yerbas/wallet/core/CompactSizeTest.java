package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class CompactSizeTest {
    @Test
    void roundTripsCanonicalValues() {
        long[] values = {0, 252, 253, 65535, 65536, 4_294_967_295L, 4_294_967_296L};
        for (long value : values) {
            byte[] encoded = CompactSize.write(value);
            CompactSize.Decoded decoded = CompactSize.read(encoded, 0);
            assertEquals(value, decoded.value());
            assertEquals(encoded.length, decoded.encodedLength());
        }
    }

    @Test
    void rejectsNonCanonicalEncoding() {
        assertThrows(IllegalArgumentException.class,
                () -> CompactSize.read(new byte[] {(byte) 0xfd, 1, 0}, 0));
    }

    @Test
    void writesExpectedBoundaries() {
        assertArrayEquals(new byte[] {(byte) 0xfc}, CompactSize.write(252));
        assertArrayEquals(new byte[] {(byte) 0xfd, (byte) 0xfd, 0}, CompactSize.write(253));
    }
}
