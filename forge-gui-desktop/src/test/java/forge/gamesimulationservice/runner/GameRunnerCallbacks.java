package forge.gamesimulationservice.runner;

import forge.game.Game;

public interface GameRunnerCallbacks {
    forge.gamesimulationservice.model.Game getGame();
    void afterGameRun(forge.gamesimulationservice.model.Game decks, Game outcome);
}