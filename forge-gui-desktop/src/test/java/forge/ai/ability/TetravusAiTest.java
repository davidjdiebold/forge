package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class TetravusAiTest extends SimulationTest {
    @Test
    public void splitsCountersWhenNoMajorBlockerDuty() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card tetravus = addCard("Tetravus", ai);
        tetravus.setCounters(CounterEnumType.P1P1, 3);

        AssertJUnit.assertEquals(3, TokenAi.chooseTetravusSplitAmount(ai, tetravus));
    }

    @Test
    public void keepsCountersToTradeWithLargeFlyer() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        Card tetravus = addCard("Tetravus", ai);
        tetravus.setCounters(CounterEnumType.P1P1, 3);
        addCard("Serra Angel", opponent);

        AssertJUnit.assertEquals(0, TokenAi.chooseTetravusSplitAmount(ai, tetravus));
    }
}
