#!/bin/sh
# Minimal gradlew shim — downloads Gradle if needed
set -eu
DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
GRADLE_VERSION="8.6"
GRADLE_HOME="$HOME/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
JAR="$GRADLE_HOME/gradle-${GRADLE_VERSION}/lib/gradle-launcher-${GRADLE_VERSION}.jar"
if [ ! -f "$JAR" ]; then
  echo "Downloading Gradle $GRADLE_VERSION..." >&2
  mkdir -p "$GRADLE_HOME"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  TMP="$(mktemp -d 2>/dev/null || mktemp -d -t gradle_dl)"
  if command -v curl >/dev/null 2>&1; then curl -L "$URL" -o "$TMP/gradle.zip"
  elif command -v wget >/dev/null 2>&1; then wget -O "$TMP/gradle.zip" "$URL"
  else echo "Need curl or wget" >&2; exit 1; fi
  if command -v unzip >/dev/null 2>&1; then unzip -q "$TMP/gradle.zip" -d "$TMP"
  else python3 -c "import zipfile; zipfile.ZipFile('$TMP/gradle.zip').extractall('$TMP')"
  fi
  mkdir -p "$DIR/gradle/wrapper"
  # fall back to system gradle if unzip layout unexpected
  if [ -f "$TMP/gradle-${GRADLE_VERSION}/lib/gradle-launcher-${GRADLE_VERSION}.jar" ]; then
    mkdir -p "$GRADLE_HOME/gradle-${GRADLE_VERSION}"
    cp -r "$TMP/gradle-${GRADLE_VERSION}/"* "$GRADLE_HOME/gradle-${GRADLE_VERSION}/" 2>/dev/null || true
  fi
  rm -rf "$TMP"
fi
if [ -f "$JAR" ]; then
  exec java -jar "$JAR" "$@"
else
  if command -v gradle >/dev/null 2>&1; then exec gradle "$@"
  else echo "Gradle not found. Install Gradle 8.6 or JDK 17." >&2; exit 1; fi
fi
