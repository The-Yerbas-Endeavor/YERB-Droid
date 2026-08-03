// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#include "BRYerbasDGW.h"

#include <string.h>

typedef struct { uint64_t limb[8]; } YerbUInt512;
typedef struct { uint64_t limb[4]; } YerbUInt256;

static void y512_zero(YerbUInt512 *v) { memset(v, 0, sizeof(*v)); }

static YerbUInt256 y256_from_compact(uint32_t compact)
{
    YerbUInt256 out = {{0, 0, 0, 0}};
    uint32_t size = compact >> 24;
    uint32_t word = compact & 0x007fffffu;
    if (compact & 0x00800000u) return out;

    if (size <= 3) {
        word >>= 8 * (3 - size);
        out.limb[0] = word;
    } else {
        uint32_t shift = 8 * (size - 3);
        uint32_t limb = shift / 64;
        uint32_t bits = shift % 64;
        if (limb < 4) out.limb[limb] |= (uint64_t)word << bits;
        if (bits && limb + 1 < 4) out.limb[limb + 1] |= (uint64_t)word >> (64 - bits);
    }
    return out;
}

static int y256_is_zero(const YerbUInt256 *v)
{
    return (v->limb[0] | v->limb[1] | v->limb[2] | v->limb[3]) == 0;
}

static int y256_cmp(const YerbUInt256 *a, const YerbUInt256 *b)
{
    for (int i = 3; i >= 0; --i) {
        if (a->limb[i] < b->limb[i]) return -1;
        if (a->limb[i] > b->limb[i]) return 1;
    }
    return 0;
}

static uint32_t y256_compact(const YerbUInt256 *v)
{
    if (y256_is_zero(v)) return 0;

    int top = 31;
    while (top > 0) {
        uint8_t byte = (uint8_t)(v->limb[top / 8] >> ((top % 8) * 8));
        if (byte != 0) break;
        --top;
    }

    uint32_t size = (uint32_t)top + 1;
    uint32_t compact = 0;
    if (size <= 3) {
        compact = (uint32_t)v->limb[0] << (8 * (3 - size));
    } else {
        uint32_t shift = 8 * (size - 3);
        uint32_t limb = shift / 64;
        uint32_t bits = shift % 64;
        uint64_t value = (limb < 4) ? (v->limb[limb] >> bits) : 0;
        if (bits && limb + 1 < 4) value |= v->limb[limb + 1] << (64 - bits);
        compact = (uint32_t)value;
    }

    if (compact & 0x00800000u) {
        compact >>= 8;
        ++size;
    }
    return compact | (size << 24);
}

static YerbUInt512 y512_from_256(YerbUInt256 v)
{
    YerbUInt512 out;
    y512_zero(&out);
    for (int i = 0; i < 4; ++i) out.limb[i] = v.limb[i];
    return out;
}

static void y512_add_256(YerbUInt512 *a, YerbUInt256 b)
{
    __uint128_t carry = 0;
    for (int i = 0; i < 8; ++i) {
        __uint128_t sum = (__uint128_t)a->limb[i] + (i < 4 ? b.limb[i] : 0) + carry;
        a->limb[i] = (uint64_t)sum;
        carry = sum >> 64;
    }
}

static void y512_mul_small(YerbUInt512 *a, uint64_t m)
{
    __uint128_t carry = 0;
    for (int i = 0; i < 8; ++i) {
        __uint128_t p = (__uint128_t)a->limb[i] * m + carry;
        a->limb[i] = (uint64_t)p;
        carry = p >> 64;
    }
}

static void y512_div_small(YerbUInt512 *a, uint64_t d)
{
    __uint128_t rem = 0;
    for (int i = 7; i >= 0; --i) {
        __uint128_t cur = (rem << 64) | a->limb[i];
        a->limb[i] = (uint64_t)(cur / d);
        rem = cur % d;
    }
}

static YerbUInt256 y512_trim256(const YerbUInt512 *a)
{
    YerbUInt256 out = {{a->limb[0], a->limb[1], a->limb[2], a->limb[3]}};
    return out;
}

uint32_t BRYerbasDGWNextTarget(const BRYerbasDGWBlock *blocks, size_t count)
{
    const YerbUInt256 powLimit = y256_from_compact(YERBAS_POW_LIMIT_COMPACT);
    if (!blocks || count < YERBAS_DGW_BLOCKS) return YERBAS_POW_LIMIT_COMPACT;

    YerbUInt512 average;
    y512_zero(&average);

    for (uint32_t n = 1; n <= YERBAS_DGW_BLOCKS; ++n) {
        YerbUInt256 target = y256_from_compact(blocks[n - 1].target);
        if (n == 1) {
            average = y512_from_256(target);
        } else {
            y512_mul_small(&average, n);
            y512_add_256(&average, target);
            y512_div_small(&average, n + 1);
        }
    }

    int64_t actual = (int64_t)blocks[0].timestamp -
                     (int64_t)blocks[YERBAS_DGW_BLOCKS - 1].timestamp;
    const int64_t expected = (int64_t)YERBAS_DGW_BLOCKS * YERBAS_TARGET_SPACING;
    if (actual < expected / 3) actual = expected / 3;
    if (actual > expected * 3) actual = expected * 3;

    y512_mul_small(&average, (uint64_t)actual);
    y512_div_small(&average, (uint64_t)expected);

    YerbUInt256 result = y512_trim256(&average);
    if (y256_is_zero(&result) || y256_cmp(&result, &powLimit) > 0) result = powLimit;
    return y256_compact(&result);
}

int BRYerbasDGWVerifyTarget(uint32_t candidateTarget,
                           const BRYerbasDGWBlock *blocks,
                           size_t count)
{
    return candidateTarget == BRYerbasDGWNextTarget(blocks, count);
}
