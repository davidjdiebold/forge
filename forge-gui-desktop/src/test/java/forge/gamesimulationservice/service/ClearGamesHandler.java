package forge.gamesimulationservice.service;

import forge.gamesimulationservice.model.ClearGamesRequest;
import forge.gamesimulationservice.model.ClearGamesResponse;

public class ClearGamesHandler implements TypedHandler<ClearGamesRequest, ClearGamesResponse> {
    private final GameRunnerController _controller;

    public ClearGamesHandler(GameRunnerController controller) {
        _controller = controller;
    }

    @Override
    public ClearGamesResponse handle(ClearGamesRequest clearGamesRequest) {
        _controller.clearGames();
        return new ClearGamesResponse();
    }
}
