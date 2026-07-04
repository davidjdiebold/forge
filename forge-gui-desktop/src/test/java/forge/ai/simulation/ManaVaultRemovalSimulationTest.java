package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ManaVaultRemovalSimulationTest extends SimulationTest {
    @Test
    public void disenchantDoesNotTargetLoneManaVault() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Plains", 2, ai);
        addCardToZone("Disenchant", ai, ZoneType.Hand);
        addCard("Mana Vault", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));
    }

    @Test
    public void disenchantPrefersRealTargetOverManaVault() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Plains", 2, ai);
        addCardToZone("Disenchant", ai, ZoneType.Hand);
        addCard("Mana Vault", opponent);
        addCard("Winter Orb", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Disenchant", sa.getHostCard().getName());
        AssertJUnit.assertEquals("Winter Orb", sa.getTargetCard().getName());
    }
}
