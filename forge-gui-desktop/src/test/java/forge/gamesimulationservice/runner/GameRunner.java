package forge.gamesimulationservice.runner;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.tournament.system.AbstractTournament;
import forge.gamemodes.tournament.system.TournamentPairing;
import forge.gamemodes.tournament.system.TournamentPlayer;
import forge.gamesimulationservice.service.ApiAdapters;
import forge.player.GamePlayerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameRunner {

    private final GameRunnerCallbacks _callbacks;
    private final GameRules _rules;

    private final AtomicBoolean _isUp = new AtomicBoolean(false);
    private final int _nbExecutors = (int) (Runtime.getRuntime().availableProcessors() * 0.85);
    private final ExecutorService _executorService = Executors.newFixedThreadPool(_nbExecutors);

    private final ExecutorService _timeoutPool = Executors.newCachedThreadPool();

    public GameRunner(GameRunnerCallbacks callbacks, GameRules rules) {
        _callbacks = callbacks;
        _rules = rules;
    }

    public void start() {
        if(!_isUp.get()) {
            _isUp.set(true);
            for (int i = 0; i < _nbExecutors; i++) {
                _executorService.submit(this::runGames);
            }
        }
    }

    public void stop() {
        _isUp.set(false);
    }

    private void runGames() {
        try {
            while (_isUp.get()) {
                forge.gamesimulationservice.model.Game gameSetup = _callbacks.getGame();
                if(gameSetup==null)
                    continue;

                Match mc = buildMatch(gameSetup);

                Map<Integer, String> drawSchedule = ApiAdapters.buildDrawSchedule(gameSetup.getDrawSchedules());
                Random random = new Random(gameSetup.getRandomSeed());
                final Game game = mc.createGame(drawSchedule, random);
                _timeoutPool.submit(() -> {
                    try {
                        Thread.sleep(1000 * 60 * 2);
                        if (!game.isGameOver()) {
                            game.cancel();
                        }
                    } catch (InterruptedException ignored) {
                    }
                });

                try {
                    mc.startGame(game);
                } catch (Throwable ex) {
                    if (!game.isGameOver()) {
                        game.setGameOver(GameEndReason.Draw);
                    }
                }

                if (_isUp.get()) {
                    _callbacks.afterGameRun(gameSetup, game);
                }
            }
        }
        catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private Match buildMatch(forge.gamesimulationservice.model.Game gameSetup) {
        Deck playerA = ApiAdapters.buildDeck(gameSetup.getDecks()[0], "playerA");
        Deck playerB = ApiAdapters.buildDeck(gameSetup.getDecks()[1], "playerB");

        DeckGroup deckGroup = new DeckGroup("Tournament");
        List<TournamentPlayer> players = new ArrayList<>();
        deckGroup.addAiDeck(playerA);
        players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(playerA.getName(), 0), 0));

        deckGroup.addAiDeck(playerB);
        players.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer(playerB.getName(), 0), 1));

        TournamentPairing pairing = new TournamentPairing(0, players);
        List<RegisteredPlayer> regPlayers = AbstractTournament.registerTournamentPlayers(pairing, deckGroup);
        Match mc = new Match(_rules, regPlayers, "TourneyMatch");
        return mc;
    }
}