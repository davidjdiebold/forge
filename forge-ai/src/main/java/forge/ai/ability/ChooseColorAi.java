package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class ChooseColorAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        
        if (!sa.hasParam("AILogic")) {
            return false;
        }
        final String logic = sa.getParam("AILogic");

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        if ("Nykthos, Shrine to Nyx".equals(source.getName())) {
            return SpecialCardAi.NykthosShrineToNyx.consider(ai, sa);
        }

        if ("Oona, Queen of the Fae".equals(source.getName())) {
        	if (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
        		return false;
        	}
            // Set PayX here to maximum value.
            int x = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(x));
            return true;
        }
        
        if ("Addle".equals(source.getName())) {
        	if (ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) || ai.getOpponent().getCardsIn(ZoneType.Hand).isEmpty()) {
        		return false;
        	}
            return true;
        }
        
        if (logic.equals("MostExcessOpponentControls")) {
        	for (byte color : MagicColor.WUBRG) {
        		CardCollectionView ailist = ai.getCardsIn(ZoneType.Battlefield);
        		CardCollectionView opplist = ai.getOpponent().getCardsIn(ZoneType.Battlefield);
        		
        		ailist = CardLists.filter(ailist, CardPredicates.isColor(color));
        		opplist = CardLists.filter(opplist, CardPredicates.isColor(color));

                int excess = ComputerUtilCard.evaluatePermanentList(opplist) - ComputerUtilCard.evaluatePermanentList(ailist);
                if (excess > 4) {
                	return true;
                }
            }
        	return false;
        }

        if (logic.equals("MostProminentInComputerDeck")) {
            if ("Astral Cornucopia".equals(source.getName())) {
                // activate in Main 2 hoping that the extra mana surplus will make a difference
                // (consider expanding)
                return ph.is(PhaseType.MAIN2, ai);
            }
        }

        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, sa);
    }

}
