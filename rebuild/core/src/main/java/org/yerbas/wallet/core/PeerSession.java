package org.yerbas.wallet.core;

/** Deterministic state machine for a single Yerbas peer handshake. */
public final class PeerSession {
    public enum State { NEW, VERSION_SENT, VERSION_RECEIVED, READY, CLOSED }

    private final NetworkParameters network;
    private State state = State.NEW;
    private int remoteProtocol;

    public PeerSession(NetworkParameters network) { this.network = network; }
    public State state() { return state; }
    public int remoteProtocol() { return remoteProtocol; }

    public void markVersionSent() {
        require(State.NEW);
        state = State.VERSION_SENT;
    }

    public void receiveVersion(int protocolVersion) {
        if (state != State.NEW && state != State.VERSION_SENT) throw new IllegalStateException("unexpected version");
        if (protocolVersion < network.minimumPeerProtocol()) {
            state = State.CLOSED;
            throw new IllegalArgumentException("peer protocol too old: " + protocolVersion);
        }
        remoteProtocol = protocolVersion;
        state = State.VERSION_RECEIVED;
    }

    public void receiveVerack() {
        if (state != State.VERSION_RECEIVED) throw new IllegalStateException("verack before version");
        state = State.READY;
    }

    public void close() { state = State.CLOSED; }

    private void require(State expected) {
        if (state != expected) throw new IllegalStateException("expected " + expected + " but was " + state);
    }
}
