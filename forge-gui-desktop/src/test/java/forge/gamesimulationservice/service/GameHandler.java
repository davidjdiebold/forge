package forge.gamesimulationservice.service;

import forge.gamesimulationservice.model.PostGamesRequest;
import forge.gamesimulationservice.model.PostGamesResponse;

public class GameHandler implements TypedHandler<PostGamesRequest, PostGamesResponse> {

    private final GameRunnerController _controller;

    public GameHandler(GameRunnerController controller) {
        _controller = controller;
    }

    @Override
    public PostGamesResponse handle(PostGamesRequest postGamesRequest) {
        return _controller.post(postGamesRequest);
    }
}
