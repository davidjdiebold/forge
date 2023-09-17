package forge.gamesimulationservice.model;

public class PostGamesRequest {
    private Game[] games;

    public PostGamesRequest() {
    }

    public Game[] getGames() {
        return games;
    }

    public void setGames(Game[] games) {
        this.games = games;
    }
}
