#!/usr/bin/env bash
set -e

if [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
  echo "Initializing SDKMAN environment..."
  source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

if command -v jdk17 >/dev/null 2>&1; then
  . jdk17
fi

java_version_line="$(java -version 2>&1 | head -n1)"
if [[ $java_version_line =~ \"([0-9]+)(\.([0-9]+))? ]]; then
  java_major="${BASH_REMATCH[1]}"
  if [[ "$java_major" == "1" ]]; then
    java_major="${BASH_REMATCH[3]}"
  fi
fi

if [[ "$java_major" != "17" ]]; then
  echo "Error: Java 17 is required. Found: $java_version_line in $(which java)}" >&2
  exit 1
fi

./gradlew -PincludeIntegrationTests clean release
