package forge.gamesimulationservice.model;

public class RunGameSyncResponse {
    private GameOutcome outcome;

    public RunGameSyncResponse() {
    }

    public GameOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(GameOutcome outcome) {
        this.outcome = outcome;
    }
}
