#!/bin/sh

# Build script using sbt. Overwrites `./comport` if exists.
# LINKFLAGS (whitespace-separated) is picked up by build.sbt and appended
# to nativeConfig.linkingOptions (used e.g. for `-static` in the Alpine
# container build).

set -eux
set pipefail

LINKFLAGS=${LINKFLAGS:-}
SBT_LAUNCH=${SBT_LAUNCH:-sbt}

export LINKFLAGS

${SBT_LAUNCH} -Dsbt.color=never nativeLink

cp target/scala-3.8.2/comport-out ./comport
