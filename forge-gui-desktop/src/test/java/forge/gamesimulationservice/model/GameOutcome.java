package forge.gamesimulationservice.model;

import forge.game.GameEventApi;

public class GameOutcome {
    private String gameId;
    private boolean isWin;
    private int lastTurnNumber;
    private int firstPlayer;
    private GameEventApi[] events;

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

    public GameEventApi[] getEvents() {
        return events;
    }

    public void setEvents(GameEventApi[] events) {
        this.events = events;
    }

    public int getFirstPlayer() {
        return firstPlayer;
    }

    public void setFirstPlayer(int iPlayer) {
        this.firstPlayer = iPlayer;
    }
}