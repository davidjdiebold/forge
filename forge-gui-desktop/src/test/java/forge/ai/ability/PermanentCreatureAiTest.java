package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PermanentCreatureAiTest extends SimulationTest {
    @Test
    public void doesNotCastNonArtifactCreatureIntoAbyssWhenArtifactCreatureIsSafe() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        addCard("The Abyss", opponent);
        addCard("Juggernaut", ai);
        Card vulnerableCreature = addCardToZone("Elvish Visionary", ai, ZoneType.Hand);
        SpellAbility cast = vulnerableCreature.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new PermanentCreatureAi().checkApiLogic(ai, cast));
    }
}
