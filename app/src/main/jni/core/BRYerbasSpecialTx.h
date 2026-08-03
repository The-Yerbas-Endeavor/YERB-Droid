// Copyright (c) 2026 The Yerbas developers
// Distributed under the MIT software license.

#ifndef BRYerbasSpecialTx_h
#define BRYerbasSpecialTx_h

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
    uint16_t version;
    uint16_t type;
    uint32_t lockTime;
    const uint8_t *extraPayload;
    size_t extraPayloadLen;
    size_t serializedLen;
} BRYerbasSpecialTxInfo;

// Parses the common transaction envelope used by Yerbas/Dash special transactions.
// This does not interpret type-specific payload contents; it safely locates and
// preserves the CompactSize-prefixed extra payload following nLockTime.
int BRYerbasParseSpecialTxEnvelope(const uint8_t *tx,
                                   size_t txLen,
                                   BRYerbasSpecialTxInfo *info);

#ifdef __cplusplus
}
#endif

#endif // BRYerbasSpecialTx_h
