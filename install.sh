sudo apt update
sudo apt install -y openjdk-8-jdk
sudo apt install -y maven

mkdir forge
cd forge
git clone git@github.com:davidjdiebold/forge.git
git fetch
git checkout gamerunnerapi

mvn clean install -DskipTests
mvn -pl forge-gui-desktop dependency:build-classpath -Dmdep.outputFile=/tmp/forge-cp.txt -DincludeScope=test
