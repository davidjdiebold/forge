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

public class AttachAiTest extends SimulationTest {
    @Test
    public void animateDeadPrefersFlyingCreatureWhenOpponentHasMoat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Swamp", 2, ai);
        Card animateDead = addCardToZone("Animate Dead", ai, ZoneType.Hand);
        Card graveTitan = addCardToZone("Grave Titan", ai, ZoneType.Graveyard);
        Card serraAngel = addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        addCard("Moat", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = animateDead.getFirstSpellAbility();
        sa.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new AttachAi().canPlayAIWithSubs(ai, sa));
        AssertJUnit.assertEquals(serraAngel, sa.getTargetCard());
    }
}
