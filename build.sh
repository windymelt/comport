#!/bin/sh

# Build script using scala-cli. Overwrites `./comport` if exists.

set -eux
set pipefail

LINKFLAGS=${LINKFLAGS:-}
COURSIER=${COURSIER:-cs}
SCALA_CLI_LAUNCH=${SCALA_CLI_LAUNCH:-scala-cli}

${SCALA_CLI_LAUNCH} --power package . -o comport -f \
          --native-mode release-fast \
          --native-lto full \
          --native-gc commix \
          --native-linking "${LINKFLAGS}"
