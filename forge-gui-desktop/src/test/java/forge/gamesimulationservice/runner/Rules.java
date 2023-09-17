package forge.gamesimulationservice.runner;

import forge.deck.DeckFormat;
import forge.game.GameFormat;
import forge.game.GameRules;

public class Rules {
    private final GameRules _rules;
    private final GameFormat _gameFormat;
    private final DeckFormat _deckFormat;

    public Rules(GameRules rules, GameFormat gameFormat, DeckFormat deckFormat) {
        _rules = rules;
        _gameFormat = gameFormat;
        _deckFormat = deckFormat;
    }

    public GameRules getRules() {
        return _rules;
    }

    public GameFormat getGameFormat() {
        return _gameFormat;
    }

    public DeckFormat getDeckFormat() {
        return _deckFormat;
    }
}
