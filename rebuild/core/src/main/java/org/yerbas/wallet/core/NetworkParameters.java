package org.yerbas.wallet.core;

import java.util.List;

/** Immutable Yerbas mainnet wire and consensus constants. */
public record NetworkParameters(
        int protocolVersion,
        int minimumPeerProtocol,
        int port,
        byte[] messageStart,
        int targetSpacingSeconds,
        int dgwWindow,
        String genesisHash,
        List<String> dnsSeeds) {

    public NetworkParameters {
        messageStart = messageStart.clone();
        dnsSeeds = List.copyOf(dnsSeeds);
    }

    @Override
    public byte[] messageStart() {
        return messageStart.clone();
    }

    public static NetworkParameters mainnet() {
        return new NetworkParameters(
                70223,
                70223,
                15420,
                new byte[] {0x79, 0x65, 0x72, 0x62},
                120,
                60,
                "eff0bbe5c1bbe1ef8da54822a18f528d6dc58232990bdb86e0a77ab2814ed12c",
                List.of(
                        "weednode00.yerbas.org",
                        "weednode01.yerbas.org",
                        "weednode02.yerbas.org",
                        "weednode03.yerbas.org",
                        "weednode420.yerbas.org",
                        "weednode05.yerbas.org"));
    }
}
