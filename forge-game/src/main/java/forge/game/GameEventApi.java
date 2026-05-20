package forge.game;

public class GameEventApi {
    public String name;
    public int player;
    public int turn;
    public String card;
    public int life;
    public String from;
    public String to;

    public GameEventApi(String name, int player, int turn, String card, int life) {
        this(name, player, turn, card, life, null, null);
    }

    public GameEventApi(String name, int player, int turn, String card, int life, String from, String to) {
        this.name = name;
        this.player = player;
        this.turn = turn;
        this.card = card;
        this.life = life;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public int getPlayer() {
        return player;
    }

    public int getTurn() {
        return turn;
    }

    public String getCard() {
        return card;
    }

    public int getLife() {
        return life;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
