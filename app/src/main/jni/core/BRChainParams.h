//
// Yerbas SPV chain parameters
//
#ifndef BRChainParams_h
#define BRChainParams_h

#include "BRMerkleBlock.h"
#include "BRSet.h"
#include <stddef.h>
#include <stdint.h>

typedef struct {
    uint32_t height;
    UInt256 hash;
    uint32_t timestamp;
    uint32_t target;
} BRCheckPoint;

typedef struct {
    const char * const *dnsSeeds;
    uint16_t standardPort;
    uint32_t magicNumber;
    uint64_t services;
    int (*verifyDifficulty)(const BRMerkleBlock *block, const BRSet *blockSet);
    const BRCheckPoint *checkpoints;
    size_t checkpointsCount;
} BRChainParams;

static const char *BRMainNetDNSSeeds[] = {
    "weednode00.yerbas.org.",
    "weednode01.yerbas.org.",
    "weednode02.yerbas.org.",
    "weednode03.yerbas.org.",
    "weednode420.yerbas.org.",
    "weednode05.yerbas.org.",
    NULL
};

static const char *BRTestNetDNSSeeds[] = {
    "weednode00.yerbas.org.",
    NULL
};

// Trusted Yerbas checkpoints. Checkpoints allow fast SPV startup and prevent
// the wallet from accepting a chain that diverges before a known-good block.
static const BRCheckPoint BRMainNetCheckpoints[] = {
    {       0, u256_hex_decode("eff0bbe5c1bbe1ef8da54822a18f528d6dc58232990bdb86e0a77ab2814ed12c"), 1652138420, 0x20001fff },
    {    1420, u256_hex_decode("9cf529c09eaf90f1b1b96c681c203aa80e1840b1709c928ab6e840b562d54c34"), 0, 0 },
    {    8837, u256_hex_decode("25c1c019d70e6990d3ed680fa9703cb84d620008a6cb8f635bbcdcef913dfbae"), 0, 0 },
    {   16460, u256_hex_decode("6cb79413f86770f856556ce641e15beb45656915db9ce4511cc9d4853b532fe5"), 0, 0 },
    {   42069, u256_hex_decode("729ee24deac4d1df060191debdf52079a70987dc2001e058002641a1412d3782"), 0, 0 },
    {  108069, u256_hex_decode("15280a0f159f2739a3e6658bbc68534cefa79b14f5a250e1e4f724b5b3985998"), 0, 0 },
    {  209639, u256_hex_decode("c99ce3a58ba3828a1a09469d0afedb91e8238e6cb4fd2bc3970c9ff56bbbb528"), 0, 0 },
    {  310420, u256_hex_decode("5e36ff7864c6ef90a165d28b702884fa1c91be4ffea3111d9fbc3724fa6f410f"), 0, 0 },
    {  410420, u256_hex_decode("b21e783ad0b134010e08f1641e425fc4cf74db586bf928ca4ed202da21c50be5"), 0, 0 },
    {  811220, u256_hex_decode("be1c1caf6166786435a81c81b61d809f98823641dbe64b15516ad59b3e4bce10"), 0, 0 },
    { 1060820, u256_hex_decode("f70aa712f4d467d4e28e431ac2b8851c37e9335669075b4c25f072b1b032feb0"), 0, 0 }
};

static const BRCheckPoint BRTestNetCheckpoints[] = {
    { 0, UINT256_ZERO, 0, 0 }
};

// Yerbas uses Dark Gravity Wave rather than Bitcoin's 2016-block retarget.
// The legacy Bitcoin verifier rejected valid Yerbas headers. Until the native
// DGW verifier is shared with Core, header continuity, PoW and checkpoints are
// enforced while the Bitcoin-only retarget check is disabled.
static int BRMainNetVerifyDifficulty(const BRMerkleBlock *block, const BRSet *blockSet)
{
    (void)block;
    (void)blockSet;
    return 1;
}

static int BRTestNetVerifyDifficulty(const BRMerkleBlock *block, const BRSet *blockSet)
{
    (void)block;
    (void)blockSet;
    return 1;
}

static const BRChainParams BRMainNetParams = {
    BRMainNetDNSSeeds,
    15420,
    0x62726579, // wire bytes: 79 65 72 62 ("yerb") on little-endian hosts
    0,
    BRMainNetVerifyDifficulty,
    BRMainNetCheckpoints,
    sizeof(BRMainNetCheckpoints)/sizeof(*BRMainNetCheckpoints)
};

static const BRChainParams BRTestNetParams = {
    BRTestNetDNSSeeds,
    15421,
    0x74726579, // provisional testnet wire bytes: "yert"
    0,
    BRTestNetVerifyDifficulty,
    BRTestNetCheckpoints,
    sizeof(BRTestNetCheckpoints)/sizeof(*BRTestNetCheckpoints)
};

#endif // BRChainParams_h
