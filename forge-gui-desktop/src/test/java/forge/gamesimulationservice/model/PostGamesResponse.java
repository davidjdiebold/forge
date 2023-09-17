package forge.gamesimulationservice.model;

public class PostGamesResponse {
    private String status = "ok";

    public PostGamesResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
