package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SylvanLibrarySimulationTest extends SimulationTest {
    @Test
    public void castsSylvanLibraryInMain1InsteadOfHoldingPendelhavenAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCard("Forest", ai);
        addCard("Pendelhaven", ai);
        addCard("Scryb Sprites", ai).setSickness(false);
        addCardToZone("Sylvan Library", ai, ZoneType.Hand);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, ai);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Sylvan Library", sa.getHostCard().getName());
    }

    @Test
    public void keepsForestAbovePlainsWhenHandNeedsDoubleGreen() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Forest", ai);
        addCardToZone("Trained Armodon", ai, ZoneType.Hand);

        Card plains = createCard("Plains", ai);
        Card forest = createCard("Forest", ai);
        CardCollection choices = new CardCollection();
        choices.add(plains);
        choices.add(forest);
        CardCollectionView reordered = ai.getController().orderMoveToZoneList(choices, ZoneType.Library, null);

        AssertJUnit.assertEquals("Forest", reordered.get(0).getName());
        AssertJUnit.assertEquals("Plains", reordered.get(1).getName());
    }
}
