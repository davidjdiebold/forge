package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
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

    private static boolean considerTransmuteArtifact(final Player ai, final SpellAbility sa) {
        CardCollection ownArtifacts = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                CardPredicates.Presets.ARTIFACTS);
        ownArtifacts = CardLists.filter(ownArtifacts, CardPredicates.canBeSacrificedBy(sa, false));

        return chooseTransmuteArtifactSacrifice(ai, sa, ownArtifacts) != null;
    }

    public static Card chooseTransmuteArtifactSacrifice(final Player ai, final SpellAbility sa, final CardCollectionView choices) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        Card bestSacrifice = null;
        Card bestTarget = null;
        int bestNetScore = 0;
        for (Card sacrifice : choices) {
            final int leftover = getTransmuteLeftoverMana(ai, sacrifice);
            final Card target = chooseTransmuteArtifactTarget(ai, sa, ai.getCardsIn(ZoneType.Library), sacrifice.getCMC(), leftover);
            if (target == null) {
                continue;
            }

            final int netScore = scoreTransmuteArtifactTarget(ai, target, sacrifice.getCMC()) - scoreTransmuteSacrifice(sacrifice);
            if (netScore <= 0) {
                continue;
            }
            if (bestSacrifice == null || netScore > bestNetScore
                    || (netScore == bestNetScore && sacrifice.getCMC() < bestSacrifice.getCMC())) {
                bestSacrifice = sacrifice;
                bestTarget = target;
                bestNetScore = netScore;
            }
        }

        return bestTarget == null ? null : bestSacrifice;
    }

    public static Card chooseTransmuteArtifactTarget(final Player ai, final SpellAbility sa, final CardCollectionView fetchList) {
        int sackedCMC = 0;
        if (sa.getHostCard().hasSVar("SackedCMC")) {
            sackedCMC = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getHostCard().getSVar("SackedCMC"), sa);
        }
        return chooseTransmuteArtifactTarget(ai, sa, fetchList, sackedCMC, getTransmuteLeftoverMana(ai, null));
    }

    private static Card chooseTransmuteArtifactTarget(final Player ai, final SpellAbility sa, final CardCollectionView fetchList,
            final int sackedCMC, final int leftover) {
        if (fetchList == null || fetchList.isEmpty()) {
            return null;
        }

        Card best = null;
        int bestScore = 0;
        for (Card artifact : fetchList) {
            if (!artifact.isArtifact() || artifact.getCMC() > sackedCMC + leftover) {
                continue;
            }

            final int score = scoreTransmuteArtifactTarget(ai, artifact, sackedCMC);
            if (score > bestScore) {
                best = artifact;
                bestScore = score;
            }
        }
        return best;
    }

    private static int getTransmuteLeftoverMana(final Player ai, final Card sacrificed) {
        final CardCollection manaSources = ComputerUtilMana.getAvailableManaSources(ai, true);
        int available = manaSources.size() + ai.getManaPool().totalMana() - 2;
        if (sacrificed != null && manaSources.contains(sacrificed)) {
            available--;
        }
        return Math.max(0, available);
    }

    private static int scoreTransmuteArtifactTarget(final Player ai, final Card artifact, final int sackedCMC) {
        final String name = artifact.getName();
        if ("Chaos Orb".equals(name)) {
            return 900;
        }

        if (isBadTransmuteManaTarget(name)) {
            return 0;
        }

        if (isReusableManaArtifact(artifact)) {
            // Transmute should ramp only toward a real spell in hand, and not into
            // one-shot or awkward sources like Black Lotus or Mana Vault.
            if (!hasThreatToRampInto(ai)) {
                return 0;
            }
            final int currentBestMana = getBestReusableManaArtifactScore(ai);
            final int targetMana = scoreReusableManaArtifact(artifact);
            if (targetMana <= currentBestMana) {
                return 0;
            }
            return 500 + targetMana;
        }

        final int cmc = artifact.getCMC();
        if (cmc >= 5) {
            return 700 + ComputerUtilCard.evaluatePermanentList(new CardCollection(artifact)) * 20 + cmc * 30;
        }

        return 0;
    }

    private static int scoreTransmuteSacrifice(final Card artifact) {
        if (artifact.hasSVar("SacMe")) {
            return Math.max(0, 80 - Integer.parseInt(artifact.getSVar("SacMe")) * 10);
        }
        if (isReusableManaArtifact(artifact)) {
            return 500 + scoreReusableManaArtifact(artifact);
        }
        if (!artifact.getTriggers().isEmpty() || !artifact.getStaticAbilities().isEmpty()
                || !artifact.getReplacementEffects().isEmpty()) {
            return 250 + artifact.getCMC() * 20;
        }
        for (SpellAbility ab : artifact.getSpellAbilities()) {
            if (ab.isActivatedAbility() && ab.getApi() != null) {
                return 220 + artifact.getCMC() * 20;
            }
        }
        return 100 + artifact.getCMC() * 15;
    }

    private static boolean hasThreatToRampInto(final Player ai) {
        final int available = ComputerUtilMana.getAvailableManaSources(ai, true).size();
        for (Card handCard : ai.getCardsIn(ZoneType.Hand)) {
            if (handCard.isLand() || handCard.getCMC() < 4) {
                continue;
            }
            SpellAbility first = handCard.getFirstSpellAbility();
            if (first == null) {
                continue;
            }
            first.setActivatingPlayer(ai, true);
            if (!ComputerUtilMana.hasEnoughManaSourcesToCast(first, ai) && handCard.getCMC() <= available + 2) {
                return true;
            }
        }
        return false;
    }

    private static int getBestReusableManaArtifactScore(final Player ai) {
        int best = 0;
        for (Card permanent : ai.getCardsIn(ZoneType.Battlefield)) {
            if (permanent.isArtifact() && isReusableManaArtifact(permanent)) {
                best = Math.max(best, scoreReusableManaArtifact(permanent));
            }
        }
        return best;
    }

    private static boolean isReusableManaArtifact(final Card artifact) {
        return scoreReusableManaArtifact(artifact) > 0;
    }

    private static int scoreReusableManaArtifact(final Card artifact) {
        switch (artifact.getName()) {
            case "Sol Ring":
                return 300;
            case "Mox Sapphire":
            case "Mox Jet":
            case "Mox Ruby":
            case "Mox Pearl":
            case "Mox Emerald":
                return 170;
            default:
                return 0;
        }
    }

    private static boolean isBadTransmuteManaTarget(final String name) {
        return "Black Lotus".equals(name) || "Lotus Petal".equals(name) || "Mana Vault".equals(name);
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
