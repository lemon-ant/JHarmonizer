#!/usr/bin/env bash
set -euo pipefail

if command -v mise >/dev/null 2>&1 && mise where java@21 >/dev/null 2>&1; then
  exec mise x java@21 -- mvn "$@"
fi

if [[ "${JAVA_HOME:-}" == *"/java/21."* ]] || [[ "${JAVA_HOME:-}" == *"/java/21/"* ]]; then
  exec mvn "$@"
fi

echo "ERROR: Java 21 is required. Install it (e.g. via mise) and run: mise x java@21 -- mvn <args>" >&2
exit 1
