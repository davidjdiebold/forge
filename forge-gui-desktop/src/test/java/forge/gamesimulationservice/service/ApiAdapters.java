package forge.gamesimulationservice.service;

import forge.LobbyPlayer;
import forge.StaticData;
import forge.deck.Deck;
import forge.game.GameEndReason;
import forge.game.player.PlayerStatistics;
import forge.game.player.RegisteredPlayer;
import forge.gamesimulationservice.model.CardCount;
import forge.gamesimulationservice.model.GameOutcome;
import forge.item.PaperCard;

import java.util.List;
import java.util.Map;

public class ApiAdapters {
    public static Deck buildDeck(forge.gamesimulationservice.model.Deck deck, String name) {
        Deck ret = new Deck(name);
        for (CardCount cardCount : deck.getMain()) {
            String card = cardCount.getCard();
            PaperCard c = StaticData.instance().getCommonCards().getCard(card);
            if (c == null) {
                throw new IllegalArgumentException("Card not found : " + card);
            }
            ret.getMain().add(c, cardCount.getCount());
        }
        return ret;
    }

    public static GameOutcome build(String gameId, forge.game.Game game) {
        GameOutcome ret = new GameOutcome();
        ret.setGameId(gameId);
        ret.setWin(false);
        //outcome.getTurn
        for (Map.Entry<RegisteredPlayer, PlayerStatistics> entry : game.getOutcome()) {
            LobbyPlayer player = entry.getKey().getPlayer();
            if (player.getName().equals("playerA")) {
                ret.setWin(entry.getValue().getOutcome().hasWon()
                        && game.getOutcome().getWinCondition() != GameEndReason.Draw);
            }
        }
        List<String> drawn = game.getRegisteredPlayers().get(0).drawn;
        ret.setCardsDrawn(new String[drawn.size()]);
        int i = 0;
        for (String card : drawn) {
            ret.getCardsDrawn()[i] = card;
            i++;
        }
        ret.setLastTurnNumber(game.getOutcome().getLastTurnNumber());
        return ret;
    }
}
