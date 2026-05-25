package forge.ai.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class DiscardAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Cost abCost = sa.getPayCosts();
        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        // temporarily disabled until better AI
        if (!willPayCosts(ai, sa, abCost, source)) {
            return false;
        }

        if ("Chandra, Flamecaller".equals(sourceName)) {
            final int hand = ai.getCardsIn(ZoneType.Hand).size();
            return MyRandom.getRandom().nextFloat() < (1.0 / (1 + hand));
        }

        // Hand-refresh spells like Wheel of Fortune and Windfall (each player
        // discards their hand and draws 7) should be deferred when the AI has
        // an unplayed draw-punisher in hand (Underworld Dreams etc.) — get
        // the punisher down first so the 7-card refresh hits the opponent
        // for free damage on every drawn card.
        if (("Wheel of Fortune".equals(sourceName)
                || "Windfall".equals(sourceName)
                || "Reforge the Soul".equals(sourceName))
                && SpecialCardAi.Timetwister.hasUnplayedDrawPunisherInHand(ai, source)) {
            return false;
        }

        if (aiLogic.equals("VolrathsShapeshifter")) {
            return SpecialCardAi.VolrathsShapeshifter.consider(ai, sa);
        }

        if (aiLogic.equals("Recall")) {
            return considerRecall(ai, sa);
        }

        final boolean humanHasHand = ai.getWeakestOpponent().getCardsIn(ZoneType.Hand).size() > 0;

        if (sa.usesTargeting()) {
            if (!discardTargetAI(ai, sa)) {
                return false;
            }
        } else {
            // TODO: Add appropriate restrictions
            final List<Player> players = AbilityUtils.getDefinedPlayers(source, sa.getParam("Defined"), sa);

            if (players.size() == 1) {
                if (players.get(0) == ai) {
                    // the ai should only be using something like this if he has
                    // few cards in hand,
                    // cards like this better have a good drawback to be in the AIs deck
                } else {
                    // defined to the human, so that's fine as long the human has cards
                    if (!humanHasHand) {
                        return false;
                    }
                }
            } else {
                // Both players discard, any restrictions?
            }
        }

        if (sa.hasParam("NumCards")) {
           if (sa.getParam("NumCards").equals("X") && sa.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int cardsToDiscard = Math.min(ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger()), ai.getWeakestOpponent()
                        .getCardsIn(ZoneType.Hand).size());
                if (cardsToDiscard < 1) {
                    return false;
                }
                sa.setXManaCostPaid(cardsToDiscard);
            } else {
                if (AbilityUtils.calculateAmount(source, sa.getParam("NumCards"), sa) < 1) {
                    return false;
                }
            }
        }

        // TODO: Improve support for Discard AI for cards with AnyNumber set to true.
        if (sa.hasParam("AnyNumber")) {
            if ("DiscardUncastableAndExcess".equals(aiLogic)) {
                final CardCollectionView inHand = ai.getCardsIn(ZoneType.Hand);
                final int numLandsOTB = CardLists.count(ai.getCardsIn(ZoneType.Hand), CardPredicates.Presets.LANDS);
                int numDiscard = 0;
                int numOppInHand = 0;
                for (Player p : ai.getGame().getPlayers()) {
                    if (p.getCardsIn(ZoneType.Hand).size() > numOppInHand) {
                        numOppInHand = p.getCardsIn(ZoneType.Hand).size();
                    }
                }
                for (Card c : inHand) {
                    if (c.equals(source)) { continue; }
                    if (c.hasSVar("DoNotDiscardIfAble") || c.hasSVar("IsReanimatorCard")) { continue; }
                    if (c.isCreature() && !ComputerUtilMana.hasEnoughManaSourcesToCast(c.getSpellPermanent(), ai)) {
                        numDiscard++;
                    }
                    if ((c.isLand() && numLandsOTB >= 5) || (c.getFirstSpellAbility() != null && !ComputerUtilMana.hasEnoughManaSourcesToCast(c.getFirstSpellAbility(), ai))) {
                        if (numDiscard + 1 <= numOppInHand) {
                            numDiscard++;
                        }
                    }
                }
                if (numDiscard == 0) {
                    return false;
                }
            }
        }

        // Don't use discard abilities before main 2 if possible
        if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)
                && !sa.hasParam("ActivationPhases") && !aiLogic.startsWith("AnyPhase")) {
            return false;
        }

        if (aiLogic.equals("AnyPhaseIfFavored")) {
            if (ai.getGame().getCombat() != null) {
                if (ai.getCardsIn(ZoneType.Hand).size() < ai.getGame().getCombat().getDefenderPlayerByAttacker(source).getCardsIn(ZoneType.Hand).size()) {
                    return false;
                }
            }
        }

        // Don't tap creatures that may be able to block
        if (ComputerUtil.waitForBlocking(sa)) {
            return false;
        }

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(0.9, sa.getActivationsThisTurn());

        // some other variables here, like handsize vs. maxHandSize

        return randomReturn;
    }

    private boolean discardTargetAI(final Player ai, final SpellAbility sa) {
        final PlayerCollection opps = ai.getOpponents();
        Collections.shuffle(opps);
        for (Player opp : opps) {
            if (opp.getCardsIn(ZoneType.Hand).isEmpty() && !ComputerUtil.activateForCost(sa, ai)) {
                continue;
            } else if (!opp.canDiscardBy(sa, true)) { // e.g. Tamiyo, Collector of Tales
                continue;
            }
            // TODO when DiscardValid is used and opponent plays with hand revealed, check if he has matching cards
            if (sa.usesTargeting()) {
                if (sa.canTarget(opp)) {
                    sa.resetTargets();
                    sa.getTargets().add(opp);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
            if (!discardTargetAI(ai, sa)) {
                if (mandatory && opp != null) {
                    sa.getTargets().add(opp);
                } else if (mandatory && sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                } else {
                    return false;
                }
            }
        } else {
            if (sa.hasParam("AILogic")) {
            	if ("AtLeast2".equals(sa.getParam("AILogic"))) {
            		final List<Player> players = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            		if (players.isEmpty() || players.get(0).getCardsIn(ZoneType.Hand).size() < 2) {
            			return false;
            		}
            	}
            }
            if ("X".equals(sa.getParam("RevealNumber")) && sa.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int cardsToDiscard = Math.min(ComputerUtilCost.getMaxXValue(sa, ai, true), ai.getWeakestOpponent()
                        .getCardsIn(ZoneType.Hand).size());
                sa.setXManaCostPaid(cardsToDiscard);
            }
        }

        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // Drawback AI improvements
        // if parent draws cards, make sure cards in hand + cards drawn > 0
        if (sa.usesTargeting()) {
            return discardTargetAI(ai, sa);
        }
        // TODO: check for some extra things
        return true;
    }

    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if (mode == PlayerActionConfirmMode.Random) {
            // TODO For now AI will always discard Random used currently with: Balduvian Horde and similar cards
            return true;
        }
        return super.confirmAction(player, sa, mode, message, params);
    }

    /**
     * Recall: pay XXU, discard X cards, then return X cards from graveyard to
     * hand. Worth casting when we have junk in hand (extra lands, uncastable
     * cards) and high-value cards in the graveyard worth recovering
     * (Ancestral Recall, big creatures, key answers).
     */
    private static boolean considerRecall(final Player ai, final SpellAbility sa) {
        // Need a non-trivial graveyard to recover from.
        final CardCollectionView graveyard = ai.getCardsIn(ZoneType.Graveyard);
        if (graveyard.size() < 2) {
            return false;
        }

        // Determine max X we can afford. Each X costs 1 generic (XXU), and
        // it discards X cards then returns X. We want X >= 2 for it to be
        // strictly net-positive after paying U.
        final int maxX = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
        if (maxX < 2) {
            return false;
        }

        // Count junk cards in hand we'd willingly discard (excluding Recall
        // itself which is on the stack already, but still treat hand as the
        // post-cast hand).
        int junk = 0;
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.equals(sa.getHostCard())) {
                continue;
            }
            if (c.hasSVar("DoNotDiscardIfAble") || c.hasSVar("IsReanimatorCard")) {
                continue;
            }
            if (ComputerUtil.isWorseThanDraw(ai, c)) {
                junk++;
            }
        }
        if (junk < 2) {
            return false;
        }

        // Count valuable cards in graveyard worth recalling.
        int valuableInGrave = 0;
        for (Card c : graveyard) {
            if (c.isLand()) {
                continue;
            }
            if (c.isCreature() && ComputerUtilCard.evaluateCreature(c) >= 150) {
                valuableInGrave++;
                continue;
            }
            // Non-creature spells of any meaningful CMC, or anything flagged
            // as a key card.
            if (!c.isCreature() && c.getCMC() >= 1) {
                valuableInGrave++;
            } else if ("Ancestral Recall".equals(c.getName())
                    || c.hasSVar("DoNotDiscardIfAble")) {
                valuableInGrave++;
            }
        }
        if (valuableInGrave < 2) {
            return false;
        }

        // Choose X = min(maxX, junk, valuableInGrave) capped at a reasonable
        // budget; recalling more than 4 cards at once tends to over-commit.
        int chosenX = Math.min(Math.min(maxX, junk), valuableInGrave);
        chosenX = Math.min(chosenX, 4);
        if (chosenX < 2) {
            return false;
        }
        sa.setXManaCostPaid(chosenX);
        return true;
    }
}
