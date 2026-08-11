#!/bin/sh

# Build script using sbt. Overwrites `./comport` if exists.
# LINKFLAGS (whitespace-separated) is picked up by build.sbt and appended
# to nativeConfig.linkingOptions (used e.g. for `-static` in the Alpine
# container build).
# COMPORT_VERSION is picked up by build.sbt and baked into BuildInfo, which
# `comport --version` prints.

set -eux
set pipefail

LINKFLAGS=${LINKFLAGS:-}
SBT_LAUNCH=${SBT_LAUNCH:-sbt}

export LINKFLAGS

# --server forces the JVM launcher and skips sbtn (the thin native client).
# sbt 2's sbtn is glibc-linked and does not run on Alpine/musl.
${SBT_LAUNCH} --server -Dsbt.color=never nativeLink

cp target/out/native0.5/scala-3.8.2/comport/comport ./comport
