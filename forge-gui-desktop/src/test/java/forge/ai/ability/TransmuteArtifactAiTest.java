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

public class TransmuteArtifactAiTest extends SimulationTest {
    @Test
    public void choosesChaosOrbOverBadManaTargets() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility transmute = createTransmute(ai, 0);

        addMana(ai, 4);
        addCardToZone("Black Lotus", ai, ZoneType.Library);
        addCardToZone("Mana Vault", ai, ZoneType.Library);
        addCardToZone("Chaos Orb", ai, ZoneType.Library);

        Card target = SacrificeAi.chooseTransmuteArtifactTarget(ai, transmute, ai.getCardsIn(ZoneType.Library));

        AssertJUnit.assertNotNull(target);
        AssertJUnit.assertEquals("Chaos Orb", target.getName());
    }

    @Test
    public void rejectsManaVaultAsTransmuteTarget() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility transmute = createTransmute(ai, 0);

        addMana(ai, 4);
        addCardToZone("Mana Vault", ai, ZoneType.Library);

        Card target = SacrificeAi.chooseTransmuteArtifactTarget(ai, transmute, ai.getCardsIn(ZoneType.Library));

        AssertJUnit.assertNull(target);
    }

    @Test
    public void doesNotReplaceMoxWithBlackLotus() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility transmute = createTransmute(ai, 0);

        addMana(ai, 4);
        Card mox = addCard("Mox Sapphire", ai);
        addCardToZone("Black Lotus", ai, ZoneType.Library);

        Card sacrifice = SacrificeAi.chooseTransmuteArtifactSacrifice(ai, transmute, new CardCollection(mox));

        AssertJUnit.assertNull(sacrifice);
    }

    @Test
    public void fetchesBetterManaOnlyWithThreatInHand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        SpellAbility transmute = createTransmute(ai, 0);

        addMana(ai, 4);
        Card ornithopter = addCard("Ornithopter", ai);
        addCardToZone("Sol Ring", ai, ZoneType.Library);

        Card sacrificeWithoutThreat = SacrificeAi.chooseTransmuteArtifactSacrifice(ai, transmute, new CardCollection(ornithopter));
        AssertJUnit.assertNull(sacrificeWithoutThreat);

        addCardToZone("Shivan Dragon", ai, ZoneType.Hand);

        Card sacrificeWithThreat = SacrificeAi.chooseTransmuteArtifactSacrifice(ai, transmute, new CardCollection(ornithopter));
        AssertJUnit.assertNotNull(sacrificeWithThreat);
        AssertJUnit.assertEquals("Ornithopter", sacrificeWithThreat.getName());
    }

    private SpellAbility createTransmute(final Player ai, final int sackedCMC) {
        Card transmute = addCardToZone("Transmute Artifact", ai, ZoneType.Hand);
        transmute.setSVar("SackedCMC", "Number$" + sackedCMC);
        SpellAbility sa = transmute.getFirstSpellAbility();
        sa.setActivatingPlayer(ai, true);
        return sa;
    }

    private void addMana(final Player ai, final int count) {
        for (int i = 0; i < count; i++) {
            addCard("Island", ai);
        }
    }
}
