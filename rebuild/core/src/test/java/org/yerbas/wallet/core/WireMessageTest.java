package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class WireMessageTest {
    @Test
    void roundTripsYerbasMessage() {
        NetworkParameters network = NetworkParameters.mainnet();
        WireMessage original = new WireMessage("ping", new byte[] {1, 2, 3, 4});
        WireMessage parsed = WireMessage.parse(network, original.serialize(network));
        assertEquals("ping", parsed.command());
        assertArrayEquals(new byte[] {1, 2, 3, 4}, parsed.payload());
    }

    @Test
    void rejectsWrongMagicAndChecksum() {
        NetworkParameters network = NetworkParameters.mainnet();
        byte[] encoded = new WireMessage("verack", new byte[0]).serialize(network);
        encoded[0] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> WireMessage.parse(network, encoded));

        encoded = new WireMessage("ping", new byte[] {9}).serialize(network);
        encoded[20] ^= 1;
        byte[] corrupted = encoded;
        assertThrows(IllegalArgumentException.class, () -> WireMessage.parse(network, corrupted));
    }
}
