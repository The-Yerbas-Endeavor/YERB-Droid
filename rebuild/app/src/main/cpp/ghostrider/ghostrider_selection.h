#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

namespace yerbas::ghostrider {

struct Selection {
    std::array<int, 15> core{};
    std::array<int, 6> cn{};
};

// prevHash is the 32-byte internal/wire-order previous block hash used by Yerbas Core.
Selection selectAlgorithms(const std::array<std::uint8_t, 32>& prevHash);

} // namespace yerbas::ghostrider
