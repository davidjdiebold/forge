package forge.ai.ability;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class CopyArtifactAiTest extends SimulationTest {
    @Test
    public void choosesThreatOverManaSource() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        SpellAbility copyArtifact = createCopyArtifact(ai);

        Card solRing = addCard("Sol Ring", opponent);
        Card juggernaut = addCard("Juggernaut", opponent);

        Card choice = new CloneAi().chooseSingleCard(ai, copyArtifact,
                cardCollection(solRing, juggernaut), true, null, null);

        AssertJUnit.assertNotNull(choice);
        AssertJUnit.assertEquals("Juggernaut", choice.getName());
    }

    @Test
    public void declinesManaSourceWhenHandDoesNotNeedMana() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        SpellAbility copyArtifact = createCopyArtifact(ai);

        addMana(ai, 4);
        Card solRing = addCard("Sol Ring", opponent);

        Card choice = new CloneAi().chooseSingleCard(ai, copyArtifact,
                new CardCollection(solRing), true, null, null);

        AssertJUnit.assertNull(choice);
        AssertJUnit.assertFalse(new PermanentAi().checkApiLogic(ai, copyArtifact));
    }

    @Test
    public void copiesManaSourceWhenHandNeedsMana() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        SpellAbility copyArtifact = createCopyArtifact(ai);

        addMana(ai, 4);
        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);
        Card solRing = addCard("Sol Ring", opponent);

        Card choice = new CloneAi().chooseSingleCard(ai, copyArtifact,
                new CardCollection(solRing), true, null, null);

        AssertJUnit.assertNotNull(choice);
        AssertJUnit.assertEquals("Sol Ring", choice.getName());
        AssertJUnit.assertTrue(new PermanentAi().checkApiLogic(ai, copyArtifact));
    }

    private SpellAbility createCopyArtifact(final Player ai) {
        Card copyArtifact = addCardToZone("Copy Artifact", ai, ZoneType.Hand);
        SpellAbility sa = copyArtifact.getFirstSpellAbility();
        sa.setActivatingPlayer(ai, true);
        return sa;
    }

    private void addMana(final Player ai, final int count) {
        for (int i = 0; i < count; i++) {
            addCard("Island", ai);
        }
    }

    private CardCollection cardCollection(final Card first, final Card second) {
        CardCollection cards = new CardCollection(first);
        cards.add(second);
        return cards;
    }
}
