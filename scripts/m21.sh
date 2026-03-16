#!/usr/bin/env bash
set -euo pipefail

# 与 fish 的 m21 函数保持一致：临时使用 JDK 21 执行 mvn
DEFAULT_JAVA_HOME="/Users/catovo/Library/Java/JavaVirtualMachines/ms-21.0.8/Contents/Home"
JAVA_HOME_TO_USE="${M21_JAVA_HOME:-$DEFAULT_JAVA_HOME}"

if [[ ! -x "${JAVA_HOME_TO_USE}/bin/java" ]]; then
  echo "m21.sh error: JAVA_HOME does not contain bin/java: ${JAVA_HOME_TO_USE}" >&2
  echo "You can override by setting M21_JAVA_HOME, for example:" >&2
  echo "  M21_JAVA_HOME=/path/to/jdk21 ./scripts/m21.sh clean compile" >&2
  exit 1
fi

export JAVA_HOME="${JAVA_HOME_TO_USE}"
export PATH="${JAVA_HOME}/bin:${PATH}"

exec mvn "$@"
