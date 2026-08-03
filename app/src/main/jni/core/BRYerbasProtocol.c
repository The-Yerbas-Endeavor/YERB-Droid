// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#include "BRYerbasProtocol.h"
#include "BRCrypto.h"
#include <string.h>

int BRYerbasBlockHeaderParse(BRYerbasBlockHeader *header, const uint8_t *bytes, size_t bytesLen)
{
    size_t off = 0;

    if (!header || !bytes || bytesLen != YERB_HEADER_SIZE) return 0;

    header->version = UInt32GetLE(&bytes[off]);
    off += sizeof(uint32_t);
    header->prevBlock = UInt256Get(&bytes[off]);
    off += sizeof(UInt256);
    header->merkleRoot = UInt256Get(&bytes[off]);
    off += sizeof(UInt256);
    header->timestamp = UInt32GetLE(&bytes[off]);
    off += sizeof(uint32_t);
    header->target = UInt32GetLE(&bytes[off]);
    off += sizeof(uint32_t);
    header->nonce = UInt32GetLE(&bytes[off]);
    off += sizeof(uint32_t);

    return off == YERB_HEADER_SIZE;
}

int BRYerbasBlockHeaderSerialize(const BRYerbasBlockHeader *header, uint8_t *bytes, size_t bytesLen)
{
    size_t off = 0;

    if (!header || !bytes || bytesLen < YERB_HEADER_SIZE) return 0;

    UInt32SetLE(&bytes[off], header->version);
    off += sizeof(uint32_t);
    UInt256Set(&bytes[off], header->prevBlock);
    off += sizeof(UInt256);
    UInt256Set(&bytes[off], header->merkleRoot);
    off += sizeof(UInt256);
    UInt32SetLE(&bytes[off], header->timestamp);
    off += sizeof(uint32_t);
    UInt32SetLE(&bytes[off], header->target);
    off += sizeof(uint32_t);
    UInt32SetLE(&bytes[off], header->nonce);
    off += sizeof(uint32_t);

    return off == YERB_HEADER_SIZE;
}

void BRYerbasBlockId(UInt256 *blockId, const uint8_t header[YERB_HEADER_SIZE])
{
    if (!blockId || !header) return;
    SHA256_2(blockId, header, YERB_HEADER_SIZE);
}

int BRYerbasCompactTarget(UInt256 *target, uint32_t compact)
{
    uint32_t size = compact >> 24;
    uint32_t word = compact & 0x007fffffu;
    int negative = (compact & 0x00800000u) != 0;
    uint8_t out[sizeof(UInt256)] = {0};

    if (!target || negative || word == 0) return 0;

    if (size <= 3) {
        word >>= 8 * (3 - size);
        out[0] = (uint8_t)(word & 0xffu);
        out[1] = (uint8_t)((word >> 8) & 0xffu);
        out[2] = (uint8_t)((word >> 16) & 0xffu);
    } else {
        size_t offset = size - 3;
        if (offset + 3 > sizeof(out)) return 0;
        out[offset] = (uint8_t)(word & 0xffu);
        out[offset + 1] = (uint8_t)((word >> 8) & 0xffu);
        out[offset + 2] = (uint8_t)((word >> 16) & 0xffu);
    }

    memcpy(target->u8, out, sizeof(out));
    return 1;
}

int BRYerbasHashMeetsTarget(UInt256 hash, UInt256 target)
{
    size_t i = sizeof(UInt256);

    while (i > 0) {
        --i;
        if (hash.u8[i] < target.u8[i]) return 1;
        if (hash.u8[i] > target.u8[i]) return 0;
    }

    return 1;
}
