#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
build_dir="$project_dir/.build"

if (( $# > 1 )); then
  printf 'Usage: bash run-tests.sh [--followup]\n' >&2
  exit 2
fi

case "${1:-}" in
  '') test_class='demo.inventory.InventoryLedgerTest' ;;
  --followup) test_class='demo.inventory.FollowupTest' ;;
  *) printf 'Usage: bash run-tests.sh [--followup]\n' >&2; exit 2 ;;
esac

if ! command -v java >/dev/null 2>&1; then
  printf 'Java 17+ with a Java compiler is required. No dependencies are downloaded.\n' >&2
  exit 2
fi

# Remove only this exercise's own generated build directory.
rm -rf -- "$build_dir"
mkdir -p -- "$build_dir"

sources=(
  "$project_dir"/src/main/java/demo/inventory/*.java
  "$project_dir"/src/test/java/demo/inventory/*.java
)

if command -v javac >/dev/null 2>&1; then
  compiler=(javac)
  printf 'Compiler: javac\n'
else
  compiler=(java -m jdk.compiler/com.sun.tools.javac.Main)
  printf 'Compiler: JDK compiler module\n'
fi

compiler_log="$build_dir/compiler.log"
if "${compiler[@]}" --release 17 -encoding UTF-8 \
    -d "$build_dir" "${sources[@]}" >"$compiler_log" 2>&1; then
  cat "$compiler_log"
else
  compiler_status=$?
  compiler_output="$(<"$compiler_log")"
  if [[ "$compiler_output" == *'release version 17 not supported'* ]]; then
    # Some trimmed Java runtimes contain jdk.compiler but omit ct.sym.
    # This fallback checks Java 17 syntax and bytecode, not its API surface.
    printf 'Java 17 release symbols unavailable; using -source 17 -target 17.\n'
    "${compiler[@]}" -source 17 -target 17 -encoding UTF-8 \
      -d "$build_dir" "${sources[@]}"
  else
    printf '%s\n' "$compiler_output" >&2
    exit "$compiler_status"
  fi
fi

java -cp "$build_dir" "$test_class"
