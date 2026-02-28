package forge.gamesimulationservice.model;

public class Game {
    private String id;
    private int weight;

    private Deck[] decks;

    private DrawSchedule[] drawSchedules;

    private int randomSeed;

    private boolean isOneShot;

    public Game() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Deck[] getDecks() {
        return decks;
    }

    public void setDecks(Deck[] decks) {
        this.decks = decks;
    }

    public DrawSchedule[] getDrawSchedules() {
        return drawSchedules;
    }

    public void setDrawSchedules(DrawSchedule[] drawSchedules) {
        this.drawSchedules = drawSchedules;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(int randomSeed) {
        this.randomSeed = randomSeed;
    }

    public boolean isOneShot() {
        return isOneShot;
    }

    public void setOneShot(boolean oneShot) {
        isOneShot = oneShot;
    }
}
