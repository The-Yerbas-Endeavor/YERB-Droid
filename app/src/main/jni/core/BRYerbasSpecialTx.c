// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#include "BRYerbasSpecialTx.h"

#include <string.h>

static uint16_t read16le(const uint8_t *p)
{
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static uint32_t read32le(const uint8_t *p)
{
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

static uint64_t read64le(const uint8_t *p)
{
    uint64_t v = 0;
    for (unsigned i = 0; i < 8; ++i) v |= (uint64_t)p[i] << (8 * i);
    return v;
}

static int read_compact_size(const uint8_t *buf, size_t len,
                             uint64_t *value, size_t *used)
{
    if (!buf || len == 0 || !value || !used) return 0;

    uint8_t first = buf[0];
    if (first < 253) {
        *value = first;
        *used = 1;
        return 1;
    }
    if (first == 253) {
        if (len < 3) return 0;
        uint64_t v = read16le(buf + 1);
        if (v < 253) return 0;
        *value = v;
        *used = 3;
        return 1;
    }
    if (first == 254) {
        if (len < 5) return 0;
        uint64_t v = read32le(buf + 1);
        if (v <= 0xffffu) return 0;
        *value = v;
        *used = 5;
        return 1;
    }

    if (len < 9) return 0;
    uint64_t v = read64le(buf + 1);
    if (v <= 0xffffffffu) return 0;
    *value = v;
    *used = 9;
    return 1;
}

static int advance(size_t *off, size_t amount, size_t total)
{
    if (!off || amount > total - *off) return 0;
    *off += amount;
    return 1;
}

int BRYerbasParseSpecialTxEnvelope(const uint8_t *tx,
                                   size_t txLen,
                                   BRYerbasSpecialTxInfo *info)
{
    if (!tx || !info || txLen < 10) return 0;
    memset(info, 0, sizeof(*info));

    size_t off = 0;
    uint32_t rawVersion = read32le(tx);
    info->version = (uint16_t)(rawVersion & 0xffffu);
    info->type = (uint16_t)(rawVersion >> 16);
    off += 4;

    uint64_t inputCount = 0;
    size_t used = 0;
    if (!read_compact_size(tx + off, txLen - off, &inputCount, &used) ||
        !advance(&off, used, txLen)) return 0;

    if (inputCount > (txLen / 41u)) return 0;
    for (uint64_t i = 0; i < inputCount; ++i) {
        if (!advance(&off, 36, txLen)) return 0;
        uint64_t scriptLen = 0;
        if (!read_compact_size(tx + off, txLen - off, &scriptLen, &used) ||
            !advance(&off, used, txLen) || scriptLen > SIZE_MAX ||
            !advance(&off, (size_t)scriptLen, txLen) ||
            !advance(&off, 4, txLen)) return 0;
    }

    uint64_t outputCount = 0;
    if (!read_compact_size(tx + off, txLen - off, &outputCount, &used) ||
        !advance(&off, used, txLen)) return 0;

    if (outputCount > (txLen / 9u)) return 0;
    for (uint64_t i = 0; i < outputCount; ++i) {
        if (!advance(&off, 8, txLen)) return 0;
        uint64_t scriptLen = 0;
        if (!read_compact_size(tx + off, txLen - off, &scriptLen, &used) ||
            !advance(&off, used, txLen) || scriptLen > SIZE_MAX ||
            !advance(&off, (size_t)scriptLen, txLen)) return 0;
    }

    if (!advance(&off, 4, txLen)) return 0;
    info->lockTime = read32le(tx + off - 4);

    if (info->type != 0) {
        uint64_t payloadLen = 0;
        if (!read_compact_size(tx + off, txLen - off, &payloadLen, &used) ||
            !advance(&off, used, txLen) || payloadLen > SIZE_MAX ||
            payloadLen > txLen - off) return 0;
        info->extraPayload = tx + off;
        info->extraPayloadLen = (size_t)payloadLen;
        off += (size_t)payloadLen;
    }

    if (off != txLen) return 0;
    info->serializedLen = off;
    return 1;
}
