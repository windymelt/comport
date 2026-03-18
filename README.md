# comport

Deterministic port allocator for git worktrees. Assigns a stable, collision-resistant set of port numbers based on the worktree path so that multiple worktrees of the same project can run their services simultaneously without port conflicts.

## How it works

`comport` hashes the git worktree path (FNV-1a 64-bit) and maps it to a slot in a configurable port range (default 10000--59999). Each service gets a consecutive port starting from the slot's base port. Because the hash is deterministic, the same worktree always gets the same ports.

Service names are resolved in the following priority:

1. `--names` flag (comma-separated)
2. Auto-detection from `compose.yaml` / `docker-compose.yaml`
3. Fallback to `--num-services` (numeric indices)

## Installation

Download a prebuilt binary from the [Releases](https://github.com/windymelt/comport/releases) page. Binaries are available for:

- `x86_64-linux`
- `aarch64-linux`
- `x86_64-macos`
- `aarch64-macos`

```sh
# Example: download and install on Linux x86_64
curl -sL https://api.github.com/repos/windymelt/comport/releases/latest \
  | grep -o 'https://[^"]*comport-[^"]*x86_64-linux' \
  | xargs curl -Lo comport
chmod +x comport
sudo mv comport /usr/local/bin/
```

### Build from source

Requires [Scala CLI](https://scala-cli.virtuslab.org/).

```sh
# Run directly
scala-cli run .

# Build native binary
scala-cli --power package . -o comport --native-mode release-fast --native-gc commix
```

## Usage

Run `comport` inside a git repository. It outputs shell `export` statements that you can `eval`:

```sh
eval "$(comport)"
```

### Options

```
  --num-services <int>   Number of services (used when --names is not specified
                         and no compose file is found). Default: 1
  --names <string>       Service names (comma-separated). Overrides compose file
                         auto-detection when specified
  --prefix <string>      Environment variable prefix. Default: PORT
  --base <int>           Start of the port range. Default: 10000
  --range <int>          Width of the port range. Default: 50000
  --show                 Show allocation info on stderr
  --dotenv               Output in .env format without export
```

### Examples

#### Auto-detect from compose file

If your worktree has a `compose.yaml` with services `web` and `db`:

```sh
$ comport --show
export PORT_BASE=34210
export PORT_DB=34210
export PORT_WEB=34211
```

#### Explicit service names

```sh
$ comport --names api,worker,redis --prefix SVC
export SVC_BASE=28430
export SVC_API=28430
export SVC_WORKER=28431
export SVC_REDIS=28432
```

#### .env file output

```sh
comport --dotenv > .env
```

#### Use in docker compose

You can reference the allocated ports in `compose.yaml`:

```yaml
services:
  web:
    ports:
      - "${PORT_WEB}:8080"
  db:
    ports:
      - "${PORT_DB}:5432"
```

```sh
eval "$(comport)" && docker compose up
```

#### Use with direnv

Add the following to your `.envrc`:

```sh
eval "$(comport)"
```

Whenever you `cd` into the worktree, direnv will automatically export the allocated ports.

## License

BSD 3-Clause License. See [LICENSE](LICENSE).
