package org.yerbas.wallet.core;

/** Boundary for Yerbas GhostRider proof-of-work verification. */
@FunctionalInterface
public interface ProofOfWorkVerifier {
    /** Returns true only when the header's GhostRider hash satisfies its compact target. */
    boolean verify(BlockHeader header);

    /** Production-safe default: reject headers until the native GhostRider verifier is loaded. */
    static ProofOfWorkVerifier unavailable() {
        return header -> false;
    }
}
