#!/usr/bin/env bash
set -euo pipefail

mvn clean install -DskipTests
mvn -pl forge-gui-desktop dependency:build-classpath -Dmdep.outputFile=/tmp/forge-cp.txt -DincludeScope=test
