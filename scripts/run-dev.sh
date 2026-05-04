#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [[ -f ".env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

if [[ -d "/c/Program Files/Java/jdk-21" ]]; then
  export JAVA_HOME="/c/Program Files/Java/jdk-21"
elif [[ -d "/mnt/c/Program Files/Java/jdk-21" ]]; then
  export JAVA_HOME="/mnt/c/Program Files/Java/jdk-21"
fi

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export VAADIN_USAGE_STATS_ENABLED=false
export SPRING_DEVTOOLS_RESTART_ENABLED=true

VAADIN_HOME="$REPO_ROOT/target/vaadin-home"
mkdir -p "$VAADIN_HOME/.vaadin"

VAADIN_HOME_JVM="$VAADIN_HOME"
if command -v cygpath >/dev/null 2>&1; then
  VAADIN_HOME_JVM="$(cygpath -w "$VAADIN_HOME")"
fi

docker compose up -d

mvn spring-boot:run \
  "-Dspring-boot.run.profiles=dev" \
  "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=true -Duser.home=$VAADIN_HOME_JVM"
