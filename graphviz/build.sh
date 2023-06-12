#!/usr/bin/env bash

set -e
cd "$(dirname "$0")"

# This script builds the graphviz library for WebAssembly.
# It generates graphviz.wasm, and embeds it into the common kotlin source code, for easy deployment.
# It only requires docker and standard linux tools.

GRAPHVIZ_VERSION="${GRAPHVIZ_VERSION:-latest}"
GRAPHVIZ_REPO="${GRAPHVIZ_REPO:-https://gitlab.com/graphviz/graphviz.git}"
EMBEDDED_KT_FILE="${KOTLIN_EMBEDDED_WASM:-../src/commonMain/kotlin/com/github/yeicor/kraphviz/Graphviz.kt}"

# Figure out the version of graphviz we're building
if [ "$GRAPHVIZ_VERSION" = "latest" ]; then
  # Get the latest version from the git repo
  GRAPHVIZ_VERSION="$(git ls-remote --tags "$GRAPHVIZ_REPO" | grep -o '/[0-9]\+\.[0-9]\+\.[0-9]\+$' | sed 's,/,,' | sort -V | tail -n1)"
fi
echo "Building graphviz v$GRAPHVIZ_VERSION..."

# Start docker build
docker build -t graphviz-build --build-arg GRAPHVIZ_VERSION="$GRAPHVIZ_VERSION" .

# Extract the wasm file from the docker image
docker create --name graphviz-build graphviz-build --command ignoreme
docker cp graphviz-build:/graphviz.wasm graphviz.wasm
docker rm graphviz-build

# Embed the wasm file into the kotlin source code. TODO: Compression? Resources instead of source code?
mkdir -p "$(dirname "$EMBEDDED_KT_FILE")"
cat > "$EMBEDDED_KT_FILE" <<-EOF
package $(dirname "${EMBEDDED_KT_FILE##*/src/commonMain/kotlin/}" | tr / .)

internal object Graphviz {
  internal val WASM_B64: List<String> =
      listOf(
          "$(base64 --wrap=$((64*1024)) graphviz.wasm | tr '\n' '!' | sed -E 's/!$//' | sed 's/!/",\n          "/g')")
}
EOF
