package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class VersionPayloadTest {
    @Test
    void roundTripsVersionPayload() {
        VersionPayload.NetworkAddress receiver = VersionPayload.unspecified(15420);
        VersionPayload.NetworkAddress sender = new VersionPayload.NetworkAddress(1, new byte[16], 15420);
        VersionPayload original = new VersionPayload(
                70223, 1, 1_700_000_000L, receiver, sender,
                42L, "/YERB-Droid:0.1.0/", 1_060_820, true);

        VersionPayload decoded = VersionPayload.decode(original.encode());
        assertEquals(original.protocolVersion(), decoded.protocolVersion());
        assertEquals(original.userAgent(), decoded.userAgent());
        assertEquals(original.startHeight(), decoded.startHeight());
        assertEquals(original.relay(), decoded.relay());
        assertArrayEquals(original.receiver().ipv6(), decoded.receiver().ipv6());
        assertEquals(15420, decoded.receiver().port());
    }
}
