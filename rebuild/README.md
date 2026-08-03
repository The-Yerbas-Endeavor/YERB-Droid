# YERB-Droid clean rebuild

This directory is a new wallet implementation. It does not depend on the inherited Breadwallet/Ravencoin peer, header, proof-of-work, transaction, or Android application code.

## Architecture

- `core`: platform-independent Java 17 models, serialization, validation, wallet state, transaction building and deterministic tests.
- `native`: planned C/C++ GhostRider proof-of-work and exact consensus helpers exposed through a narrow JNI boundary.
- `app`: planned modern Android UI and secure-storage integration.

## Rules

1. Legacy code may be consulted for branding and user-flow ideas, but is not copied into the new consensus or networking implementation without tests.
2. Every network constant must be sourced from current Yerbas Core.
3. Every consensus rule needs a known Core-generated test vector.
4. Private keys and seeds must never leave the device.
5. Release builds remain disabled until peer sync, restore, send, receive, reorg and asset tests pass.

## First milestones

1. Typed mainnet/testnet parameters.
2. 80-byte header parsing and block identifiers.
3. Peer message framing and handshake for protocol 70223.
4. Header chain storage and checkpoint verification.
5. Native GhostRider and DGW verification.
6. Standard and special transaction parsing.
7. HD wallet, signing and transaction construction.
8. Android application shell, encrypted seed storage and background sync.
