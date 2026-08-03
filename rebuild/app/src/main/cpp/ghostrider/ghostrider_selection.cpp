#include "ghostrider_selection.h"

#include <algorithm>
#include <stdexcept>

namespace yerbas::ghostrider {
namespace {

std::uint8_t nibble(const std::array<std::uint8_t, 32>& hash, int index) {
    if (index < 0 || index > 63) throw std::out_of_range("nibble index");
    // Mirrors uint256::GetNibble(): nibble 0 is the low nibble of byte 0.
    const std::uint8_t byte = hash[static_cast<std::size_t>(index / 2)];
    return (index & 1) == 0 ? static_cast<std::uint8_t>(byte & 0x0f)
                            : static_cast<std::uint8_t>((byte >> 4) & 0x0f);
}

template <std::size_t N>
std::array<int, N> permute(const std::array<std::uint8_t, 32>& prevHash,
                           std::array<int, N> available) {
    std::array<int, N> result{};
    std::size_t count = 0;
    for (int i = 63; i >= 0 && count < N; --i) {
        std::size_t selected = nibble(prevHash, i);
        if (selected >= N) selected %= N;
        if (available[selected] >= 0) {
            result[count++] = available[selected];
            available[selected] = -1;
        }
    }
    for (std::size_t i = 0; i < N && count < N; ++i) {
        if (available[i] >= 0) result[count++] = available[i];
    }
    if (count != N) throw std::logic_error("incomplete GhostRider selection");
    return result;
}

} // namespace

Selection selectAlgorithms(const std::array<std::uint8_t, 32>& prevHash) {
    Selection selection;
    selection.core = permute(prevHash, std::array<int, 15>{
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14});
    selection.cn = permute(prevHash, std::array<int, 6>{0, 1, 2, 3, 4, 5});
    return selection;
}

} // namespace yerbas::ghostrider
