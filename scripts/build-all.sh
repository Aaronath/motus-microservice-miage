#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
mvn clean package -DskipTests -q
echo "Build OK — artefacts dans */target/*.jar"
