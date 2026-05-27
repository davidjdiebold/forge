package forge.ai.ability;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilCost;
import forge.ai.PlayerControllerAi;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentCreatureAi extends PermanentAi {

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {

        if ("Never".equals(aiLogic)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card card = sa.getHostCard();
        final Game game = ai.getGame();

        // FRF Dash Keyword
        if (sa.isDash()) {
            //only checks that the dashed creature will attack
            if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                if (game.getReplacementHandler().wouldPhaseBeSkipped(ai, "BeginCombat"))
                    return false;
                if (ComputerUtilCost.canPayCost(sa.getHostCard().getSpellPermanent(), ai, false)) {
                    //do not dash if creature can be played normally
                    return false;
                }
                Card dashed = CardUtil.getLKICopy(sa.getHostCard());
                dashed.setSickness(false);
                return ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, dashed);
            } else {
                return false;
            }
        }

        // Blitz Keyword: avoid casting in Main2
        if (sa.isBlitz() && ph.getPhase().isAfter(PhaseType.MAIN1)) {
            return false;
        }

        // Prevent the computer from summoning Ball Lightning type creatures
        // after attacking
        if (card.hasSVar("EndOfTurnLeavePlay")
                && (!ph.isPlayerTurn(ai) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || game.getReplacementHandler().wouldPhaseBeSkipped(ai, "BeginCombat"))) {
            // AiPlayDecision.AnotherTime
            return false;
        }

        // Flash logic
        boolean advancedFlash = false;
        if (ai.getController().isAI()) {
            advancedFlash = ((PlayerControllerAi)ai.getController()).getAi().getBooleanProperty(AiProps.FLASH_ENABLE_ADVANCED_LOGIC);
        }
        if (card.hasKeyword(Keyword.FLASH) || (!ai.canCastSorcery() && sa.canCastTiming(ai))) {
            if (advancedFlash) {
                return doAdvancedFlashLogic(card, ai, sa);
            } else {
                // save cards with flash for surprise blocking
                if ((ai.isUnlimitedHandSize() || ai.getCardsIn(ZoneType.Hand).size() <= ai.getMaxHandSize()
                        || ph.getPhase().isBefore(PhaseType.END_OF_TURN))
                        && ai.getManaPool().totalMana() <= 0
                        && (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                        && (!card.hasETBTrigger(true) && !card.hasSVar("AmbushAI"))
                        && game.getStack().isEmpty()
                        && !ComputerUtil.castPermanentInMain1(ai, sa)) {
                    // AiPlayDecision.AnotherTime;
                    return false;
                }
            }
        }

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    private boolean doAdvancedFlashLogic(Card card, final Player ai, SpellAbility sa) {
        Game game = ai.getGame();
        PhaseHandler ph = game.getPhaseHandler();
        Combat combat = game.getCombat();
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();

        boolean isOppTurn = ph.getPlayerTurn().isOpponentOf(ai);
        boolean isOwnEOT = ph.is(PhaseType.END_OF_TURN, ai);
        boolean isEOTBeforeMyTurn = ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
        boolean isMyDeclareBlockers = ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS, ai) && ph.inCombat();
        boolean isOppDeclareAttackers = ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && isOppTurn && ph.inCombat();
        boolean isMyMain1OrLater = ph.is(PhaseType.MAIN1, ai) || (ph.getPhase().isAfter(PhaseType.MAIN1) && ph.getPlayerTurn().equals(ai));
        boolean canRespondToStack = false;
        if (!game.getStack().isEmpty()) {
            SpellAbility peekSa = game.getStack().peekAbility();
            Player activator = peekSa.getActivatingPlayer();
            if (activator != null && activator.isOpponentOf(ai) && peekSa.getApi() != ApiType.DestroyAll
                    && peekSa.getApi() != ApiType.DamageAll) {
                canRespondToStack = true;
            }
        }

        boolean hasETBTrigger = card.hasETBTrigger(true);
        boolean hasAmbushAI = card.hasSVar("AmbushAI");
        boolean defOnlyAmbushAI = hasAmbushAI && "BlockOnly".equals(card.getSVar("AmbushAI"));
        boolean loseFloatMana = ai.getManaPool().totalMana() > 0 && !ManaEffectAi.canRampPool(ai, card);
        boolean willDiscardNow = isOwnEOT && !ai.isUnlimitedHandSize() && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize();
        boolean willDieNow = combat != null && ComputerUtilCombat.lifeInSeriousDanger(ai, combat);
        boolean wantToCastInMain1 = ph.is(PhaseType.MAIN1, ai) && ComputerUtil.castPermanentInMain1(ai, sa);
        boolean isCommander = card.isCommander();

        // figure out if the card might be a valuable blocker
        boolean valuableBlocker = false;
        if (combat != null && combat.getDefendingPlayers().contains(ai)) {
            // Currently we use a rather simplistic assumption that if we're behind on creature count on board,
            // a flashed in creature might prove to be good as an additional defender
            int numUntappedPotentialBlockers = CardLists.filter(ai.getCreaturesInPlay(), new Predicate<Card>() {
                @Override
                public boolean apply(final Card card) {
                    return card.isUntapped() && !ComputerUtilCard.isUselessCreature(ai, card);
                }
            }).size();

            if (combat.getAttackersOf(ai).size() > numUntappedPotentialBlockers) {
                valuableBlocker = true;
            }
        }

        int chanceToObeyAmbushAI = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_OBEY_AMBUSHAI);
        int chanceToAddBlocker = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_AS_VALUABLE_BLOCKER);
        int chanceToCastForETB = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_DUE_TO_ETB_EFFECTS);
        int chanceToRespondToStack = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_RESPOND_TO_STACK_WITH_ETB);
        int chanceToProcETBBeforeMain1 = aic.getIntProperty(AiProps.FLASH_CHANCE_TO_CAST_FOR_ETB_BEFORE_MAIN1);
        boolean canCastAtOppTurn = true;
        for (Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            for (StaticAbility s : c.getStaticAbilities()) {
                if ("CantBeCast".equals(s.getParam("Mode")) && StringUtils.contains(s.getParam("Activator"), "NonActive")
                        && (!s.getParam("Activator").startsWith("You") || c.getController().equals(ai))) {
                    canCastAtOppTurn = false;
                    break;
                }
            }
        }

        if (loseFloatMana || willDiscardNow || willDieNow) {
            // Will lose mana in pool or about to discard a card in cleanup or about to die in combat, so use this opportunity
            return true;
        } else if (isCommander && isMyMain1OrLater) {
            // Don't hold out specifically if this card is a commander, since otherwise it leads to stupid AI choices
            return true;
        } else if (wantToCastInMain1) {
            // Would rather cast it in Main 1 or as soon as possible anyway, so go for it
            return isMyMain1OrLater;
        } else if (hasAmbushAI && MyRandom.percentTrue(chanceToObeyAmbushAI, ai.getGame().getRandom())) {
            // Is an ambusher, so try to hold for declare blockers in combat where the AI defends, if possible
            return defOnlyAmbushAI && canCastAtOppTurn ? isOppDeclareAttackers : (isOppDeclareAttackers || isMyDeclareBlockers);
        } else if (valuableBlocker && isOppDeclareAttackers && MyRandom.percentTrue(chanceToAddBlocker, ai.getGame().getRandom())) {
            // Might serve as a valuable blocker in a combat where we are behind on untapped blockers
            return true;
        } else if (hasETBTrigger && MyRandom.percentTrue(chanceToCastForETB, ai.getGame().getRandom())) {
            // Instant speed is good when a card has an ETB trigger, but prolly don't cast in own turn before Main 1 not
            // to mana lock the AI or lose the chance to consider other options. Try to utilize it as a response to stack
            // if possible.
            return isMyMain1OrLater || isOppTurn || MyRandom.percentTrue(chanceToProcETBBeforeMain1, ai.getGame().getRandom());
        } else if (hasETBTrigger && canRespondToStack && MyRandom.percentTrue(chanceToRespondToStack, ai.getGame().getRandom())) {
            // Try to do something meaningful in response to an opposing effect on stack. Note that this is currently
            // too random to likely be meaningful, serious improvement might be needed.
            return canCastAtOppTurn || ph.getPlayerTurn().equals(ai);
        } else {
            // Doesn't have a ETB trigger and doesn't seem to be good as an ambusher, try to surprise the opp before my turn
            // TODO: maybe implement a way to reserve mana for this
            return canCastAtOppTurn ? isEOTBeforeMyTurn : isOwnEOT;
        }
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        if (!super.checkApiLogic(ai, sa)) {
            return false;
        }

        final Card card = sa.getHostCard();
        final ManaCost mana = card.getManaCost();
        final Game game = ai.getGame();

        /*
         * Checks if the creature will have non-positive toughness after
         * applying static effects. Exceptions: 1. has "etbCounter" keyword (eg.
         * Endless One) 2. paid non-zero for X cost 3. has ETB trigger 4. has
         * ETB replacement 5. has NoZeroToughnessAI svar (eg. Veteran Warleader)
         * 
         * 1. and 2. should probably be merged and applied on the card after
         * checking for effects like Doubling Season for getNetToughness to see
         * the true value. 3. currently allows the AI to suicide creatures as
         * long as it has an ETB. Maybe it should check if said ETB is actually
         * worth it. Not sure what 4. is for. 5. needs to be updated to ensure
         * that the net toughness is still positive after static effects.
         */
        // AiPlayDecision.WouldBecomeZeroToughnessCreature
        if (card.hasStartOfKeyword("etbCounter") || mana.countX() != 0
                || card.hasETBTrigger(false) || card.hasETBReplacement() || card.hasSVar("NoZeroToughnessAI")) {
                return true;
        }

        final Card copy = CardUtil.getLKICopy(card);
        ComputerUtilCard.applyStaticContPT(game, copy, null);
        if (copy.getNetToughness() > 0) {
            // Defer casting this creature if an opposing permanent can already
            // remove it (e.g. an Icatian Javelineers in play) AND the AI has
            // a removal spell in hand that could destroy that threat first.
            if (shouldDeferCreatureToHandleThreat(ai, sa, copy)) {
                return false;
            }
            // Don't drop creatures that can be repeatedly pinged off by an
            // opposing source (e.g. Prodigal Sorcerer, Triskelion, Pestilence)
            // unless the creature provides meaningful value on entry / cannot
            // realistically be held.
            if (shouldDeferDueToOpposingPingers(ai, sa, copy)) {
                return false;
            }
            // Defer non-artifact creatures while The Abyss is in play unless we
            // can swarm (deploy multiple this turn) so the recurring kill is
            // amortized. Otherwise we just feed the trigger one creature per turn.
            if (shouldDeferUnderAbyss(ai, sa, copy)) {
                return false;
            }
            return true;
        }

        return false;
    }

    /**
     * If The Abyss (or any permanent whose upkeep trigger destroys a target
     * nonartifact creature controlled by the active player) is on the
     * battlefield, hold non-artifact creatures unless we can swarm.
     */
    private static boolean shouldDeferUnderAbyss(final Player ai, final SpellAbility sa, final Card creatureLKI) {
        if (creatureLKI.isArtifact()) {
            return false;
        }
        boolean abyssInPlay = false;
        for (Card c : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            if (isAbyssLike(c)) {
                abyssInPlay = true;
                break;
            }
        }
        if (!abyssInPlay) {
            return false;
        }
        // Count non-artifact creatures we already control. The Abyss will kill
        // exactly one per upkeep, so each additional one we add is only worth
        // it if we're stacking multiple this turn (so the loss is amortized).
        int ownNonArtCreatures = 0;
        for (Card c : ai.getCreaturesInPlay()) {
            if (!c.isArtifact()) {
                ownNonArtCreatures++;
            }
        }
        // Count additional non-artifact creatures still in hand that the AI
        // could plausibly chain after this one to actually swarm.
        int otherNonArtInHand = 0;
        final Card self = sa.getHostCard();
        for (Card h : ai.getCardsIn(ZoneType.Hand)) {
            if (h.equals(self) || h.isLand() || h.isArtifact() || !h.isCreature()) {
                continue;
            }
            SpellAbility cast = h.getFirstSpellAbility();
            if (cast == null) {
                continue;
            }
            cast.setActivatingPlayer(ai, true);
            if (ComputerUtilCost.canPayCost(cast, ai, false)) {
                otherNonArtInHand++;
            }
        }
        // Already have at least one non-art creature out and no swarm follow-up
        // available -> just feeding The Abyss. Hold it.
        return ownNonArtCreatures >= 1 && otherNonArtInHand == 0;
    }

    private static boolean isAbyssLike(final Card c) {
        if ("The Abyss".equals(c.getName())) {
            return true;
        }
        for (forge.game.trigger.Trigger t : c.getTriggers()) {
            if (t.getMode() != forge.game.trigger.TriggerType.Phase) {
                continue;
            }
            if (!"Upkeep".equals(t.getParam("Phase"))) {
                continue;
            }
            String execName = t.getParam("Execute");
            if (execName == null) {
                continue;
            }
            String svar = c.getSVar(execName);
            if (svar == null) {
                continue;
            }
            if (svar.contains("DB$ Destroy")
                    && svar.contains("Creature.nonArtifact")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true when this creature would be killed by an existing opponent
     * permanent (via a damage activated ability or static damage effect) and
     * the AI has removal in hand that could neutralize that threat instead.
     * In that case we prefer to spend this priority pass on the removal so the
     * creature survives once it is finally cast.
     */
    private static boolean shouldDeferCreatureToHandleThreat(final Player ai, final SpellAbility sa, final Card creatureLKI) {
        final int toughness = creatureLKI.getNetToughness();
        if (toughness <= 0) {
            return false;
        }
        for (Player opp : ai.getOpponents()) {
            for (Card threat : opp.getCardsIn(ZoneType.Battlefield)) {
                Integer dmgFromThreat = damagePotentialAgainstAiCreature(threat);
                if (dmgFromThreat == null || dmgFromThreat < toughness) {
                    continue;
                }
                // This threat can kill our creature once cast; check if AI has
                // a removal spell in hand that can deal with the threat first.
                if (hasRemovalSpellInHandFor(ai, sa, threat)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any opposing permanent can repeatedly ping/damage this
     * creature for at least its toughness (e.g. Prodigal Sorcerer, Triskelion,
     * Pestilence) and the creature isn't worth feeding to it (no meaningful
     * ETB value). In that case we'd rather hold it.
     */
    private static boolean shouldDeferDueToOpposingPingers(final Player ai, final SpellAbility sa, final Card creatureLKI) {
        final int toughness = creatureLKI.getNetToughness();
        if (toughness <= 0) {
            return false;
        }
        // Creatures with meaningful ETB triggers may still be worth casting
        // even if they die immediately after.
        final Card actual = sa.getHostCard();
        if (actual.hasETBTrigger(true) || actual.hasETBReplacement()) {
            return false;
        }
        // If the creature has indestructible or protection from the likely
        // damage color it'll be fine anyway; let other checks handle it.
        if (actual.hasKeyword(Keyword.INDESTRUCTIBLE)) {
            return false;
        }
        for (Player opp : ai.getOpponents()) {
            for (Card threat : opp.getCardsIn(ZoneType.Battlefield)) {
                // Activated pingers (Prodigal Sorcerer / Triskelion / etc.)
                Integer dmgFromAbility = damagePotentialAgainstAiCreature(threat);
                if (dmgFromAbility != null && dmgFromAbility >= toughness) {
                    return true;
                }
                // Recurring upkeep damage-to-all sources (Pestilence-like).
                if (hasRecurringDamageToAllCreatures(threat, toughness)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Detects permanents with a recurring (upkeep/at-phase) ability that
     * damages all creatures (Pestilence, Earthquake-style enchantments, etc.)
     * for at least the given amount.
     */
    private static boolean hasRecurringDamageToAllCreatures(final Card threat, final int toughness) {
        // Check activated abilities that deal damage to each creature
        for (SpellAbility ab : threat.getSpellAbilities()) {
            if (ab.getApi() != ApiType.DamageAll || !ab.isAbility()) {
                continue;
            }
            String numDmg = ab.getParam("NumDmg");
            if (numDmg == null) {
                continue;
            }
            int dmg = AbilityUtils.calculateAmount(threat, numDmg, ab);
            String validCards = ab.getParamOrDefault("ValidCards", "");
            if (dmg >= toughness && (validCards.contains("Creature") || validCards.isEmpty())) {
                return true;
            }
        }
        // Pestilence-like triggers (deal damage during upkeep)
        for (forge.game.trigger.Trigger t : threat.getTriggers()) {
            if (t.getMode() != forge.game.trigger.TriggerType.Phase) {
                continue;
            }
            String execName = t.getParam("Execute");
            if (execName == null) {
                continue;
            }
            String svar = threat.getSVar(execName);
            if (svar == null) {
                continue;
            }
            if (svar.contains("DB$ DamageAll") && svar.contains("ValidCards$ Creature")) {
                return true;
            }
        }
        return false;
    }

    private static Integer damagePotentialAgainstAiCreature(final Card threat) {
        int best = -1;
        for (SpellAbility ab : threat.getSpellAbilities()) {
            if (ab.getApi() != ApiType.DealDamage || !ab.isAbility()) {
                continue;
            }
            String numDmg = ab.getParam("NumDmg");
            if (numDmg == null) {
                continue;
            }
            String validTgts = ab.getParam("ValidTgts");
            // Must be able to target a creature controlled by an opponent of the threat.
            if (validTgts != null
                    && !validTgts.contains("Any")
                    && !validTgts.contains("Creature")) {
                continue;
            }
            int dmg = AbilityUtils.calculateAmount(threat, numDmg, ab);
            if (dmg > best) {
                best = dmg;
            }
        }
        return best >= 0 ? best : null;
    }

    private static boolean hasRemovalSpellInHandFor(final Player ai, final SpellAbility currentSa, final Card threat) {
        final int threatToughness = Math.max(1, threat.getNetToughness());
        final Card currentSource = currentSa.getHostCard();
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.equals(currentSource) || c.isLand()) {
                continue;
            }
            for (SpellAbility ab : c.getSpellAbilities()) {
                if (!ab.isSpell() || ab.getApi() != ApiType.DealDamage) {
                    continue;
                }
                String numDmg = ab.getParam("NumDmg");
                if (numDmg == null) {
                    continue;
                }
                int abDmg = numDmg.equals("X")
                        ? ComputerUtilCost.getMaxXValue(ab, ai, false)
                        : AbilityUtils.calculateAmount(c, numDmg, ab);
                if (abDmg < threatToughness) {
                    continue;
                }
                ab.setActivatingPlayer(ai, true);
                if (ComputerUtilCost.canPayCost(ab, ai, false)) {
                    return true;
                }
            }
        }
        return false;
    }

}
