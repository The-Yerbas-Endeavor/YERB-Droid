# Yerbas Core compatibility

This branch migrates YERB-Droid from inherited Bitcoin/Ravencoin assumptions toward the current Yerbas Core network.

## Confirmed current mainnet values

- P2P port: `15420`
- Message-start bytes: `79 65 72 62` (`yerb`)
- Genesis hash: `eff0bbe5c1bbe1ef8da54822a18f528d6dc58232990bdb86e0a77ab2814ed12c`
- Genesis time: `1652138420`
- Genesis compact target: `0x20001fff`
- Pubkey address prefix: `140`
- Script address prefix: `19`
- Private-key prefix: `128`
- BIP44 coin type: `200`
- Current Core protocol version: `70223`

## Implemented native compatibility modules

- `BRYerbasProtocol.[ch]`
  - current protocol, port and message magic constants
  - fixed 80-byte block-header parsing and serialization
  - double-SHA256 block IDs/locators
  - compact target expansion and PoW target comparison
  - special-transaction version/type helpers
- `BRYerbasDGW.[ch]`
  - exact 60-block DarkGravityWave arithmetic
  - 120-second target spacing
  - 512-bit averaging and timespan retarget math matching `yerbas/src/pow.cpp`
- `BRYerbasSpecialTx.[ch]`
  - bounds-checked transaction envelope parsing
  - extraction and preservation of CompactSize-prefixed special-transaction payloads
- `.github/workflows/native-protocol-checks.yml`
  - Clang syntax checks
  - current-network constant checks
  - rejection of inherited Ravencoin protocol constants

## Confirmed header and proof-of-work architecture

Yerbas serializes a standard 80-byte block header. The block ID and block locator hash are double-SHA256 of that serialized header. Proof of work is a separate GhostRider hash over the header, with algorithm selection derived from the previous block hash.

The inherited 120-byte KAWPOW header parser is therefore invalid for Yerbas and must be removed from `BRMerkleBlock.c` and `BRPeer.c`.

## Six-workstream status

1. **Peer protocol:** constants and reusable framing/header primitives are implemented; direct `BRPeer.c` integration remains.
2. **Merkle/header validation:** fixed 80-byte parsing and target checks are implemented; direct `BRMerkleBlock.c` replacement remains.
3. **GhostRider and DGW:** exact DGW is implemented; GhostRider dependency port remains.
4. **Transactions:** safe special-transaction envelope/payload parsing is implemented; ownership and round-trip serialization in `BRTransaction` remain.
5. **Android modernization:** repository cleanup and native CI checks are implemented; Gradle/AGP/SDK/NDK upgrades remain after native integration.
6. **Testing:** protocol syntax/constant CI and native test scaffolding are implemented; known mainnet block, PoW, DGW, transaction and live-peer vectors remain.

## Work still requiring integration

1. Replace `BRPeer.c` protocol constants and Ravencoin header locator code with `BRYerbasProtocol` calls.
2. Replace `BRMerkleBlock.c` X16R/X16Rv2/KAWPOW parsing with fixed 80-byte Yerbas parsing.
3. Port GhostRider's `HashSelection`, chained core hashes and CryptoNight variants into the Android native build.
4. Connect `BRYerbasDGWNextTarget()` to the SPV header-chain verifier.
5. Extend the main `BRTransaction` object to own and reserialize special-transaction extra payload bytes.
6. Modernize Gradle, AGP, SDK, NDK and dependencies after the native protocol migration compiles.
7. Add known-block, known-PoW, DGW, special-transaction and live-peer test vectors.

## Release gate

Do not publish or merge a production APK until all of the following pass:

1. Native and Java builds on a clean runner.
2. Handshake with multiple Yerbas Core `70223` peers.
3. Header synchronization from genesis and from the newest checkpoint.
4. GhostRider verification against known mainnet block vectors.
5. Exact DGW target verification across real mainnet history.
6. Wallet restore and deterministic address-vector tests.
7. Incoming and outgoing standard YERB transaction tests.
8. Special transaction and asset payload round-trip tests.
9. Reorg and invalid-header rejection tests.
10. Signed release build tested on currently supported Android versions.

## Security note

The existing production peer and merkle-block files still contain inherited Ravencoin logic. The new modules are isolated and reviewable, but the wallet remains non-release-ready until those modules are integrated, GhostRider is ported, and clean Android builds plus live-network tests pass.
