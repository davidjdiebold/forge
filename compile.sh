#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="/home/d.diebold/devtools/sdkman/candidates/java/8.0.292.hs-adpt"
export PATH="${JAVA_HOME}/bin:$PATH"

mvn clean install -DskipTests
mvn -pl forge-gui-desktop dependency:build-classpath -Dmdep.outputFile=/tmp/forge-cp.txt -DincludeScope=test
