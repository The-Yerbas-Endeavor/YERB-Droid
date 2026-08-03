package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class NetworkParametersTest {
    @Test
    void mainnetMatchesYerbasCore() {
        NetworkParameters params = NetworkParameters.mainnet();
        assertEquals(70223, params.protocolVersion());
        assertEquals(15420, params.port());
        assertArrayEquals(new byte[] {0x79, 0x65, 0x72, 0x62}, params.messageStart());
        assertEquals(120, params.targetSpacingSeconds());
        assertEquals(60, params.dgwWindow());
        assertEquals(6, params.dnsSeeds().size());
    }
}
