package org.yerbas.wallet.core;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

/** Tracks one outstanding ping and enforces peer liveness deadlines. */
public final class PeerLiveness {
    private final Duration pingInterval;
    private final Duration timeout;
    private Instant lastActivity;
    private Instant pingSentAt;
    private long outstandingNonce;
    private boolean awaitingPong;

    public PeerLiveness(Instant now, Duration pingInterval, Duration timeout) {
        if (pingInterval.isNegative() || pingInterval.isZero()) throw new IllegalArgumentException("pingInterval must be positive");
        if (timeout.isNegative() || timeout.isZero()) throw new IllegalArgumentException("timeout must be positive");
        this.pingInterval = pingInterval;
        this.timeout = timeout;
        this.lastActivity = now;
    }

    public void recordActivity(Instant now) {
        if (now.isBefore(lastActivity)) throw new IllegalArgumentException("time moved backwards");
        lastActivity = now;
    }

    public OptionalLong maybeCreatePing(Instant now) {
        if (awaitingPong || now.isBefore(lastActivity.plus(pingInterval))) return OptionalLong.empty();
        outstandingNonce = ThreadLocalRandom.current().nextLong();
        pingSentAt = now;
        awaitingPong = true;
        return OptionalLong.of(outstandingNonce);
    }

    public boolean acceptPong(long nonce, Instant now) {
        if (!awaitingPong || nonce != outstandingNonce) return false;
        awaitingPong = false;
        pingSentAt = null;
        lastActivity = now;
        return true;
    }

    public boolean timedOut(Instant now) {
        return awaitingPong && !now.isBefore(pingSentAt.plus(timeout));
    }

    public boolean awaitingPong() { return awaitingPong; }
}
