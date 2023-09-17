package forge.gamesimulationservice.model;

import forge.gamesimulationservice.service.GameRunnerController;
import forge.gamesimulationservice.service.TypedHandler;

public class GameOutcomesHandler implements TypedHandler<GetGameOutcomesRequest, GetGameOutcomesResponse> {

    private final GameRunnerController _controller;

    public GameOutcomesHandler(GameRunnerController controller) {
        _controller = controller;
    }

    @Override
    public GetGameOutcomesResponse handle(GetGameOutcomesRequest request) {
        return _controller.post(request);
    }
}
