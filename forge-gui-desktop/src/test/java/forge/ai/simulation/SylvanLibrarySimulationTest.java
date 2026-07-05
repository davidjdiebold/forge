package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SylvanLibrarySimulationTest extends SimulationTest {
    @Test
    public void castsSylvanLibraryInMain1InsteadOfHoldingPendelhavenAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Forest", ai);
        addCard("Pendelhaven", ai);
        addCard("Scryb Sprites", ai).setSickness(false);
        addCardToZone("Sylvan Library", ai, ZoneType.Hand);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Sylvan Library", sa.getHostCard().getName());
    }
}
