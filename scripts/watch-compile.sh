#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [[ -d "/c/Program Files/Java/jdk-21" ]]; then
  export JAVA_HOME="/c/Program Files/Java/jdk-21"
elif [[ -d "/mnt/c/Program Files/Java/jdk-21" ]]; then
  export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Watching Java/resources. Keep scripts/run-dev.sh running in another terminal."
echo "When files change, this script runs: mvn compile"

if command -v inotifywait >/dev/null 2>&1; then
  while inotifywait -r -e modify,create,delete,move src/main/java src/main/resources >/dev/null 2>&1; do
    sleep 0.7
    echo "Change detected. Compiling..."
    mvn compile
  done
else
  last_fingerprint=""
  while true; do
    fingerprint="$(
      find src/main/java src/main/resources -type f -print0 \
        | sort -z \
        | xargs -0 stat -c '%n:%Y' 2>/dev/null \
        || find src/main/java src/main/resources -type f -print0 \
          | sort -z \
          | xargs -0 stat -f '%N:%m'
    )"

    if [[ -n "$last_fingerprint" && "$fingerprint" != "$last_fingerprint" ]]; then
      echo "Change detected. Compiling..."
      mvn compile
    fi

    last_fingerprint="$fingerprint"
    sleep 1
  done
fi
