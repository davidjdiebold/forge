CP=$(cat /tmp/forge-cp.txt)
cd forge-gui-desktop
java -cp "target/classes:target/test-classes:../forge-gui/res/languages:$CP" forge.gamesimulationservice.GameSimulationServiceMain
