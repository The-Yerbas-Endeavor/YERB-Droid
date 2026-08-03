# Yerbas Core compatibility repair

This branch is the staging area for restoring YERB-Droid compatibility with current Yerbas Core.

## Implemented

- Current Yerbas mainnet seeds, port, magic, genesis data, and checkpoints.
- Yerbas protocol constants for protocol `70223`, mainnet magic `yerb`, port `15420`, 80-byte headers, DGW window 60, and 120-second spacing.
- Exact 80-byte header parsing and serialization.
- Double-SHA256 block identifier/locator hashing.
- Compact target expansion and hash-to-target comparison helpers.
- Dash/Yerbas special-transaction base-version and type extraction from the packed 32-bit version.
- Native regression-test source for these primitives.
- Repository ignore rules for Android, Gradle, CMake, NDK, Ninja, object, APK, and AAB output.

## Confirmed current Yerbas behavior

Yerbas uses two distinct header hashes:

1. `GetHash()` is the double-SHA256 hash of the serialized 80-byte block header. This is the block ID used by locators, inventory, previous-block references, checkpoints, and explorer/RPC identifiers.
2. `GetPOWHash()` is GhostRider over the same serialized header, with algorithm selection derived from the previous block hash. This is compared with the target encoded by `nBits`.

Yerbas does not use Ravencoin's 120-byte KAWPOW header format. The inherited X16R, X16Rv2, Ethash, ProgPoW, and KAWPOW paths in YERB-Droid must therefore be removed.

## Remaining implementation work

### Peer and header path

- Replace `BRPeer.c` constants with the shared Yerbas protocol constants.
- Replace variable 80/120-byte Ravencoin header handling with fixed 80-byte Yerbas headers.
- Replace X16R/X16Rv2/KAWPOW locator generation with double-SHA256 block IDs.
- Remove KAWPOW activation-time assumptions and 120-byte offsets.

### GhostRider proof of work

- Port `HashSelection`, `coreHash`, `cnHash`, and the required CryptoNight variants from Yerbas Core.
- Add `BRYerbasGhostRiderHash(header80, prevHash)`.
- Validate the GhostRider result against the expanded compact target.
- Add known-block GhostRider vectors from Yerbas Core.

### Dark Gravity Wave

- Port the exact 60-block DGW arithmetic from `src/pow.cpp`.
- Use 120-second target spacing and mainnet `powLimit`.
- Verify expected `nBits` for known consecutive mainnet headers.

### Special transactions

- Parse the packed transaction version into base version and type.
- For non-zero transaction type, parse and preserve the CompactSize-prefixed extra payload after `nLockTime`.
- Include extra payload bytes in transaction serialization and txid calculation.
- Treat unsupported special transaction types as opaque payloads rather than rejecting or truncating them.

### Build and release gates

- Add the new protocol source to CMake.
- Remove obsolete X16R/KAWPOW source entries after GhostRider is integrated.
- Modernize Gradle/SDK/NDK dependencies.
- Build all supported Android ABIs in CI.
- Test clean sync, restore, receive, send, fee handling, assets, and special transactions against live Yerbas peers.

This branch remains a draft until the native GhostRider/DGW implementation compiles and known-chain vectors pass.
