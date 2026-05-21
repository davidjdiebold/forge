package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class BalanceAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");
        double diff = 0;
        Player opp = aiPlayer.getWeakestOpponent();
        final CardCollectionView compPerms = aiPlayer.getCardsIn(ZoneType.Battlefield);
        for (Player min : aiPlayer.getOpponents()) {
            if (min.getCardsIn(ZoneType.Battlefield).size() < opp.getCardsIn(ZoneType.Battlefield).size()) {
                opp = min;
            }
        }
        final CardCollectionView humPerms = opp.getCardsIn(ZoneType.Battlefield);

        final int aiCreatureCount = CardLists.filter(compPerms, CardPredicates.Presets.CREATURES).size();
        final int oppCreatureCount = CardLists.filter(humPerms, CardPredicates.Presets.CREATURES).size();

        if ("BalanceCreaturesAndLands".equals(logic)) {
            // TODO Copied over from hardcoded Balance. We should be checking value of the lands/creatures for each opponent, not just counting
            diff += CardLists.filter(humPerms, CardPredicates.Presets.LANDS).size() -
                    CardLists.filter(compPerms, CardPredicates.Presets.LANDS).size();
            diff += 1.5 * (oppCreatureCount - aiCreatureCount);
        }
        else if ("BalancePermanents".equals(logic)) {
            // Don't cast if you have to sacrifice permanents
            diff += humPerms.size() - compPerms.size();
        }

        if (diff < 0) {
            // Don't sacrifice permanents even if opponent has a ton of cards in hand
            return false;
        }

        final CardCollectionView humHand = opp.getCardsIn(ZoneType.Hand);
        final CardCollectionView compHand = aiPlayer.getCardsIn(ZoneType.Hand);
        diff += 0.5 * (humHand.size() - compHand.size());

        // Strong opportunity: opponent has at least two more creatures than AI and AI has no
        // (or barely any) creatures itself — Balance essentially wipes their board for free.
        // Casting this is almost always correct, so bypass the probabilistic gate.
        if (oppCreatureCount - aiCreatureCount >= 2 && aiCreatureCount <= 1 && diff > 0) {
            return true;
        }

        // Life pressure override: if the opponent's creatures can put the AI within a few
        // turns of lethal, Balance is worth casting to defuse the clock even if the random
        // gate would normally skip it. We require diff > 0 so we still come out ahead on
        // the exchange.
        if (diff > 0 && opponentClockThreatensAi(aiPlayer, opp, oppCreatureCount)) {
            return true;
        }

        // Larger differential == more chance to actually cast this spell
        return diff > 2 && MyRandom.getRandom().nextInt(100) < diff*10;
    }

    /**
     * Returns true when the opponent's creatures can race the AI to a quick
     * death (e.g. lethal in 3 attack steps or fewer) or when the AI is already
     * in life danger. Used to override the probabilistic gate in canPlayAI so
     * that a clearly favourable Balance still gets cast under pressure.
     */
    private static boolean opponentClockThreatensAi(final Player ai, final Player opp, final int oppCreatureCount) {
        if (oppCreatureCount <= 0) {
            return false;
        }
        if (ComputerUtil.aiLifeInDanger(ai, false, 0)) {
            return true;
        }
        final int oppPower = CardLists.getTotalPower(
                CardLists.filter(opp.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES),
                true, false);
        if (oppPower <= 0) {
            return false;
        }
        // Estimate how many attack steps until the AI dies, ignoring any blocking the
        // AI could do (the whole point of Balance is to remove those threats).
        final int turnsToDeath = (int) Math.ceil((double) ai.getLife() / oppPower);
        return turnsToDeath <= 3;
    }
}
