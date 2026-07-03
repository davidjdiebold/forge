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

public class ChangeZoneAiTest extends SimulationTest {
    @Test
    public void regrowthDefersDefensiveCardWhenCreatureCanBeCastAndOpponentHasNoThreats() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Forest", 3, ai);
        addCardToZone("Centaur Courser", ai, ZoneType.Hand);
        Card regrowth = addCardToZone("Regrowth", ai, ZoneType.Hand);
        addCardToZone("Swords to Plowshares", ai, ZoneType.Graveyard);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility regrowthSa = regrowth.getFirstSpellAbility();
        regrowthSa.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new ChangeZoneAi().canPlayAIWithSubs(ai, regrowthSa));
    }
}
