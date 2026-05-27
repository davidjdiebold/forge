package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SacrificeAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        return sacrificeTgtAI(ai, sa, false);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI should only activate this during Human's turn

        return sacrificeTgtAI(ai, sa, false);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        return sacrificeTgtAI(ai, sa, mandatory) || mandatory;
    }

    private boolean sacrificeTgtAI(final Player ai, final SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final boolean destroy = sa.hasParam("Destroy");
        final String aiLogic = sa.getParamOrDefault("AILogic", "");

        if ("TransmuteArtifact".equals(aiLogic)) {
            return considerTransmuteArtifact(ai, sa);
        }

        if (sa.usesTargeting()) {
            final PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            if (targetableOpps.isEmpty()) {
                // TODO also check if own SacMe makes this a reasonable (or even better) choice
                if (mandatory && sa.canTarget(ai)) {
                    sa.resetTargets();
                    sa.getTargets().add(ai);
                    return true;
                }
                return false;
            }
            final Player opp = targetableOpps.max(PlayerPredicates.compareByLife());
            sa.resetTargets();
            sa.getTargets().add(opp);
            if (mandatory) {
                return true;
            }
            final String valid = sa.getParam("SacValid");
            String num = sa.getParamOrDefault("Amount" , "1");
            final int amount = AbilityUtils.calculateAmount(source, num, sa);

            List<Card> list = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);

            for (Card c : list) {
                if (c.hasSVar("SacMe") && Integer.parseInt(c.getSVar("SacMe")) > 3) {
                    return false;
                }
            }
            if (!destroy) {
                list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(sa, true));
            } else {
                if (!CardLists.getKeyword(list, Keyword.INDESTRUCTIBLE).isEmpty()) {
                    // human can choose to destroy indestructibles
                    return false;
                }
            }

            if (list.isEmpty()) {
                return false;
            }

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                sa.setXManaCostPaid(Math.min(ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger()), amount));
            }

            final int half = (amount / 2) + (amount % 2); // Half of amount rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (!sa.isTrigger() && list.size() < half) {
                return false;
            }
        }

        final String defined = sa.getParamOrDefault("Defined", "You");
        final String targeted = sa.getParamOrDefault("ValidTgts", "");
        final String valid = sa.getParamOrDefault("SacValid", "Self");
        if (valid.equals("Self")) {
            // Self Sacrifice.
        } else if (defined.equals("Player") || targeted.equals("Player") || targeted.equals("Opponent")
                || ((defined.equals("Player.Opponent") || defined.equals("Opponent")) && !sa.isTrigger())) {
            // is either "Defined$ Player.Opponent" or "Defined$ Opponent" obsolete?

            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is worse than his "worst" valid
            final String num = sa.getParamOrDefault("Amount", "1");
            int amount = AbilityUtils.calculateAmount(source, num, sa);

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                amount = Math.min(ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger()), amount);
            }

            List<Card> humanList = CardLists.getValidCards(ai.getStrongestOpponent().getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);

            // Since all of the cards have AI:RemoveDeck:All, I enabled 1 for 1
            // (or X for X) trades for special decks
            return humanList.size() >= amount;
        } else if (defined.equals("You")) {
            List<Card> computerList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);
            for (Card c : computerList) {
                if ("Lethal".equals(aiLogic)) {
                    boolean isLethal = false;
                    for (Player opp : ai.getOpponents()) {
                        if (opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife() && c.getNetPower() >= opp.getLife()) {
                            isLethal = true;
                            break;
                        }
                    }
                    for (Card creature : ai.getOpponents().getCreaturesInPlay()) {
                        if (creature.canBeDestroyed() && c.getNetPower() >= creature.getNetToughness()) {
                            isLethal = true;
                            break;
                        }
                    }
                    return c.hasSVar("SacMe") || isLethal;
                }
                if (c.hasSVar("SacMe") || ComputerUtilCard.evaluateCreature(c) <= 135) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    /**
     * Transmute Artifact: only cast when there's a worthwhile upgrade in the
     * library AND the AI can afford to pay the X difference if needed.
     * Otherwise the searched-for artifact ends up in the graveyard.
     */
    private static boolean considerTransmuteArtifact(final Player ai, final SpellAbility sa) {
        // Find artifacts we can sacrifice (excluding the spell itself which is on the stack).
        CardCollection ownArtifacts = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                CardPredicates.Presets.ARTIFACTS);
        ownArtifacts = CardLists.filter(ownArtifacts, CardPredicates.canBeSacrificedBy(sa, false));
        if (ownArtifacts.isEmpty()) {
            return false;
        }

        // Determine the cheapest artifact we would sacrifice (lowest CMC = least loss).
        Card sacCandidate = null;
        int minCMC = Integer.MAX_VALUE;
        for (Card c : ownArtifacts) {
            if (c.getCMC() < minCMC) {
                minCMC = c.getCMC();
                sacCandidate = c;
            }
        }
        if (sacCandidate == null) {
            return false;
        }
        int sacCMC = sacCandidate.getCMC();

        // Find candidate artifacts in our library.
        CardCollection libArtifacts = CardLists.filter(ai.getCardsIn(ZoneType.Library),
                CardPredicates.Presets.ARTIFACTS);
        if (libArtifacts.isEmpty()) {
            return false;
        }

        // Available mana after paying the UU cost of Transmute Artifact itself.
        int totalManaSources = forge.ai.ComputerUtilMana.getAvailableManaSources(ai, true).size();
        // Subtract this spell's own cost (UU = 2). If we're being evaluated, the
        // spell isn't yet committed, so this approximates leftover mana.
        int leftover = Math.max(0, totalManaSources - 2);

        // Look for a useful upgrade we can actually fetch:
        // libCMC <= sacCMC + leftover (so X can be paid if needed)
        // AND the new artifact must be a real upgrade (strictly higher CMC).
        for (Card lib : libArtifacts) {
            int libCMC = lib.getCMC();
            if (libCMC > sacCMC + leftover) {
                continue;
            }
            if (libCMC > sacCMC) {
                return true;
            }
        }
        return false;
    }

    public static boolean doSacOneEachLogic(Player ai, SpellAbility sa) {
        Game game = ai.getGame();

        sa.resetTargets();
        for (Player p : game.getPlayers()) {
            CardCollection targetable = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.isTargetableBy(sa));
            if (!targetable.isEmpty()) {
                CardCollection priorityTgts = new CardCollection();
                if (p.isOpponentOf(ai)) {
                    priorityTgts.addAll(CardLists.filter(targetable, CardPredicates.canBeSacrificedBy(sa, true)));
                    if (!priorityTgts.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(priorityTgts));
                    } else {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(targetable));
                    }
                } else {
                    for (Card c : targetable) {
                        if (c.canBeSacrificedBy(sa, true) && (c.hasSVar("SacMe") || (c.isCreature() && ComputerUtilCard.evaluateCreature(c) <= 135)) && !c.equals(sa.getHostCard())) {
                            priorityTgts.add(c);
                        }
                    }
                    if (!priorityTgts.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getWorstPermanentAI(priorityTgts, false, false, false, false));
                    } else {
                        targetable.remove(sa.getHostCard());
                        if (!targetable.isEmpty()) {
                            sa.getTargets().add(ComputerUtilCard.getWorstPermanentAI(targetable, true, true, true, false));
                        } else {
                            sa.getTargets().add(sa.getHostCard()); // sac self only as a last resort
                        }
                    }
                }
            }
        }
        return true;
    }

}
