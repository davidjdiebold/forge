package forge.ai.ability;

import forge.ai.ComputerUtilMana;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class BlackLotusAiTest extends SimulationTest {
    @Test
    public void blackLotusDoesNotCastJayemdaeTomeOnTurnOne() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);

        addCards("Island", 1, ai);
        addCardToZone("Black Lotus", ai, ZoneType.Hand);
        Card tome = addCardToZone("Jayemdae Tome", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = tome.getFirstSpellAbility();
        sa.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }
}
