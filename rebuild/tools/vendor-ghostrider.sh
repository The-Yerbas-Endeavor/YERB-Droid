#!/usr/bin/env bash
set -euo pipefail

YERBAS_REPO="https://github.com/The-Yerbas-Endeavor/yerbas.git"
YERBAS_COMMIT="65c6b6336bf9cc0eb7fa125ed5aa983a17e6fe7a"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/cpp/ghostrider/vendor"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

git clone --filter=blob:none --no-checkout "$YERBAS_REPO" "$TMP/yerbas"
git -C "$TMP/yerbas" checkout "$YERBAS_COMMIT" -- \
  src/hash_selection.cpp src/hash_selection.h src/hash.h \
  src/crypto src/cryptonote src/uint256.h src/arith_uint256.h

rm -rf "$DEST"
mkdir -p "$DEST"
cp -a "$TMP/yerbas/src/." "$DEST/"
printf '%s\n' "$YERBAS_COMMIT" > "$DEST/YERBAS_COMMIT"

echo "Vendored GhostRider sources from $YERBAS_COMMIT into $DEST"
