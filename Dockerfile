FROM alpine:latest

# libc++-dev is for exception handling
RUN apk add --no-cache bash curl clang libc++-dev alpine-sdk lld musl-dev openjdk17-jre-headless
RUN curl -fLo /usr/local/bin/coursier https://github.com/coursier/launchers/raw/master/coursier.jar && chmod +x /usr/local/bin/coursier
RUN coursier install sbt
ENV PATH="$PATH:/root/.local/share/coursier/bin"
WORKDIR /work
