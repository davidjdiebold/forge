package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class DamageDealAiTest extends SimulationTest {
    @Test
    public void triskelionStartsAnimateDeadLethalLineWithFaceDamage() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Swamp", 2, ai);
        Card triskelion = addCard("Triskelion", ai);
        triskelion.setCounters(CounterEnumType.P1P1, 3);
        addCardToZone("Animate Dead", ai, ZoneType.Hand);
        opponent.setLife(5, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility triskelionDamage = findSAWithPrefix(triskelion, "Remove a +1/+1 counter");
        AssertJUnit.assertNotNull(triskelionDamage);
        triskelionDamage.setActivatingPlayer(ai, true);
        AssertJUnit.assertEquals("Triskelion", triskelionDamage.getParam("AILogic"));
        AssertJUnit.assertEquals(3, triskelion.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(triskelionDamage.canTarget(opponent));

        AssertJUnit.assertTrue(new DamageDealAi().canPlayAI(ai, triskelionDamage));
        AssertJUnit.assertEquals(opponent, triskelionDamage.getTargets().getFirstTargetedPlayer());
    }

    @Test
    public void triskelionUsesLastCounterOnItselfForAnimateDeadLethalLine() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Swamp", 2, ai);
        Card triskelion = addCard("Triskelion", ai);
        triskelion.setCounters(CounterEnumType.P1P1, 1);
        addCardToZone("Animate Dead", ai, ZoneType.Hand);
        opponent.setLife(3, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility triskelionDamage = findSAWithPrefix(triskelion, "Remove a +1/+1 counter");
        AssertJUnit.assertNotNull(triskelionDamage);
        triskelionDamage.setActivatingPlayer(ai, true);
        AssertJUnit.assertEquals("Triskelion", triskelionDamage.getParam("AILogic"));
        AssertJUnit.assertEquals(1, triskelion.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertTrue(triskelionDamage.canTarget(triskelion));

        AssertJUnit.assertTrue(new DamageDealAi().canPlayAI(ai, triskelionDamage));
        AssertJUnit.assertEquals(triskelion, triskelionDamage.getTargetCard());
    }
}
