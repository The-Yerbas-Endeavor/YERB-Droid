# Yerbas Core compatibility

This branch begins the migration of YERB-Droid from its inherited Bitcoin/Ravencoin network assumptions to the current Yerbas Core network.

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

## Critical inherited code still requiring replacement

`BRPeer.c` contains Ravencoin message magic, protocol versions, X16R/X16Rv2/KAWPOW header parsing, and Ravencoin-specific assumptions. These must be replaced with Yerbas Core-compatible message framing and Yerbas proof-of-work/header parsing before the wallet is safe to release.

The prior `BRChainParams.h` contained Bitcoin DNS seeds, ports, message magic and checkpoints. This branch replaces those values with Yerbas data and removes the Bitcoin 2016-block retarget verifier, which is incompatible with Yerbas Dark Gravity Wave.

## Release gate

Do not publish or merge a production APK until all of the following pass:

1. Native and Java builds on a clean runner.
2. Handshake with multiple Yerbas Core `70223` peers.
3. Header synchronization from genesis and from the newest checkpoint.
4. Wallet restore and deterministic address-vector tests.
5. Incoming and outgoing standard YERB transaction tests.
6. Fee calculation and rejected-transaction tests.
7. Asset issue, transfer, reissue and metadata parsing tests.
8. Reorg and invalid-header rejection tests.
9. Removal of committed `.cxx`, object files and generated build output.
10. Signed release build tested on currently supported Android versions.

## Security note

The temporary mainnet difficulty callback accepts headers after normal header/PoW validation and checkpoint enforcement because the inherited callback only understands Bitcoin's 2016-block retarget. A native Yerbas DGW verifier remains a mandatory production release gate.
