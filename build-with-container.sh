#!/bin/sh

set -eux
set pipefail

docker build -t comport-builder .
docker run --rm -v "$PWD":/work --rm -e SCALA_CLI_LAUNCH='coursier launch scala-cli --' -e LINKFLAGS=-static comport-builder:latest sh /work/build.sh
