package forge.gamesimulationservice.service;

import forge.gamesimulationservice.model.GetDecksRequest;
import forge.gamesimulationservice.model.GetDecksResponse;

public class DecksHandler implements TypedHandler<GetDecksRequest, GetDecksResponse> {
    private final GameRunnerController _controller;

    public DecksHandler(GameRunnerController controller) {
        _controller = controller;
    }

    @Override
    public GetDecksResponse handle(GetDecksRequest getDecksRequest) {
        return _controller.post(getDecksRequest);
    }
}
