package forge.gamesimulationservice.model;

public class GetDecksResponse {
    private Deck[] decks;

    public GetDecksResponse() {
    }

    public Deck[] getDecks() {
        return decks;
    }

    public void setDecks(Deck[] decks) {
        this.decks = decks;
    }
}
