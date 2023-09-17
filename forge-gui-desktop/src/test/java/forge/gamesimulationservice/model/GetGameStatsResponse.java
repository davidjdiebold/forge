package forge.gamesimulationservice.model;

public class GetGameStatsResponse {
    private GameStat[] gameStats;

    public GetGameStatsResponse() {
    }

    public GameStat[] getGameStats() {
        return gameStats;
    }

    public void setGameStats(GameStat[] gameStats) {
        this.gameStats = gameStats;
    }
}