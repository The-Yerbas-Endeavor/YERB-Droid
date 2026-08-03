// Minimal native regression tests for Yerbas wire primitives.

#include "BRYerbasProtocol.h"
#include <assert.h>
#include <string.h>

static void test_special_transaction_version(void)
{
    uint32_t raw = ((uint32_t)1u << 16) | 3u;
    assert(BRYerbasTxBaseVersion(raw) == 3u);
    assert(BRYerbasTxType(raw) == 1u);
    assert(BRYerbasTxHasExtraPayload(raw));
    assert(!BRYerbasTxHasExtraPayload(3u));
}

static void test_header_round_trip(void)
{
    uint8_t serialized[YERB_HEADER_SIZE];
    BRYerbasBlockHeader input;
    BRYerbasBlockHeader output;

    memset(&input, 0, sizeof(input));
    input.version = 4;
    input.timestamp = 1652138420u;
    input.target = 0x20001fffu;
    input.nonce = 3397u;

    assert(BRYerbasBlockHeaderSerialize(&input, serialized, sizeof(serialized)));
    assert(BRYerbasBlockHeaderParse(&output, serialized, sizeof(serialized)));
    assert(output.version == input.version);
    assert(output.timestamp == input.timestamp);
    assert(output.target == input.target);
    assert(output.nonce == input.nonce);
    assert(memcmp(output.prevBlock.u8, input.prevBlock.u8, sizeof(UInt256)) == 0);
    assert(memcmp(output.merkleRoot.u8, input.merkleRoot.u8, sizeof(UInt256)) == 0);
}

static void test_compact_target(void)
{
    UInt256 target;
    UInt256 zero = UINT256_ZERO;

    assert(BRYerbasCompactTarget(&target, 0x20001fffu));
    assert(BRYerbasHashMeetsTarget(zero, target));
    assert(!BRYerbasCompactTarget(&target, 0x20801fffu));
    assert(!BRYerbasCompactTarget(&target, 0u));
}

int main(void)
{
    assert(YERB_PROTOCOL_VERSION == 70223u);
    assert(YERB_MAINNET_MAGIC == 0x62726579u);
    assert(YERB_MAINNET_PORT == 15420u);
    assert(YERB_HEADER_SIZE == 80u);
    assert(YERB_DGW_PAST_BLOCKS == 60u);
    assert(YERB_TARGET_SPACING == 120u);

    test_special_transaction_version();
    test_header_round_trip();
    test_compact_target();
    return 0;
}
