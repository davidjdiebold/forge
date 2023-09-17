package forge.gamesimulationservice.service;

public interface TypedHandler<Request, Response> {
    public Response handle(Request request);
}
