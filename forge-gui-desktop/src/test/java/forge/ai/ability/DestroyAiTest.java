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

public class DestroyAiTest extends SimulationTest {
    @Test
    public void destroySpellIgnoresCreaturesNeutralizedByMoat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Swamp", 2, ai);
        Card doomBlade = addCardToZone("Doom Blade", ai, ZoneType.Hand);
        addCard("Moat", opponent);
        addCard("Runeclaw Bear", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = doomBlade.getFirstSpellAbility();
        sa.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new DestroyAi().canPlayAIWithSubs(ai, sa));
    }
}
