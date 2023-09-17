package forge.gamesimulationservice.service;

import forge.game.GameEndReason;
import forge.game.player.PlayerStatistics;
import forge.game.player.RegisteredPlayer;
import forge.gamesimulationservice.runner.GameRunner;
import forge.gamesimulationservice.runner.GameRunnerCallbacks;
import forge.gamesimulationservice.runner.Rules;
import forge.gamesimulationservice.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class GameRunnerController implements GameRunnerCallbacks {

    private final ConcurrentHashMap<Integer, Deck> _decks;
    private final ConcurrentHashMap<String, Game> _gamesToRun;
    private final ConcurrentHashMap<String, GameStat> _stats;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<GameOutcome>> _outcomes;

    private final GameRunner _gameRunner;

    public GameRunnerController(Rules rules) {
        _gameRunner = new GameRunner(this, rules.getRules());
        _decks = new ConcurrentHashMap<>();
        _gamesToRun = new ConcurrentHashMap<>();
        _stats = new ConcurrentHashMap<>();
        _outcomes = new ConcurrentHashMap<>();
    }

    public void start() {
        _gameRunner.start();
    }

    public PostGamesResponse post(PostGamesRequest request) {
        _gamesToRun.clear();
        for (Game game : request.getGames()) {
            for (Deck deck : game.getDecks()) {
                _decks.put(deck.getId(), deck);
            }
            GameStat stat = new GameStat();
            stat.setGameId(game.getId());
            _stats.putIfAbsent(game.getId(), stat);
            _outcomes.putIfAbsent(game.getId(), new ConcurrentLinkedQueue<>());
            //TODO check gameid consistency ?
            _gamesToRun.put(game.getId(), game);
        }
        return new PostGamesResponse();
    }

    public GetGameStatsResponse post(GetGameStatsRequest request) {
        List<GameStat> stats = new ArrayList<>();
        Set<String> requestedGameIds = null;
        if(request.getGameIds() != null && request.getGameIds().length>0)
        {
            requestedGameIds = new HashSet<String>();
            for (String gameId : request.getGameIds()) {
                requestedGameIds.add(gameId);
            }
        }
        for (String gameId : _stats.keySet()) {
            if(requestedGameIds==null || requestedGameIds.contains(gameId)) {
                GameStat gameStat = _stats.get(gameId);
                stats.add(gameStat);
            }
        }
        GetGameStatsResponse getGameStatsResponse = new GetGameStatsResponse();
        getGameStatsResponse.setGameStats(stats.toArray(new GameStat[stats.size()]));
        return getGameStatsResponse;
    }

    public GetGameOutcomesResponse post(GetGameOutcomesRequest request) {
        List<GameOutcome> outcomes = new ArrayList<>();
        Set<String> requestedGameIds = null;
        if(request.getGameIds() != null && request.getGameIds().length>0)
            requestedGameIds = new HashSet<>(Arrays.asList(request.getGameIds()));

        for (String gameId : _outcomes.keySet()) {
            if(requestedGameIds==null || requestedGameIds.contains(gameId)) {
                ConcurrentLinkedQueue<GameOutcome> all = _outcomes.get(gameId);
                outcomes.addAll(all);
            }
        }
        GetGameOutcomesResponse response = new GetGameOutcomesResponse();
        response.setOutcomes(outcomes.toArray(new GameOutcome[outcomes.size()]));
        return response;
    }

    @Override
    public Game getGame() {
        if(_gamesToRun.size()==0)
            return null;
        int sumWeights = 0;
        for (Game game : _gamesToRun.values()) {
            sumWeights += game.getWeight();
        }

        Random random = new Random();
        int index = random.nextInt(sumWeights);

        int counter = 0;
        for (Game game : _gamesToRun.values()) {
            counter += game.getWeight();
            if(counter>index)
                return game;
        }
        return null;
    }

    @Override
    public void afterGameRun(Game game, forge.game.Game outcome) {
        boolean hasWon = false;
        for (Map.Entry<RegisteredPlayer, PlayerStatistics> entry : outcome.getOutcome()) {
            if (entry.getKey().getPlayer().getName().equals("playerA")) {
                hasWon = entry.getValue().getOutcome().hasWon()
                        && outcome.getOutcome().getWinCondition()!= GameEndReason.Draw;
            }
        }
        final boolean aHasWon = hasWon;
        String key = game.getId();
        _stats.computeIfPresent(key, (integer, gameStat) -> {
            gameStat.addWinLoss(aHasWon);
            return gameStat;
        });
        GameOutcome out = ApiAdapters.build(key, outcome);
        _outcomes.get(key).add(out);
    }

    public GetDecksResponse post(GetDecksRequest getDecksRequest) {
        int i = 0;
        Deck[] ret = new Deck[_decks.size()];
        for (Deck deck : _decks.values()) {
            ret[i] = deck;
            ++i;
        }
        GetDecksResponse response = new GetDecksResponse();
        response.setDecks(ret);
        return response;
    }
}
