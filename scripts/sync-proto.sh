#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="${ROOT_DIR}/../server/proto/sidekick/v1/remote.proto"
DST="${ROOT_DIR}/proto/sidekick/v1/remote.proto"

if [[ ! -f "${SRC}" ]]; then
  echo "error: source proto not found at ${SRC}" >&2
  exit 1
fi

if [[ "${1:-}" == "--check" ]]; then
  if [[ ! -f "${DST}" ]]; then
    echo "error: destination proto missing at ${DST}; run ./scripts/sync-proto.sh" >&2
    exit 1
  fi
  SRC_SUM="$(sha256sum "${SRC}" | awk '{print $1}')"
  DST_SUM="$(sha256sum "${DST}" | awk '{print $1}')"
  if [[ "${SRC_SUM}" != "${DST_SUM}" ]]; then
    echo "error: proto drift detected (${SRC_SUM} != ${DST_SUM}); run ./scripts/sync-proto.sh" >&2
    exit 1
  fi
  echo "proto in sync (${DST_SUM})"
  exit 0
fi

mkdir -p "$(dirname "${DST}")"
cp "${SRC}" "${DST}"
echo "synced ${DST}"
