# Yerbas Android wallet rebuild status

The repository now contains a clean implementation under `rebuild/`. The inherited Breadwallet/Ravencoin application remains reference-only and is not a production foundation.

## Confirmed Yerbas mainnet values

- P2P port: `15420`
- Message-start bytes: `79 65 72 62` (`yerb`)
- Protocol version: `70223`
- Genesis hash: `eff0bbe5c1bbe1ef8da54822a18f528d6dc58232990bdb86e0a77ab2814ed12c`
- Genesis time: `1652138420`
- Genesis compact target: `0x20001fff`
- Pubkey prefix: `140`
- Script prefix: `19`
- Private-key prefix: `128`
- BIP44 coin type: `200`
- Target spacing: `120` seconds
- DGW history: `60` blocks

## Implemented clean-core components

- Strict Yerbas wire-message framing and checksums
- Incremental fragmented-TCP frame decoder
- Socket peer transport with timeouts and serialized writes
- DNS seed resolution and peer deduplication
- Version/verack handshake controller with protocol enforcement
- Ping/pong handling
- Version payload codec
- Getheaders and headers codecs
- Header parsing and block-ID calculation
- Checkpoint validation
- Header persistence boundary and in-memory implementation
- Chain-work tracking and best-tip selection
- Block locator generation
- Java and native DarkGravityWave implementations
- Special-transaction envelope parsing
- Reorganization event contracts
- Native and Java regression-test scaffolding

## Mandatory release-candidate gates

The project must remain a draft until every gate below passes on an actual build runner and Android device:

1. `./gradlew :core:test` passes from a clean checkout.
2. Native C/C++ sources compile for all supported Android ABIs.
3. DNS bootstrap resolves at least two independent Yerbas peers.
4. Version/verack handshakes succeed with protocol `70223` peers.
5. Header synchronization reaches the current network tip.
6. Checkpoints and exact DGW targets validate across known historical ranges.
7. GhostRider proof-of-work is verified using known Core block vectors.
8. Header synchronization resumes correctly after process restart.
9. BIP39/BIP32/BIP44 vectors match Yerbas Core-derived addresses.
10. Seed material is encrypted using Android Keystore and never logged.
11. Standard YERB transactions can be constructed, signed, broadcast and confirmed.
12. Special transactions and asset payloads round-trip without data loss.
13. Reorganizations correctly roll wallet and UTXO state backward and forward.
14. Release APK/AAB builds with reproducible versioning and no debug keys.
15. Restore, receive, send, offline restart and upgrade tests pass on physical devices.

## Current validation boundary

The connected GitHub environment can safely create and review source files, but it cannot run the Android SDK/NDK build, connect to Yerbas peers, or install an APK on a physical device. Therefore the source is progressing toward a release candidate, but it is not honestly production-ready until the mandatory gates above are executed and their failures are corrected.
