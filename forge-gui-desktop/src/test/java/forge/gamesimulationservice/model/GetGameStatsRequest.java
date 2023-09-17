package forge.gamesimulationservice.model;

public class GetGameStatsRequest {
    private String[] gameIds;

    public GetGameStatsRequest() {
    }

    public String[] getGameIds() {
        return gameIds;
    }

    public void setGameIds(String[] gameIds) {
        this.gameIds = gameIds;
    }
}
