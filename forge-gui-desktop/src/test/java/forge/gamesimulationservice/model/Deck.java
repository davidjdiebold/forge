package forge.gamesimulationservice.model;

public class Deck {
    private int id;
    private CardCount[] main;

    public Deck() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CardCount[] getMain() {
        return main;
    }

    public void setMain(CardCount[] main) {
        this.main = main;
    }
}
