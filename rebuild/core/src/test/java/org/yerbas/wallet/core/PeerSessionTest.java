package org.yerbas.wallet.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class PeerSessionTest {
    @Test void completesHandshake() {
        PeerSession peer = new PeerSession(NetworkParameters.mainnet());
        peer.markVersionSent();
        peer.receiveVersion(70223);
        peer.receiveVerack();
        assertEquals(PeerSession.State.READY, peer.state());
    }

    @Test void rejectsOldPeers() {
        PeerSession peer = new PeerSession(NetworkParameters.mainnet());
        assertThrows(IllegalArgumentException.class, () -> peer.receiveVersion(70222));
        assertEquals(PeerSession.State.CLOSED, peer.state());
    }
}
