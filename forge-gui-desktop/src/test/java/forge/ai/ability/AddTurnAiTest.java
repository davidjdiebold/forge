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

public class AddTurnAiTest extends SimulationTest {
    @Test
    public void timeWalkWaitsForAncestralRecallWithLowManaAndNoThreats() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Island", 2, ai);
        Card timeWalk = addCardToZone("Time Walk", ai, ZoneType.Hand);
        addCardToZone("Ancestral Recall", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility extraTurn = timeWalk.getFirstSpellAbility();
        extraTurn.setActivatingPlayer(ai, true);

        AssertJUnit.assertFalse(new AddTurnAi().canPlayAI(ai, extraTurn));
    }

    @Test
    public void timeWalkStillPlaysWithThreatInPlay() {
        Game game = initAndCreateGame();
        Player opponent = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        ai.setTeam(0);
        opponent.setTeam(1);

        addCards("Island", 2, ai);
        addCard("Runeclaw Bear", ai);
        Card timeWalk = addCardToZone("Time Walk", ai, ZoneType.Hand);
        addCardToZone("Ancestral Recall", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility extraTurn = timeWalk.getFirstSpellAbility();
        extraTurn.setActivatingPlayer(ai, true);

        AssertJUnit.assertTrue(new AddTurnAi().canPlayAI(ai, extraTurn));
    }
}
