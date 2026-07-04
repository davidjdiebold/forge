package forge.ai.simulation;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class LowLifeRemovalSimulationTest extends SimulationTest {
    @Test
    public void prefersRemovalOverPotentialBlockerWhenLowOnLife() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        ai.setTeam(0);
        ai.setLife(5, null);

        addCard("Plains", ai);
        addCards("Forest", 2, ai);
        Card removal = addCardToZone("Swords to Plowshares", ai, ZoneType.Hand);
        Card threat = addCard("Serra Angel", opponent);
        addCardToZone("Centaur Courser", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);

        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(removal, sa.getHostCard());
        AssertJUnit.assertEquals(threat, sa.getTargetCard());
    }
}
