package forge.gamesimulationservice.service;

import forge.gamesimulationservice.model.GetGameStatsRequest;
import forge.gamesimulationservice.model.GetGameStatsResponse;

public class GameStatHandler implements TypedHandler<GetGameStatsRequest, GetGameStatsResponse> {

    private final GameRunnerController _controller;

    public GameStatHandler(GameRunnerController controller) {
        _controller = controller;
    }

    @Override
    public GetGameStatsResponse handle(GetGameStatsRequest getGameStatsRequest) {
        return _controller.post(getGameStatsRequest);
    }
}
