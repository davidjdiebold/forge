package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BraingeyserSimulationTest extends SimulationTest {
    @Test
    public void castsDarkRitualBeforeBraingeyserToIncreaseX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 2, ai);
        addCard("Swamp", ai);
        Card darkRitual = addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        addCardToZone("Braingeyser", ai, ZoneType.Hand);
        addLibraryCards(ai, opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);

        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(darkRitual, sa.getHostCard());

        Plan plan = picker.getPlan();
        AssertJUnit.assertTrue(plan.getDecisions().size() >= 2);
        AssertJUnit.assertTrue(plan.getDecisions().get(1).toString(true).contains("Braingeyser (5)"));
        AssertJUnit.assertTrue(plan.getDecisions().get(1).toString(true).contains("[p1]"));
    }

    @Test
    public void braingeyserPlanStopsAtEightCardsWithBlackLotus() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 4, ai);
        Card blackLotus = addCardToZone("Black Lotus", ai, ZoneType.Hand);
        addCardToZone("Braingeyser", ai, ZoneType.Hand);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        addLibraryCards(ai, opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);

        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(blackLotus, sa.getHostCard());

        Plan plan = picker.getPlan();
        AssertJUnit.assertTrue(plan.getDecisions().size() >= 2);
        AssertJUnit.assertTrue(plan.getDecisions().get(1).toString(true).contains("Braingeyser (6)"));
        AssertJUnit.assertTrue(plan.getDecisions().get(1).toString(true).contains("[p1]"));
    }
    private void addLibraryCards(final Player ai, final Player opponent) {
        for (int i = 0; i < 20; i++) {
            addCardToZone("Forest", ai, ZoneType.Library);
            addCardToZone("Forest", opponent, ZoneType.Library);
        }
    }
}
