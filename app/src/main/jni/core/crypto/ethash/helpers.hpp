// ethash: C/C++ implementation of Ethash, the Ethereum Proof of Work algorithm.
// Copyright 2018-2019 Pawel Bylica.
// Licensed under the Apache License, Version 2.0.

#pragma once

#include "ethash.hpp"

#include <string>

template <typename Hash>
inline std::string to_hex(const Hash& h)
{
    static const auto hex_chars = "0123456789abcdef";
    std::string str;
    str.reserve(sizeof(h) * 2);
    for (auto b : h.bytes)
    {
        str.push_back(hex_chars[uint8_t(b) >> 4]);
        str.push_back(hex_chars[uint8_t(b) & 0xf]);
    }
    return str;
}

inline ethash_hash256 to_hash256(const std::string& hex)
{
    auto parse_digit = [](char d) -> int { return d <= '9' ? (d - '0') : (d - 'a' + 10); };

    ethash_hash256 hash = {};
    for (size_t i = 1; i < hex.size(); i += 2)
    {
        int h = parse_digit(hex[i - 1]);
        int l = parse_digit(hex[i]);
        hash.bytes[i / 2] = uint8_t((h << 4) | l);
    }
    return hash;
}

/// Comparison operator for hash256 to be used in unit tests.
inline bool operator==(const ethash_hash256& a, const ethash_hash256& b) noexcept
{
    return std::memcmp(a.bytes, b.bytes, sizeof(a)) == 0;
}

inline bool operator!=(const ethash_hash256& a, const ethash_hash256& b) noexcept
{
    return !(a == b);
}
