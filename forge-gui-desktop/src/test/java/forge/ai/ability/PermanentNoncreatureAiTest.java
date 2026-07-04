package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PermanentNoncreatureAiTest extends SimulationTest {
    @Test
    public void doesNotCastAbyssWhenOnlyAiHasNonArtifactCreature() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        addCard("Grizzly Bears", ai);
        addCard("Juggernaut", opponent);

        Card theAbyss = addCardToZone("The Abyss", ai, ZoneType.Hand);
        SpellAbility cast = theAbyss.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new PermanentNoncreatureAi().checkApiLogic(ai, cast));
    }

    @Test
    public void castsAbyssWhenOpponentAlsoHasNonArtifactCreature() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        addCard("Grizzly Bears", ai);
        addCard("Runeclaw Bear", opponent);

        Card theAbyss = addCardToZone("The Abyss", ai, ZoneType.Hand);
        SpellAbility cast = theAbyss.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new PermanentNoncreatureAi().checkApiLogic(ai, cast));
    }

    @Test
    public void doesNotCastManaVaultToAccelerateTheAbyssWithoutPressure() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 2, ai);
        Card manaVault = addCardToZone("Mana Vault", ai, ZoneType.Hand);
        addCardToZone("The Abyss", ai, ZoneType.Hand);

        SpellAbility cast = manaVault.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new PermanentNoncreatureAi().checkApiLogic(ai, cast));
    }

    @Test
    public void castsManaVaultToAccelerateThreat() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 3, ai);
        Card manaVault = addCardToZone("Mana Vault", ai, ZoneType.Hand);
        addCardToZone("Sengir Vampire", ai, ZoneType.Hand);

        SpellAbility cast = manaVault.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new PermanentNoncreatureAi().checkApiLogic(ai, cast));
    }

    @Test
    public void castsManaVaultToAccelerateTheAbyssUnderPressure() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 2, ai);
        Card manaVault = addCardToZone("Mana Vault", ai, ZoneType.Hand);
        addCardToZone("The Abyss", ai, ZoneType.Hand);
        addCard("Serra Angel", opponent);

        SpellAbility cast = manaVault.getFirstSpellAbility();
        cast.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new PermanentNoncreatureAi().checkApiLogic(ai, cast));
    }
}
