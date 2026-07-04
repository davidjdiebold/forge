package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class AbyssSimulationTest extends SimulationTest {
    @Test
    public void doesNotCastDarkRitualIntoSengirVampireUnderAbyss() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        addCard("The Abyss", opponent);
        addCard("Swamp", ai);
        addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        addCardToZone("Sengir Vampire", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));
    }
}
