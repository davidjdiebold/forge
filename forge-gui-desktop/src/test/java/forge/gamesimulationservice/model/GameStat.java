package forge.gamesimulationservice.model;

public class GameStat {
    private String gameId;
    private int nbWin;
    private int nbLoss;

    public GameStat() {
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public int getNbWin() {
        return nbWin;
    }

    public void setNbWin(int nbWin) {
        this.nbWin = nbWin;
    }

    public int getNbLoss() {
        return nbLoss;
    }

    public void setNbLoss(int nbLoss) {
        this.nbLoss = nbLoss;
    }

    public void addWinLoss(boolean hasWon) {
        if(hasWon) {
            nbWin++;
        } else {
            nbLoss++;
        }
    }
}
