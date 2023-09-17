package forge.gamesimulationservice.model;

public class GetGameOutcomesRequest {
    private String[] gameIds;

    public GetGameOutcomesRequest() {
    }

    public String[] getGameIds() {
        return gameIds;
    }

    public void setGameIds(String[] gameIds) {
        this.gameIds = gameIds;
    }
}
