package forge.gamesimulationservice.model;

public class GameOutcome {
    private String gameId;
    private boolean isWin;
    private String[] cardsDrawn;
    private int lastTurnNumber;

    public GameOutcome() {
    }

    public int getLastTurnNumber() {
        return lastTurnNumber;
    }

    public void setLastTurnNumber(int lastTurnNumber) {
        this.lastTurnNumber = lastTurnNumber;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean isWin() {
        return isWin;
    }

    public void setWin(boolean win) {
        isWin = win;
    }

    public String[] getCardsDrawn() {
        return cardsDrawn;
    }

    public void setCardsDrawn(String[] cardsDrawn) {
        this.cardsDrawn = cardsDrawn;
    }
}