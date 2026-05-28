package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.ai.*;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPutCounter;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.MyRandom;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DamageDealAi extends DamageAiBase {
    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        final SpellAbility root = sa.getRootAbility();
        final String damage = sa.getParam("NumDmg");
        Card source = sa.getHostCard();
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);
        final String logic = sa.getParam("AILogic");

        if ("MadSarkhanDigDmg".equals(logic)) {
            return SpecialCardAi.SarkhanTheMad.considerDig(ai, sa);
        }

        if (damage.equals("X")) {
            if (sa.getSVar(damage).equals("Count$ChosenNumber")) {
                int energy = ai.getCounters(CounterEnumType.ENERGY);
                for (SpellAbility s : source.getSpellAbilities()) {
                    if ("PayEnergy".equals(s.getParam("AILogic"))) {
                        energy += AbilityUtils.calculateAmount(source, s.getParam("CounterNum"), sa);
                        break;
                    }
                }
                for (; energy > 0; energy--) {
                    if (damageTargetAI(ai, sa, energy, false)) {
                        dmg = ComputerUtilCombat.getEnoughDamageToKill(sa.getTargetCard(), energy, source, false, false);
                        if (dmg > energy || dmg < 1) {
                            continue; // in case the calculation gets messed up somewhere
                        }
                        root.setSVar("EnergyToPay", "Number$" + dmg);
                        return true;
                    }
                }
                return false;
            }
            if (sa.getSVar(damage).equals("Count$xPaid")) {
                // Life Drain
                if ("XLifeDrain".equals(logic)) {
                    return doXLifeDrainLogic(ai, sa);
                }

                // Set PayX here to maximum value.
                dmg = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());
                sa.setXManaCostPaid(dmg);
            } else if (sa.getSVar(damage).equals("Count$CardsInYourHand") && source.isInZone(ZoneType.Hand)) {
                dmg--; // the card will be spent casting the spell, so actual damage is 1 less
            }
        }
        return damageTargetAI(ai, sa, dmg, true);
    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);

        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);

        if (damage.equals("X") || sourceName.equals("Crater's Claws")) {
            if (sa.getSVar(damage).equals("Count$xPaid") || sourceName.equals("Crater's Claws")) {
                dmg = ComputerUtilCost.getMaxXValue(sa, ai, sa.isTrigger());

                // Try not to waste spells like Blaze or Fireball on early targets, try to do more damage with them if possible
                if (ai.getController().isAI()) {
                    AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
                    int holdChance = aic.getIntProperty(AiProps.HOLD_X_DAMAGE_SPELLS_FOR_MORE_DAMAGE_CHANCE);
                    if (MyRandom.percentTrue(holdChance, ai.getGame().getRandom())) {
                        int threshold = aic.getIntProperty(AiProps.HOLD_X_DAMAGE_SPELLS_THRESHOLD);
                        boolean inDanger = ComputerUtil.aiLifeInDanger(ai, false, 0);
                        boolean isLethal = sa.getTargetRestrictions().canTgtPlayer() && dmg >= ai.getWeakestOpponent().getLife() && !ai.getWeakestOpponent().cantLoseForZeroOrLessLife();
                        if (dmg < threshold && ai.getGame().getPhaseHandler().getTurn() / 2 < threshold && !inDanger && !isLethal
                                && !canSplitDamageToKillMultipleOppCreatures(ai, sa, dmg)) {
                            return false;
                        }
                    }
                }

                // Set PayX here to maximum value. It will be adjusted later depending on the target.
                sa.setXManaCostPaid(dmg);
            } else if (sa.getSVar(damage).contains("InYourHand") && source.isInZone(ZoneType.Hand)) {
                dmg = AbilityUtils.calculateAmount(source, damage, sa) - 1; // the card will be spent casting the spell, so actual damage is 1 less
            } else if (sa.getSVar(damage).equals("TargetedPlayer$CardsInHand")) {
                // cards that deal damage by the number of cards in target player's hand, e.g. Sudden Impact
                if (sa.getTargetRestrictions().canTgtPlayer()) {
                    int maxDmg = 0;
                    Player maxDamaged = null;
                    for (Player p : ai.getOpponents()) {
                        if (p.canBeTargetedBy(sa)) {
                            if (p.getCardsIn(ZoneType.Hand).size() > maxDmg) {
                                maxDmg = p.getCardsIn(ZoneType.Hand).size();
                                maxDamaged = p;
                            }
                        }
                    }
                    if (maxDmg > 0 && maxDamaged != null) {
                        if (shouldTgtP(ai, sa, maxDmg, false)) {
                            sa.resetTargets();
                            sa.getTargets().add(maxDamaged);
                            return true;
                        }
                    } else {
                        return false;
                    }
                }
            }
        }

        if (sourceName.equals("Crater's Claws") && ai.hasFerocious()) {
            dmg += 2;
        }

        String logic = sa.getParamOrDefault("AILogic", "");
        if ("DiscardLands".equals(logic)) {
            dmg = 2;
        } else if ("OpponentHasCreatures".equals(logic)) {
            for (Player opp : ai.getOpponents()) {
                if (!opp.getCreaturesInPlay().isEmpty()) {
                    return true;
                }
            }
        } else if (logic.startsWith("ProcRaid.")) {
            if (ai.getGame().getPhaseHandler().isPlayerTurn(ai) && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                for (Card potentialAtkr : ai.getCreaturesInPlay()) {
                    if (ComputerUtilCard.doesCreatureAttackAI(ai, potentialAtkr)) {
                        return false;
                    }
                }
            }
            if (!ai.getCreaturesAttackedThisTurn().isEmpty()) {
                dmg = Integer.parseInt(logic.substring(logic.indexOf(".") + 1));
            }
        } else if ("WildHunt".equals(logic)) {
            // This dummy ability will just deal 0 damage, but holds the logic for the AI for Master of Wild Hunt
            List<Card> wolves = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), "Creature.Wolf+untapped+YouCtrl+Other", ai, source, sa);
            dmg = Aggregates.sum(wolves, CardPredicates.Accessors.fnGetNetPower);
        } else if ("Triskelion".equals(logic)) {
            final int n = source.getCounters(CounterEnumType.P1P1);
            if (n <= 0) {
                return false;
            }
            // Triskelion is "about to die" if predictThreatenedObjects /
            // combat detection say so. Only then are we allowed to dump
            // remaining counters into the opponent's face for less than
            // lethal damage; otherwise prefer to keep counters around to
            // kill creatures.
            boolean triskAboutToDie = ComputerUtil.playImmediately(ai, sa);
            boolean immediate = triskAboutToDie;
            if (!immediate && !ai.getGame().getStack().isEmpty()) {
                // wait until stack is clear before pinging creatures
                return false;
            }
            if (!damageTargetAI(ai, sa, n, immediate)) {
                return false;
            }
            // If we ended up targeting a player, only allow it when the
            // damage is lethal or Triskelion is about to be removed.
            Player tgtPlayer = sa.getTargets().getFirstTargetedPlayer();
            if (tgtPlayer != null && !triskAboutToDie) {
                if (n < tgtPlayer.getLife()) {
                    return false;
                }
            }
            return true;
        } else if ("NinThePainArtist".equals(logic)) {
            // Make sure not to mana lock ourselves + make the opponent draw cards into an immediate discard
            if (ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN)) {
                boolean doTarget = damageTargetAI(ai, sa, dmg, true);
                if (doTarget) {
                    Card tgt = sa.getTargetCard();
                    if (tgt != null) {
                        return ai.getGame().getPhaseHandler().getPlayerTurn() == tgt.getController();
                    }
                }
            }
            return false;
        }

        if (sourceName.equals("Sorin, Grim Nemesis")) {
            int loyalty = source.getCounters(CounterEnumType.LOYALTY);
            for (; loyalty > 0; loyalty--) {
                if (damageTargetAI(ai, sa, loyalty, false)) {
                    dmg = ComputerUtilCombat.getEnoughDamageToKill(sa.getTargetCard(), loyalty, source, false, false);
                    if (dmg > loyalty || dmg < 1) {
                        continue;   // in case the calculation gets messed up somewhere
                    }
                    sa.setXManaCostPaid(dmg);
                    return true;
                }
            }
            return false;
        }

        if (dmg <= 0) {
            return false;
        }

        // temporarily disabled until better AI
        if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 4, sa)) {
            return false;
        }

        if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source, sa)) {
            return false;
        }

        if (!ComputerUtilCost.checkRemoveCounterCost(abCost, source, sa)) {
            return false;
        }

        if ("DiscardLands".equals(sa.getParam("AILogic")) && !ComputerUtilCost.checkDiscardCost(ai, abCost, source, sa)) {
            return false;
        }

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        // Try to chain damage/debuff effects
        Pair<SpellAbility, Integer> chainDmg = getDamagingSAToChain(ai, sa, damage);

        // test what happens if we chain this to another damaging spell
        if (chainDmg != null) {
            int extraDmg = chainDmg.getValue();
            boolean willTargetIfChained = damageTargetAI(ai, sa, dmg + extraDmg, false);
            if (!willTargetIfChained) {
                return false; // won't play it even in chain
            } else if (willTargetIfChained && chainDmg.getKey().getApi() == ApiType.Pump && sa.getTargets().isTargetingAnyPlayer()) {
                // we're trying to chain a pump spell to a damage spell targeting a player, that won't work
                // so run an additional check to ensure that we want to cast the current spell separately
                sa.resetTargets();
                if (!damageTargetAI(ai, sa, dmg, false)) {
                    return false;
                }
            } else {
                // we are about to decide to play this damage spell; if there's something chained to it, reserve mana for
                // the second spell so we don't misplay
                AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
                aic.reserveManaSourcesForNextSpell(chainDmg.getKey(), sa);
            }
        } else {
            // simple targeting when there is no spell chaining plan
            if (!damageTargetAI(ai, sa, dmg, false)) {
                return false;
            }
        }

        // Avoid wasting a fixed-damage spell on a creature that a cheaper alternative could destroy.
        // Only applies when the spell deals significantly more damage than the target's toughness
        // (e.g. 4-damage spell on a 1-toughness creature) and the AI has an in-hand or in-play
        // damage source (or another damage spell) that could handle it on its own.
        if (!damage.equals("X") && !sa.getTargets().isEmpty() && !sa.getTargets().isTargetingAnyPlayer()
                && !ComputerUtil.aiLifeInDanger(ai, false, 0)
                && !ComputerUtil.playImmediately(ai, sa)
                && allCreatureTargetsAreOverkilledWithAlternative(ai, sa, dmg)) {
            sa.resetTargets();
            return false;
        }

        if ((damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) ||
                sourceName.equals("Crater's Claws")) {
            // If I can kill my target by paying less mana, do it
            if (sa.usesTargeting() && !sa.getTargets().isTargetingAnyPlayer() && !sa.isDividedAsYouChoose()) {
                int actualPay = dmg;
                final boolean noPrevention = sa.hasParam("NoPrevention");
                for (final Card c : sa.getTargets().getTargetCards()) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if (adjDamage < actualPay) {
                        actualPay = adjDamage;
                    }
                }
                if (sourceName.equals("Crater's Claws") && ai.hasFerocious()) {
                    actualPay = actualPay > 2 ? actualPay - 2 : 0;
                }
                sa.setXManaCostPaid(actualPay);
            }
        }

        if ("DiscardCMCX".equals(sa.getParam("AILogic"))) {
            final int cmc = sa.getXManaCostPaid();
            return ai.getZone(ZoneType.Hand).contains(CardPredicates.hasCMC(cmc));
        }

        return true;
    }

    /**
     * Returns true if this divided X-damage spell can be cast right now to kill
     * at least two opposing creatures by splitting the damage between them.
     * Used to override the "hold X damage spell for bigger play" heuristic when
     * the spell already offers good multi-removal value.
     */
    private static boolean canSplitDamageToKillMultipleOppCreatures(final Player ai, final SpellAbility sa, final int maxX) {
        if (maxX <= 0 || !sa.usesTargeting()) {
            return false;
        }
        if (!"RoundedDown".equals(sa.getParam("DivideEvenly")) && !sa.isDividedAsYouChoose()) {
            return false;
        }
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final int maxTargets = tgt.getMaxTargets(source, sa);
        if (maxTargets < 2) {
            return false;
        }

        final List<Card> oppCreatures = Lists.newArrayList();
        for (final Player opp : ai.getOpponents()) {
            for (final Card c : opp.getCreaturesInPlay()) {
                if (c.canBeTargetedBy(sa)
                        && !ComputerUtil.canRegenerate(opp, c)
                        && !c.hasSVar("SacMe")) {
                    oppCreatures.add(c);
                }
            }
        }
        if (oppCreatures.size() < 2) {
            return false;
        }
        oppCreatures.sort(Comparator.comparingInt(Card::getNetToughness));

        final int limit = Math.min(maxTargets, oppCreatures.size());
        for (int n = 2; n <= limit; n++) {
            // Conservatively assume each additional target may add 1 to the cost (e.g. Fireball's RaiseCost),
            // so the effective X budget shrinks by (n - 1).
            final int xForN = maxX - (n - 1);
            if (xForN < n) {
                continue; // not enough X to deal even 1 damage to each target
            }
            final int dmgPer = xForN / n;
            if (dmgPer <= 0) {
                continue;
            }
            int killed = 0;
            for (int i = 0; i < n; i++) {
                if (oppCreatures.get(i).getNetToughness() <= dmgPer) {
                    killed++;
                }
            }
            if (killed >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when every targeted creature is significantly overkilled by
     * this spell and the AI has a cheaper alternative damage source (a creature
     * with a damage activated ability in hand or on the battlefield, or a
     * smaller fixed-damage spell in hand) capable of destroying it. Used to
     * defer wasting a high-damage spell like Psionic Blast on a 1-toughness
     * creature when an Icatian Javelineers (or similar) could handle it.
     */
    /**
     * Returns a "must remove first" creature from the killable list — typically
     * a card whose combat-damage-to-player trigger generates ongoing card
     * advantage or attrition for the opponent (Hypnotic Specter style
     * discard, lifeloss + discard, library mill, etc.). These threats are
     * undervalued by the vanilla creature evaluator (raw P/T + keywords),
     * so we promote them explicitly here.
     */
    private static Card pickPriorityRemovalTarget(final Iterable<Card> killables) {
        Card best = null;
        int bestScore = 0;
        for (Card c : killables) {
            int score = priorityRemovalScore(c);
            if (score > bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return best;
    }

    private static int priorityRemovalScore(final Card c) {
        int score = 0;
        for (Trigger t : c.getTriggers()) {
            if (t.getMode() != TriggerType.DamageDone) {
                continue;
            }
            // Only count triggers that fire when this creature deals damage,
            // generally to a player (combat damage triggers like Hypnotic
            // Specter's discard, Hellfire Sliver's lifeloss, etc.).
            String validSource = t.getParam("ValidSource");
            if (validSource != null && !validSource.contains("Self") && !validSource.contains("Card.Self")) {
                continue;
            }
            String validTarget = t.getParam("ValidTarget");
            if (validTarget != null && !validTarget.contains("Player") && !validTarget.contains("Opponent")) {
                continue;
            }
            SpellAbility exec = t.ensureAbility();
            if (exec == null) {
                continue;
            }
            ApiType api = exec.getApi();
            if (api == ApiType.Discard
                    || api == ApiType.LoseLife
                    || api == ApiType.Mill
                    || api == ApiType.DealDamage
                    || api == ApiType.Sacrifice
                    || api == ApiType.ChangeZone) {
                score += 50;
            } else {
                score += 20;
            }
        }
        // Flying-with-trigger threats (Hypnotic Specter, Vampire Nighthawk
        // style) are particularly hard to remove via combat and deserve an
        // extra bump.
        if (score > 0 && c.hasKeyword(Keyword.FLYING)) {
            score += 25;
        }
        return score;
    }

    private static boolean allCreatureTargetsAreOverkilledWithAlternative(final Player ai, final SpellAbility sa, final int dmg) {
        if (dmg <= 1) {
            return false; // can't be overkill if we deal 1 or less
        }
        boolean anyCreatureTarget = false;
        for (Card tgt : sa.getTargets().getTargetCards()) {
            if (!tgt.isCreature()) {
                // Non-creature target (e.g. planeswalker) — don't apply overkill logic uniformly.
                return false;
            }
            anyCreatureTarget = true;
            int toughness = tgt.getNetToughness();
            if (toughness < 1) {
                toughness = 1;
            }
            // Require at least 2x damage to call it overkill, and an absolute waste of >= 2.
            boolean overkilled = dmg >= toughness * 2 && (dmg - toughness) >= 2;
            if (!overkilled) {
                return false;
            }
            if (!hasCheaperDamageSourceFor(ai, sa, tgt)) {
                return false;
            }
        }
        return anyCreatureTarget;
    }

    private static boolean hasCheaperDamageSourceFor(final Player ai, final SpellAbility currentSa, final Card target) {
        final int needed = Math.max(1, target.getNetToughness());
        final int currentDmg;
        {
            String numDmgStr = currentSa.getParam("NumDmg");
            currentDmg = numDmgStr != null ? AbilityUtils.calculateAmount(currentSa.getHostCard(), numDmgStr, currentSa) : Integer.MAX_VALUE;
        }
        final Card currentSource = currentSa.getHostCard();

        // 1) Creatures already on the battlefield with a damage activated ability strong enough.
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.equals(currentSource)) {
                continue;
            }
            for (SpellAbility ab : c.getSpellAbilities()) {
                if (ab.getApi() != ApiType.DealDamage || !ab.isAbility()) {
                    continue;
                }
                String numDmg = ab.getParam("NumDmg");
                if (numDmg == null) {
                    continue;
                }
                int otherDmg = AbilityUtils.calculateAmount(c, numDmg, ab);
                if (otherDmg >= needed) {
                    return true;
                }
            }
        }

        // 2) Creatures in hand with damage activated abilities (will be usable a turn after being cast).
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!c.isCreature()) {
                continue;
            }
            for (SpellAbility ab : c.getSpellAbilities()) {
                if (ab.getApi() != ApiType.DealDamage || !ab.isAbility()) {
                    continue;
                }
                String numDmg = ab.getParam("NumDmg");
                if (numDmg == null) {
                    continue;
                }
                int otherDmg = AbilityUtils.calculateAmount(c, numDmg, ab);
                if (otherDmg >= needed) {
                    return true;
                }
            }
        }

        // 3) Other fixed-damage spells in hand that could kill the target with less waste.
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.equals(currentSource) || c.isCreature() || c.isLand()) {
                continue;
            }
            for (SpellAbility ab : c.getSpellAbilities()) {
                if (!ab.isSpell() || ab.getApi() != ApiType.DealDamage) {
                    continue;
                }
                String numDmg = ab.getParam("NumDmg");
                if (numDmg == null || numDmg.equals("X")) {
                    continue;
                }
                int otherDmg = AbilityUtils.calculateAmount(c, numDmg, ab);
                if (otherDmg >= needed && otherDmg < currentDmg) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * dealDamageChooseTgtC.
     * </p>
     *
     * @param d
     *            a int.
     * @param noPrevention
     *            a boolean.
     * @param pl
     *            a {@link forge.game.player.Player} object.
     * @param mandatory
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    private Card dealDamageChooseTgtC(final Player ai, final SpellAbility sa, final int d, final boolean noPrevention,
            final Player pl, final boolean mandatory) {
        // wait until stack is empty (prevents duplicate kills)
        if (!sa.isTrigger() && !ai.getGame().getStack().isEmpty()) {
            //TODO:all removal APIs require a check to prevent duplicate kill/bounce/exile/etc.
            //      The original code is a blunt instrument that also blocks all use of removal as interrupts. The issue is
            //      with the AI not having code to consider what occurred previously in the stack thus it has no memory of
            //      removing a target already if something else is placed on top of the stack. A better solution is to place
            //      the checking mechanism after the target is chosen and determine if the topstack invalidates the earlier
            //      removal (shroud effect, pump against damage) so a new removal can/should be applied if possible.
            //return null;
        }
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player activator = sa.getActivatingPlayer();
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        List<Card> hPlay = getTargetableCards(ai, sa, pl, tgt, activator, source, game);

        // Filter MustTarget requirements
        StaticAbilityMustTarget.filterMustTargetCards(ai, hPlay, sa);

        CardCollection killables = CardLists.filter(hPlay, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getSVar("Targeting").equals("Dies")
                        || (ComputerUtilCombat.getEnoughDamageToKill(c, d, source, false, noPrevention) <= d)
                            && !ComputerUtil.canRegenerate(ai, c)
                            && !c.hasSVar("SacMe")
                            && !ComputerUtilCard.hasActiveUndyingOrPersist(c);
            }
        });

        // Filter AI-specific targets if provided
        killables = ComputerUtil.filterAITgts(sa, ai, killables, true);

        // Try not to target anything which will already be dead by the time the spell resolves
        killables = ComputerUtil.filterCreaturesThatWillDieThisTurn(ai, killables, sa);

        Card targetCard = null;
        if (pl.isOpponentOf(ai) && activator.equals(ai) && !killables.isEmpty()) {
            if (sa.getTargetRestrictions().canTgtPlaneswalker()) {
                targetCard = ComputerUtilCard.getBestPlaneswalkerAI(killables);
            }
            if (targetCard == null) {
                // Before picking the "best" creature by raw evaluator score,
                // give priority to creatures with damage-on-player triggers
                // that punish the AI (Hypnotic Specter's discard, Juzam's
                // lifeloss-then-discard style, etc.). Vanilla evaluators
                // undervalue such 2/2 fliers, leaving them in play for free
                // card advantage.
                Card priority = pickPriorityRemovalTarget(killables);
                targetCard = priority != null
                        ? priority
                        : ComputerUtilCard.getBestCreatureAI(killables);
            }

            return targetCard;
        }

        if (!mandatory) {
            return null;
        }

        // try unfiltered now
        hPlay = getTargetableCards(pl, sa, pl, tgt, activator, source, game);
        List<Card> controlledByOpps = CardLists.filterControlledBy(hPlay, ai.getOpponents());

        if (!hPlay.isEmpty()) {
            if (pl.isOpponentOf(ai) && activator.equals(ai)) {
                if (sa.getTargetRestrictions().canTgtPlaneswalker()) {
                    targetCard = ComputerUtilCard.getBestPlaneswalkerAI(controlledByOpps);
                }
                if (targetCard == null) {
                    targetCard = ComputerUtilCard.getBestCreatureAI(controlledByOpps);
                }
            }
            if (targetCard == null) {
                targetCard = ComputerUtilCard.getWorstCreatureAI(hPlay);
            }

            return targetCard;
        }

        return null;
    }

    /**
     * <p>
     * dealDamageChooseTgtPW.
     * </p>
     *
     * @param d
     *            a int.
     * @param noPrevention
     *            a boolean.
     * @param pl
     *            a {@link forge.game.player.Player} object.
     * @param mandatory
     *            a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    private Card dealDamageChooseTgtPW(final Player ai, final SpellAbility sa, final int d, final boolean noPrevention,
                                       final Player pl, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Player activator = sa.getActivatingPlayer();
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        List<Card> hPlay = CardLists.filter(getTargetableCards(ai, sa, pl, tgt, activator, source, game), CardPredicates.Presets.PLANESWALKERS);

        CardCollection killables = CardLists.filter(hPlay, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getSVar("Targeting").equals("Dies")
                        || (ComputerUtilCombat.getEnoughDamageToKill(c, d, source, false, noPrevention) <= d)
                        && !ComputerUtil.canRegenerate(ai, c)
                        && !c.hasSVar("SacMe");
            }
        });

        // Filter AI-specific targets if provided
        killables = ComputerUtil.filterAITgts(sa, ai, killables, true);

        // We can kill a planeswalker, so go for it
        if (pl.isOpponentOf(ai) && activator.equals(ai) && !killables.isEmpty()) {
            return ComputerUtilCard.getBestPlaneswalkerAI(killables);
        }

        // We can hurt a planeswalker, so rank the one which is the best target
        if (!hPlay.isEmpty() && pl.isOpponentOf(ai) && activator.equals(ai)) {
            Card pw = ComputerUtilCard.getBestPlaneswalkerToDamage(hPlay);
            return pw == null && mandatory ? hPlay.get(0) : pw;
        }

        return null;
    }

    private List<Card> getTargetableCards(Player ai, SpellAbility sa, Player pl, TargetRestrictions tgt, Player activator, Card source, Game game) {
        List<Card> hPlay = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), tgt.getValidTgts(), activator, source, sa);

        if (activator.equals(ai)) {
            hPlay = CardLists.filterControlledBy(hPlay, pl);
        }

        final List<GameObject> objects = Lists.newArrayList(sa.getTargets());
        if (sa.hasParam("TargetUnique")) {
            objects.addAll(sa.getUniqueTargets());
        }
        for (final Object o : objects) {
            if (o instanceof Card) {
                hPlay.remove(o);
            }
        }
        hPlay = CardLists.getTargetableCards(hPlay, sa);
        return hPlay;
    }

    /**
     * <p>
     * damageTargetAI.
     * </p>
     *
     * @param saMe
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageTargetAI(final Player ai, final SpellAbility saMe, final int dmg, final boolean immediately) {
        final TargetRestrictions tgt = saMe.getTargetRestrictions();

        if (tgt == null) {
            return damageChooseNontargeted(ai, saMe, dmg);
        }

        if (tgt.isRandomTarget()) {
            return false;
        }

        return damageChoosingTargets(ai, saMe, tgt, dmg, false, immediately);
    }

    /**
     * <p>
     * damageChoosingTargets.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param dmg
     *            a int.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean damageChoosingTargets(final Player ai, final SpellAbility sa, final TargetRestrictions tgt, int dmg,
            final boolean mandatory, boolean immediately) {
        final Card source = sa.getHostCard();
        final boolean noPrevention = sa.hasParam("NoPrevention");
        final Game game = source.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final boolean divided = sa.isDividedAsYouChoose();
        final boolean oppTargetsChoice = sa.hasParam("TargetingPlayer");
        final String logic = sa.getParamOrDefault("AILogic", "");

        PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        Player enemy = targetableOpps.min(PlayerPredicates.compareByLife());
        if (enemy == null) {
            enemy = ai.getWeakestOpponent();
        }

        if ("PowerDmg".equals(logic)) {
            // check if it is better to target the player instead, the original target is already set in PumpAi.pumpTgtAI()
            if (tgt.canTgtCreatureAndPlayer() && shouldTgtP(ai, sa, dmg, noPrevention)) {
                sa.resetTargets();
                sa.getTargets().add(enemy);
            }
            return true;
        }

        // AssumeAtLeastOneTarget is used for cards with funky targeting implementation like Fight with Fire which would
        // otherwise confuse the AI by returning 0 unexpectedly during SA "AI can play" tests.
        if (tgt.getMaxTargets(source, sa) <= 0 && !logic.equals("AssumeAtLeastOneTarget")) {
            return false;
        }

        sa.resetTargets();

        // target loop
        TargetChoices tcs = sa.getTargets();

        // Do not use if would kill self
        if ("SelfDamage".equals(logic) && ai.getLife() <= Integer.parseInt(source.getSVar("SelfDamageAmount"))) {
            return false;
        }

        if ("ChoiceBurn".equals(logic)) {
            // do not waste burns on player if other choices are present
            if (shouldTgtP(ai, sa, dmg, noPrevention)) {
                tcs.add(enemy);
                return true;
            }
            return false;
        }
        if ("Polukranos".equals(logic)) {
            int dmgTaken = 0;
            CardCollection humCreatures = enemy.getCreaturesInPlay();
            Card lastTgt = null;
            humCreatures = CardLists.getTargetableCards(humCreatures, sa);
            ComputerUtilCard.sortByEvaluateCreature(humCreatures);
            // try to kill things without dying
            for (Card humanCreature : humCreatures) {
                if (FightAi.canKill(humanCreature, source, dmgTaken)) {
                    continue;
                }
                final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(humanCreature, dmg, source, false, noPrevention);
                if (assignedDamage <= dmg
                        && humanCreature.getShieldCount() == 0 && !ComputerUtil.canRegenerate(humanCreature.getController(), humanCreature)) {
                    tcs.add(humanCreature);
                    sa.addDividedAllocation(humanCreature, assignedDamage);
                    lastTgt = humanCreature;
                    dmg -= assignedDamage;
                }
                // protection is already checked by target above
                dmgTaken += humanCreature.getNetPower();

                if (dmg == 0) {
                    return true;
                }
            }
            if (dmg > 0 && lastTgt != null) {
                sa.addDividedAllocation(lastTgt, sa.getDividedValue(lastTgt) + dmg);
                dmg = 0;
                return true;
            }
            // get safe target to dump damage
            for (Card humanCreature : humCreatures) {
                if (FightAi.canKill(humanCreature, source, 0)) {
                    continue;
                }
                tcs.add(humanCreature);
                sa.addDividedAllocation(humanCreature, dmg);
                dmg = 0;
                return true;
            }
        }

        immediately = immediately || ComputerUtil.playImmediately(ai, sa);

        int totalTargetedSoFar = -1;
        while (sa.canAddMoreTarget()) {
            if (totalTargetedSoFar == tcs.size()) {
                // Avoid looping endlessly when choosing targets for cards with variable target number and type
                // like Jaya's Immolating Inferno
                break;
            }
            totalTargetedSoFar = tcs.size();
            if (oppTargetsChoice && sa.getActivatingPlayer().equals(ai) && !sa.isTrigger()) {
                // canPlayAI (sa activated by ai)
                Player targetingPlayer = AbilityUtils.getDefinedPlayers(source, sa.getParam("TargetingPlayer"), sa).get(0);
                sa.setTargetingPlayer(targetingPlayer);
                return targetingPlayer.getController().chooseTargetsFor(sa);
            }

            if (tgt.canTgtPlaneswalker()) {
                // We can damage planeswalkers with this, consider targeting.
                Card c = dealDamageChooseTgtPW(ai, sa, dmg, noPrevention, enemy, false);
                if (c != null && !shouldTgtP(ai, sa, dmg, noPrevention)) {
                    tcs.add(c);
                    if (divided) {
                        int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        assignedDamage = Math.min(dmg, assignedDamage);
                        sa.addDividedAllocation(c, assignedDamage);
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }
            }

            if (tgt.canTgtCreatureAndPlayer()) {
                Card c = null;

                if (shouldTgtP(ai, sa, dmg, noPrevention)) {
                    tcs.add(enemy);
                    if (divided) {
                        sa.addDividedAllocation(enemy, dmg);
                        break;
                    }
                    continue;
                }
                if ("RoundedDown".equals(sa.getParam("DivideEvenly"))) {
                    dmg = dmg * sa.getTargets().size() / (sa.getTargets().size() +1);
                }

                // look for creature targets; currently also catches planeswalkers that can be killed immediately
                c = dealDamageChooseTgtC(ai, sa, dmg, noPrevention, enemy, false);
                if (c != null) {
                    //option to hold removal instead only applies for single targeted removal
                    if (sa.isSpell() && !divided && !immediately && tgt.getMaxTargets(source, sa) == 1) {
                        if (!ComputerUtilCard.useRemovalNow(sa, c, dmg, ZoneType.Graveyard)) {
                            return false;
                        }
                    }
                    tcs.add(c);
                    if (divided) {
                        final int assignedDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        if (assignedDamage <= dmg) {
                            sa.addDividedAllocation(c, assignedDamage);
                        }
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }

                // When giving priority to targeting Creatures for mandatory
                // triggers feel free to add the Human after we run out of good targets

                // TODO: add check here if card is about to die from something
                // on the stack or from taking combat damage

                final Cost abCost = sa.getPayCosts();
                boolean freePing = immediately || abCost == null || sa.getTargets().size() > 0;

                if (!source.isSpell()) {
                    if (phase.is(PhaseType.END_OF_TURN) && sa.isAbility() && abCost.isReusuableResource()) {
                        if (phase.getNextTurn().equals(ai))
                            freePing = true;
                    }

                    if (phase.is(PhaseType.MAIN2) && sa.isAbility()) {
                        if (sa.isPwAbility() || source.hasSVar("EndOfTurnLeavePlay"))
                            freePing = true;
                    }
                }

                if (freePing && sa.canTarget(enemy) && !avoidTargetP(ai, sa)) {
                    tcs.add(enemy);
                    if (divided) {
                        sa.addDividedAllocation(enemy, dmg);
                        break;
                    }
                }
            } else if (tgt.canTgtCreature() || tgt.canTgtPlaneswalker()) {
                final Card c = dealDamageChooseTgtC(ai, sa, dmg, noPrevention, enemy, mandatory);
                if (c != null) {
                    //option to hold removal instead only applies for single targeted removal
                    if (!immediately && tgt.getMaxTargets(source, sa) == 1 && !divided) {
                        if (!ComputerUtilCard.useRemovalNow(sa, c, dmg, ZoneType.Graveyard)) {
                            return false;
                        }
                    }
                    tcs.add(c);
                    if (divided) {
                        // if only other legal targets hurt own stuff just dump all dmg into this
                        final Card nextTarget = dealDamageChooseTgtC(ai, sa, dmg, noPrevention, enemy, mandatory);
                        boolean dump = false;
                        if (nextTarget != null && nextTarget.getController().equals(ai)) {
                            dump = true;
                        }
                        final int assignedDamage = dump ? dmg : ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                        sa.addDividedAllocation(c, Math.min(assignedDamage, dmg));
                        dmg = dmg - assignedDamage;
                        if (dmg <= 0) {
                            break;
                        }
                    }
                    continue;
                }
            } else if ("OppAtTenLife".equals(logic)) {
                for (final Player p : ai.getOpponents()) {
                    if (sa.canTarget(p) && p.getLife() == 10) {
                        tcs.add(p);
                        return true;
                    }
                }
                return false;
            }
            if (sa.canTarget(enemy) && sa.canAddMoreTarget()) {
                if ((phase.is(PhaseType.END_OF_TURN) && phase.getNextTurn().equals(ai))
                        || (isSorcerySpeed(sa, ai) && phase.is(PhaseType.MAIN2))
                        || immediately) {
                    boolean pingAfterAttack = "PingAfterAttack".equals(logic) && phase.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.isPlayerTurn(ai);
                    boolean isPWAbility = sa.isPwAbility() && sa.getPayCosts().hasSpecificCostType(CostPutCounter.class);
                    if (isPWAbility || (pingAfterAttack && !avoidTargetP(ai, sa)) || shouldTgtP(ai, sa, dmg, noPrevention)) {
                        tcs.add(enemy);
                        if (divided) {
                            sa.addDividedAllocation(enemy, dmg);
                            break;
                        }
                    }
                }
            }
        }

        // fell through all the choices, no targets left?
        int minTgts = tgt.getMinTargets(source, sa);
        if (tcs.size() < minTgts || tcs.size() == 0) {
            if (mandatory) {
                // Sanity check: if there are any legal non-owned targets after the check (which may happen for complex cards like Rift Bolt),
                // choose a random opponent's target before forcing targeting of own stuff
                List<GameEntity> allTgtEntities = sa.getTargetRestrictions().getAllCandidates(sa, true);
                for (GameEntity ent : allTgtEntities) {
                    if ((ent instanceof Player && ((Player)ent).isOpponentOf(ai))
                            || (ent instanceof Card && ((Card)ent).getController().isOpponentOf(ai))) {
                        tcs.add(ent);
                    }
                    if (tcs.size() == minTgts) {
                        return true;
                    }
                }

                // If the trigger is mandatory, gotta choose my own stuff now
                return damageChooseRequiredTargets(ai, sa, tgt, dmg);
            }
            sa.resetTargets();
            return false;
        }

        // if opponent will gain life (ex. Fiery Justice), don't target only enemy player unless life gain is harmful or ignored
        if ("OpponentGainLife".equals(logic) && tcs.size() == 1 && tcs.contains(enemy) && ComputerUtil.lifegainPositive(enemy, source)) {
            sa.resetTargets();
            return false;
        }

        return true;
    }

    /**
     * <p>
     * damageChooseNontargeted.
     * </p>
     * @param ai
     *
     * @param saMe
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageChooseNontargeted(Player ai, final SpellAbility saMe, final int dmg) {
        // TODO: Improve circumstances where the Defined Damage is unwanted
        final List<GameEntity> objects = AbilityUtils.getDefinedEntities(saMe.getHostCard(), saMe.getParam("Defined"), saMe);
        boolean urgent = false; // can it wait?
        boolean positive = false;

        for (final GameEntity o : objects) {
            if (o instanceof Card) {
                Card c = (Card) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(c, dmg, saMe.getHostCard(), false);
                if (!c.hasKeyword(Keyword.INDESTRUCTIBLE) && ComputerUtilCombat.getDamageToKill(c, false) <= restDamage) {
                    if (c.getController().equals(ai)) {
                        return false;
                    }
                    urgent = true;
                }
                if (c.getController().isOpponentOf(ai) ^ c.getName().equals("Stuffy Doll")) {
                    positive = true;
                }
            } else if (o instanceof Player) {
                final Player p = (Player) o;
                final int restDamage = ComputerUtilCombat.predictDamageTo(p, dmg, saMe.getHostCard(), false);
                if (restDamage > 0 && p.canLoseLife()) {
                    if (!p.isOpponentOf(ai) && restDamage + 3 >= p.getLife()) {
                        // from this spell will kill me
                        return false;
                    }
                    if (p.isOpponentOf(ai)) {
                        positive = true;
                        if (p.getLife() - 3 <= restDamage) {
                            urgent = true;
                        }
                    }
                }
            }
        }
        if ("Atarka's Command".equals(ComputerUtilAbility.getAbilitySourceName(saMe))) {
            // playReusable wrongly assumes that CharmEffect options are re-usable
            return positive;
        }
        if (!positive && !(saMe instanceof AbilitySub)) {
            return false;
        }
        if (!urgent && !playReusable(ai, saMe)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * damageChooseRequiredTargets.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param tgt
     *            a {@link forge.game.spellability.TargetRestrictions} object.
     * @param dmg
     *            a int.
     * @return a boolean.
     */
    private boolean damageChooseRequiredTargets(final Player ai, final SpellAbility sa, final TargetRestrictions tgt, final int dmg) {
        // this is for Triggered targets that are mandatory
        final boolean noPrevention = sa.hasParam("NoPrevention");
        final boolean divided = sa.isDividedAsYouChoose();
        PlayerCollection opps = ai.getOpponents();

        while (sa.canAddMoreTarget()) {
            if (tgt.canTgtPlaneswalker()) {
                final Card c = dealDamageChooseTgtPW(ai, sa, dmg, noPrevention, ai, true);
                if (c != null) {
                    sa.getTargets().add(c);
                    if (divided) {
                        sa.addDividedAllocation(c, dmg);
                        break;
                    }
                    continue;
                }
            }

            // TODO: This currently also catches planeswalkers that can be killed (still necessary? Or can be removed?)
            if (tgt.canTgtCreature()) {
                final Card c = dealDamageChooseTgtC(ai, sa, dmg, noPrevention, ai, true);
                if (c != null) {
                    sa.getTargets().add(c);
                    if (divided) {
                        sa.addDividedAllocation(c, dmg);
                        break;
                    }
                    continue;
                }
            }

            if (!opps.isEmpty()) {
                Player opp = opps.getFirst();
                opps.remove(opp);
                if (sa.canTarget(opp)) {
                    if (sa.getTargets().add(opp)) {
                        if (divided) {
                            sa.addDividedAllocation(opp, dmg);
                            break;
                        }
                    }
                }
                continue;
            }

            // See if there's an indestructible target that can be used
            CardCollection indestructible = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                    Predicates.and(CardPredicates.Presets.CREATURES, CardPredicates.Presets.PLANESWALKERS, CardPredicates.hasKeyword(Keyword.INDESTRUCTIBLE), CardPredicates.isTargetableBy(sa)));

            if (!indestructible.isEmpty()) {
                Card c = ComputerUtilCard.getWorstPermanentAI(indestructible, false, false, false, false);
                sa.getTargets().add(c);
                if (divided) {
                    sa.addDividedAllocation(c, dmg);
                    break;
                }
                continue;
            }
            else if (tgt.canTgtPlaneswalker()) {
                // Second pass for planeswalkers: choose AI's worst planeswalker
                final Card c = ComputerUtilCard.getWorstPlaneswalkerToDamage(CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Predicates.and(CardPredicates.Presets.PLANESWALKERS), CardPredicates.isTargetableBy(sa)));
                if (c != null) {
                    sa.getTargets().add(c);
                    if (divided) {
                        sa.addDividedAllocation(c, dmg);
                        break;
                    }
                    continue;
                }
            }

            if (sa.canTarget(ai)) {
                if (sa.getTargets().add(ai)) {
                    if (divided) {
                        sa.addDividedAllocation(ai, dmg);
                        break;
                    }
                    continue;
                }
            }

            // if we get here then there isn't enough targets, this is the only
            // time we can return false
            return false;
        }
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final String damage = sa.getParam("NumDmg");
        int dmg = AbilityUtils.calculateAmount(source, damage, sa);

        // Remove all damage
        if (sa.hasParam("Remove")) {
            return true;
        }

        if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            dmg = ComputerUtilCost.getMaxXValue(sa, ai, true);
            sa.setXManaCostPaid(dmg);
        }

        if (!sa.usesTargeting()) {
            // If it's not mandatory check a few things
            return mandatory || damageChooseNontargeted(ai, sa, dmg);
        } else {
            if (!damageChoosingTargets(ai, sa, sa.getTargetRestrictions(), dmg, mandatory, true) && !mandatory) {
                return false;
            }

            if (damage.equals("X") && sa.getSVar(damage).equals("Count$xPaid") && !sa.isDividedAsYouChoose()) {
                // If I can kill my target by paying less mana, do it
                int actualPay = 0;
                final boolean noPrevention = sa.hasParam("NoPrevention");

                //target is a player
                if (!sa.getTargets().isTargetingAnyCard()) {
                    actualPay = dmg;
                }
                for (final Card c : sa.getTargets().getTargetCards()) {
                    final int adjDamage = ComputerUtilCombat.getEnoughDamageToKill(c, dmg, source, false, noPrevention);
                    if (adjDamage > actualPay) {
                        actualPay = adjDamage;
                    }
                }

                sa.setXManaCostPaid(actualPay);
            }
        }

        return true;
    }

    private boolean doXLifeDrainLogic(Player ai, SpellAbility sa) {
        Card source = sa.getHostCard();
        String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);

        // detect the top ability that actually targets in Drain Life and Soul Burn scripts
        SpellAbility saTgt = sa.getRootAbility();

        Player opponent = ai.getWeakestOpponent();

        // TODO: somehow account for the possible cost reduction?
        int dmg = ComputerUtilMana.determineLeftoverMana(sa, ai, MagicColor.toShortString(saTgt.getParam("XColor")), false);

        while (!ComputerUtilMana.canPayManaCost(sa, ai, dmg, false) && dmg > 0) {
            // TODO: ideally should never get here, currently put here as a precaution for complex mana base cases where the miscalculation might occur. Will remove later if it proves to never trigger.
            dmg--;
            System.out.println("Warning: AI could not pay mana cost for a XLifeDrain logic spell. Reducing X value to "+dmg);
        }

        // set the color map for black X for the purpose of Soul Burn
        // TODO: somehow generalize this calculation to allow other potential similar cards to function in the future
        if ("Soul Burn".equals(sourceName)) {
            Map<String, Integer> xByColor = Maps.newHashMap();
            xByColor.put("B", dmg - ComputerUtilMana.determineLeftoverMana(sa, ai, "R", false));
            source.setXManaCostPaidByColor(xByColor);
        }

        if (dmg < 3 && dmg < opponent.getLife()) {
            return false;
        }

        CardCollection creatures = ai.getOpponents().getCreaturesInPlay();

        Card tgtCreature = null;
        for (Card c : creatures) {
            int power = c.getNetPower();
            int toughness = c.getNetToughness();
            boolean canDie = !ComputerUtilCombat.combatantCantBeDestroyed(c.getController(), c);

            // Currently will target creatures with toughness 3+ (or power 5+)
            // and only if the creature can actually die, do not "underdrain"
            // unless the creature has high power
            if (canDie && toughness <= dmg && ((toughness == dmg && toughness >= 3) || power >= 5)) {
                tgtCreature = c;
                break;
            }
        }

        saTgt.resetTargets();
        saTgt.getTargets().add(tgtCreature != null && dmg < opponent.getLife() ? tgtCreature : opponent);

        saTgt.setXManaCostPaid(dmg);
        return true;
    }

    // Returns a pair of a SpellAbility (APIType DealDamage or Pump) and damage/debuff amount
    // The returned spell ability can be chained to "sa" to deal more damage (enough mana is available to cast both
    // and can be properly reserved).
    public static Pair<SpellAbility, Integer> getDamagingSAToChain(Player ai, SpellAbility sa, String damage) {
        if (!ai.getController().isAI()) {
            return null; // should only work for the actual AI player
        } else if (((PlayerControllerAi)ai.getController()).getAi().usesSimulation()) {
            // simulated AI shouldn't use paired decisions, it tries to find complex decisions on its own
            return null;
        }

        Game game = ai.getGame();
        int chance = ((PlayerControllerAi)ai.getController()).getAi().getIntProperty(AiProps.CHANCE_TO_CHAIN_TWO_DAMAGE_SPELLS);

        if (chance > 0 && (ComputerUtilCombat.lifeInDanger(ai, game.getCombat()) || ComputerUtil.aiLifeInDanger(ai, true, 0))) {
            chance = 100; // in danger, do it even if normally the chance is low (unless chaining is completely disabled)
        }

        if (!MyRandom.percentTrue(chance, ai.getGame().getRandom())) {
            return null;
        }

        if (sa.getSubAbility() != null || sa.getParent() != null) {
            // Doesn't work yet for complex decisions where damage is only a part of the decision process
            return null;
        }

        // chaining to this could miscalculate
        if (sa.isDividedAsYouChoose()) {
            return null;
        }

        // Try to chain damage/debuff effects
        if (StringUtils.isNumeric(damage) || (damage.startsWith("-") && StringUtils.isNumeric(damage.substring(1)))) {
            // currently only works for predictable numeric damage
            CardCollection cards = new CardCollection();
            cards.addAll(ai.getCardsIn(ZoneType.Hand));
            cards.addAll(ai.getCardsIn(ZoneType.Battlefield));
            cards.addAll(ai.getCardsActivatableInExternalZones(true));
            for (Card c : cards) {
                if (c.getZone().getPlayer() != null && c.getZone().getPlayer() != ai && c.mayPlay(ai).isEmpty()) {
                    continue;
                }
                for (SpellAbility ab : c.getSpellAbilities()) {
                    if (ab.equals(sa) || ab.getSubAbility() != null) { // decisions for complex SAs with subs are not supported yet
                        continue;
                    }
                    if (!ab.canPlay()) {
                        continue;
                    }
                    // currently works only with cards that don't have additional costs (only mana is supported)
                    if (ab.getPayCosts().hasNoManaCost() || ab.getPayCosts().hasOnlySpecificCostType(CostPartMana.class)) {
                        String dmgDef = "";
                        if (ab.getApi() == ApiType.DealDamage) {
                            dmgDef = ab.getParamOrDefault("NumDmg", "0");
                        } else if (ab.getApi() == ApiType.Pump) {
                            dmgDef = ab.getParamOrDefault("NumDef", "0");
                            if (dmgDef.startsWith("-")) {
                                dmgDef = dmgDef.substring(1);
                            } else {
                                continue; // not a toughness debuff
                            }
                        }
                        if (StringUtils.isNumeric(dmgDef)) { // currently doesn't work for X and other dependent costs
                            if (sa.usesTargeting() && ab.usesTargeting()) {
                                // Ensure that the chained spell can target at least the same things (or more) as the current one
                                TargetRestrictions tgtSa = sa.getTargetRestrictions();
                                TargetRestrictions tgtAb = sa.getTargetRestrictions();
                                String[] validTgtsSa = tgtSa.getValidTgts();
                                String[] validTgtsAb = tgtAb.getValidTgts();
                                if (!Arrays.asList(validTgtsSa).containsAll(Arrays.asList(validTgtsAb))) {
                                    continue;
                                }

                                // FIXME: should it also check restrictions for targeting players?
                                ManaCost costSa = sa.getPayCosts().getTotalMana();
                                ManaCost costAb = ab.getPayCosts().getTotalMana(); // checked for null above
                                ManaCost total = ManaCost.combine(costSa, costAb);
                                SpellAbility combinedAb = ab.copyWithDefinedCost(new Cost(total, false));
                                // can we pay both costs?
                                if (ComputerUtilMana.canPayManaCost(combinedAb, ai, 0, false)) {
                                    return Pair.of(ab, Integer.parseInt(dmgDef));
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
