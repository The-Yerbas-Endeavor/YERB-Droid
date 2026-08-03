package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BlockHeaderTest {
    @Test
    void roundTripsExactWireBytes() {
        byte[] wire = new byte[BlockHeader.SERIALIZED_SIZE];
        wire[0] = 4;
        wire[68] = 0x34;
        wire[69] = 0x12;
        wire[72] = (byte) 0xff;
        wire[73] = 0x1f;
        wire[79] = 1;

        BlockHeader header = BlockHeader.parse(wire);
        assertArrayEquals(wire, header.serialize());
        assertEquals(0x1234L, header.timestamp());
    }

    @Test
    void rejectsNonHeaderLengths() {
        assertThrows(IllegalArgumentException.class, () -> BlockHeader.parse(new byte[79]));
        assertThrows(IllegalArgumentException.class, () -> BlockHeader.parse(new byte[81]));
    }
}
