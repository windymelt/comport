#!/bin/sh

set -eux
set pipefail

docker build -t comport-builder .
docker run --rm -v "$PWD":/work \
  -e LINKFLAGS=-static \
  -e COMPORT_VERSION="${COMPORT_VERSION:-}" \
  comport-builder:latest sh /work/build.sh
