package forge.gamesimulationservice.model;

public class GetGameOutcomesResponse {
    private GameOutcome[] outcomes;

    public GetGameOutcomesResponse() {
    }

    public GameOutcome[] getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(GameOutcome[] outcomes) {
        this.outcomes = outcomes;
    }
}
