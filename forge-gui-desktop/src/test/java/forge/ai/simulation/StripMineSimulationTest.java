package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

public class StripMineSimulationTest extends SimulationTest {
    @Test
    public void stillUsesStripMineOnUtilityLandWhenNotUnderCreaturePressure() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(20, null);

        addCard("Forest", opponent);
        addCard("Breeding Pool", opponent);
        addCard("Mana Confluence", opponent);
        addCard("Mutavault", opponent);
        addCard("Strip Mine", ai);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        AssertJUnit.assertEquals("Mutavault", picker.chooseSpellAbilityToPlay(null).getTargetCard().getName());
    }

    @Test
    public void dontUseStripMineWhenBehindOnBoardAndNotAheadOnMana() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(20, null);
        ai.setLife(20, null);

        addCard("Forest", opponent);
        addCard("Breeding Pool", opponent);
        addCard("Mana Confluence", opponent);
        addCard("Mutavault", opponent);
        addCard("Erhnam Djinn", opponent);

        addCard("Strip Mine", ai);
        addCard("Forest", ai);
        addCard("Island", ai);
        addCard("Mountain", ai);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));
    }
}
