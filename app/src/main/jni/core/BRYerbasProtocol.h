// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#ifndef BRYerbasProtocol_h
#define BRYerbasProtocol_h

#include <stddef.h>
#include <stdint.h>
#include "BRInt.h"

#ifdef __cplusplus
extern "C" {
#endif

#define YERB_PROTOCOL_VERSION       70223u
#define YERB_MIN_PROTOCOL_VERSION   70223u
#define YERB_MAINNET_MAGIC          0x62726579u /* bytes: 79 65 72 62 = "yerb" */
#define YERB_MAINNET_PORT           15420u
#define YERB_HEADER_SIZE            80u
#define YERB_DGW_PAST_BLOCKS        60u
#define YERB_TARGET_SPACING         120u
#define YERB_DGW_START_HEIGHT       60u
#define YERB_ASSETS_FORK_HEIGHT     229420u

typedef struct {
    uint32_t version;
    UInt256 prevBlock;
    UInt256 merkleRoot;
    uint32_t timestamp;
    uint32_t target;
    uint32_t nonce;
} BRYerbasBlockHeader;

// Yerbas/Dash special transaction type is stored in the upper 16 bits.
static inline uint16_t BRYerbasTxBaseVersion(uint32_t rawVersion)
{
    return (uint16_t)(rawVersion & 0xffffu);
}

static inline uint16_t BRYerbasTxType(uint32_t rawVersion)
{
    return (uint16_t)(rawVersion >> 16);
}

static inline int BRYerbasTxHasExtraPayload(uint32_t rawVersion)
{
    return BRYerbasTxType(rawVersion) != 0;
}

// Parses exactly the consensus 80-byte Yerbas block header.
int BRYerbasBlockHeaderParse(BRYerbasBlockHeader *header, const uint8_t *bytes, size_t bytesLen);

// Serializes exactly the consensus 80-byte Yerbas block header.
int BRYerbasBlockHeaderSerialize(const BRYerbasBlockHeader *header, uint8_t *bytes, size_t bytesLen);

// Computes the block identifier/locator hash: double SHA-256 over the 80-byte header.
void BRYerbasBlockId(UInt256 *blockId, const uint8_t header[YERB_HEADER_SIZE]);

// Expands Bitcoin compact target encoding into a 256-bit little-endian value.
// Returns 1 for a valid non-negative target, otherwise 0.
int BRYerbasCompactTarget(UInt256 *target, uint32_t compact);

// Compares a little-endian 256-bit hash against a little-endian target.
int BRYerbasHashMeetsTarget(UInt256 hash, UInt256 target);

#ifdef __cplusplus
}
#endif

#endif // BRYerbasProtocol_h
