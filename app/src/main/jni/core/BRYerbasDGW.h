// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#ifndef BRYerbasDGW_h
#define BRYerbasDGW_h

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define YERBAS_DGW_BLOCKS 60u
#define YERBAS_TARGET_SPACING 120u
#define YERBAS_POW_LIMIT_COMPACT 0x2000ffffu

typedef struct {
    uint32_t timestamp;
    uint32_t target;
} BRYerbasDGWBlock;

// Calculates the exact DarkGravityWave target used by current Yerbas Core.
// Blocks must be ordered newest first and contain at least 60 entries.
// Returns powLimit when fewer than 60 blocks are supplied.
uint32_t BRYerbasDGWNextTarget(const BRYerbasDGWBlock *blocks, size_t count);

// Returns non-zero when candidateTarget is the exact target expected from history.
int BRYerbasDGWVerifyTarget(uint32_t candidateTarget,
                           const BRYerbasDGWBlock *blocks,
                           size_t count);

#ifdef __cplusplus
}
#endif

#endif // BRYerbasDGW_h
