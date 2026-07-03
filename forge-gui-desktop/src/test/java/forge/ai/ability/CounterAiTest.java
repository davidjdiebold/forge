package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class CounterAiTest extends SimulationTest {
    @Test
    public void countersAncestralRecallByDefault() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Island", 2, ai);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);
        Card ancestralRecall = addCardToZone("Ancestral Recall", opponent, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, opponent);
        game.getAction().checkStateEffects(true);

        SpellAbility recallSa = ancestralRecall.getFirstSpellAbility();
        recallSa.setActivatingPlayer(opponent, true);
        recallSa.getTargets().add(ai);
        recallSa.setHostCard(game.getAction().moveToStack(ancestralRecall, recallSa));
        game.getStack().freezeStack();
        game.getStack().addAndUnfreeze(recallSa);

        SpellAbility counterSa = counterspell.getFirstSpellAbility();
        counterSa.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new CounterAi().canPlayAI(ai, counterSa));
    }
}
