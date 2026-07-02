package forge.ai.ability;

import java.util.List;
import java.util.Map;

import com.google.common.base.Predicates;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CloneAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();

        boolean useAbility = true;

//        if (card.getController().isComputer()) {
//            final List<Card> creatures = AllZoneUtil.getCreaturesInPlay();
//            if (!creatures.isEmpty()) {
//                cardToCopy = CardFactoryUtil.getBestCreatureAI(creatures);
//            }
//        }

        // TODO - add some kind of check to answer
        // "Am I going to attack with this?"
        // TODO - add some kind of check for during human turn to answer
        // "Can I use this to block something?"

        if (!checkPhaseRestrictions(ai, sa, game.getPhaseHandler())) {
            return false;
        }

        PhaseHandler phase = game.getPhaseHandler();

        if (!sa.usesTargeting()) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= !c.isCreature() && !c.isTapped() && !(c.getTurnInZone() == phase.getTurn());

                // for creatures that could be improved (like Figure of Destiny)
                if (c.isCreature() && (!sa.hasParam("Duration") || (!c.isTapped() && !c.isSick()))) {
                    int power = -5;
                    if (sa.hasParam("Power")) {
                        power = AbilityUtils.calculateAmount(source, sa.getParam("Power"), sa);
                    }
                    int toughness = -5;
                    if (sa.hasParam("Toughness")) {
                        toughness = AbilityUtils.calculateAmount(source, sa.getParam("Toughness"), sa);
                    }
                    if ((power + toughness) > (c.getCurrentPower() + c.getCurrentToughness())) {
                        bFlag = true;
                    }
                }
            }

            if (!bFlag) { // All of the defined stuff is cloned, not very useful
                return false;
            }
        } else {
            sa.resetTargets();
            useAbility &= cloneTgtAI(sa);
        }

        return useAbility;
    } // end cloneCanPlayAI()

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        // AI should only activate this during Human's turn
        boolean chance = true;

        if (sa.usesTargeting()) {
            chance = cloneTgtAI(sa);
        }

        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        Card host = sa.getHostCard();
        boolean chance = true;

        if (sa.usesTargeting()) {
            chance = cloneTgtAI(sa);
        } else {
            if (sa.hasParam("Choices")) {
                CardCollectionView choices = CardLists.getValidCards(host.getGame().getCardsIn(ZoneType.Battlefield),
                        sa.getParam("Choices"), host.getController(), host, sa);

                chance = !choices.isEmpty();
            }
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with
        // not mandatory as part of the checks to cast something

        return chance || mandatory;
    }

    /**
     * <p>
     * cloneTgtAI.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return a boolean.
     */
    private boolean cloneTgtAI(final SpellAbility sa) {
        // Specific logic for cards
        if ("CloneAttacker".equals(sa.getParam("AILogic"))) {
            CardCollection valid = CardLists.getValidCards(sa.getHostCard().getController().getCardsIn(ZoneType.Battlefield), sa.getParam("ValidTgts"), sa.getHostCard().getController(), sa.getHostCard(), sa);
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(valid));
            return true;
        } else if ("CloneBestCreature".equals(sa.getParam("AILogic"))) {
            CardCollection valid = CardLists.getValidCards(sa.getHostCard().getController().getGame().getCardsIn(ZoneType.Battlefield), sa.getParam("ValidTgts"), sa.getHostCard().getController(), sa.getHostCard(), sa);
            sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(valid));
            return true;
        }

        // Default:
        // This is reasonable for now. Kamahl, Fist of Krosa and a sorcery or
        // two are the only things that clone a target. Those can just use
        // AI:RemoveDeck:All until this can do a reasonably good job of picking
        // a good target
        return false;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if (sa.hasParam("AILogic") && (!sa.usesTargeting() || sa.isTargetNumberValid())) {
            // Had a special logic for it and managed to target, so confirm if viable
            if ("CloneBestCreature".equals(sa.getParam("AILogic"))) {
                return ComputerUtilCard.evaluateCreature(sa.getTargetCard()) > ComputerUtilCard.evaluateCreature(sa.getHostCard());
            } else if ("IfDefinedCreatureIsBetter".equals(sa.getParam("AILogic"))) {
                List<Card> defined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
                Card bestDefined = ComputerUtilCard.getBestCreatureAI(defined);
                return ComputerUtilCard.evaluateCreature(bestDefined) > ComputerUtilCard.evaluateCreature(sa.getHostCard());
            }
        }

        // Currently doesn't confirm anything that's not defined by AI logic
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.ai.SpellAbilityAi#chooseSingleCard(forge.game.player.Player,
     * forge.game.spellability.SpellAbility, java.lang.Iterable, boolean,
     * forge.game.player.Player)
     */
    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        final Card host = sa.getHostCard();
        final String name = host.getName();
        final Player ctrl = host.getController();

        final Card cloneTarget = getCloneTarget(sa);
        final boolean isOpp = cloneTarget.getController().isOpponentOf(sa.getActivatingPlayer());

        final boolean isVesuva = "Vesuva".equals(name) || "Sculpting Steel".equals(name);
        final boolean canCloneLegendary = "True".equalsIgnoreCase(sa.getParam("NonLegendary"));

        String filter = !isVesuva ? "Permanent.YouDontCtrl,Permanent.nonLegendary"
                : "Permanent.YouDontCtrl+notnamed" + name + ",Permanent.nonLegendary+notnamed" + name;

        // TODO: rewrite this block so that this is done somehow more elegantly
        if (canCloneLegendary) {
            filter = filter.replace(".nonLegendary+", ".").replace(".nonLegendary", "");
        }

        CardCollection newOptions = CardLists.getValidCards(options, filter, ctrl, host, sa);
        if (!newOptions.isEmpty()) {
            options = newOptions;
        }

        if (sa.hasParam("AiChoiceLogic")) {
            final String logic = sa.getParam("AiChoiceLogic");
            if ("BestOppCtrl".equals(logic)) {
                options = CardLists.filterControlledBy(options, ctrl.getOpponents());
            }
        }

        // prevent loop of choosing copy of same card
        if (isVesuva) {
            options = CardLists.filter(options, Predicates.not(CardPredicates.sharesNameWith(host)));
        }

        // When the AI is going to control the clone, prefer artifacts that
        // strictly help us and avoid symmetric punisher artifacts like Ankh of
        // Mishra that would also hurt the AI.
        if (!isOpp) {
            Card preferred = pickBestArtifactForSelf(options, ai, "Copy Artifact".equals(name));
            if (preferred != null) {
                return preferred;
            }
            if ("Copy Artifact".equals(name)) {
                return null;
            }
        }

        Card choice = isOpp ? ComputerUtilCard.getWorstAI(options) : ComputerUtilCard.getBestAI(options);

        return choice;
    }

    /**
     * Curated artifact preference for self-controlled clones. Returns a
     * mana-acceleration / pure-value artifact when one is available, and
     * actively avoids symmetric punisher artifacts that would hurt the AI
     * just as much as they would hurt the opponent.
     */
    private static Card pickBestArtifactForSelf(final Iterable<Card> options, final Player ai, final boolean isCopyArtifact) {
        java.util.List<Card> filtered = CardLists.filter(options, new com.google.common.base.Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !isSymmetricPunisherArtifact(c);
            }
        });

        if (isCopyArtifact && !filtered.isEmpty()) {
            CardCollection nonManaArtifacts = CardLists.filter(filtered, new com.google.common.base.Predicate<Card>() {
                @Override
                public boolean apply(Card c) {
                    return !isPureManaArtifact(c);
                }
            });
            if (!nonManaArtifacts.isEmpty()) {
                return ComputerUtilCard.getBestAI(nonManaArtifacts);
            }
            if (!shouldCopyManaSourceWithCopyArtifact(ai)) {
                return null;
            }
        }

        // Tier-ordered preference list — highest tier first.
        final String[][] tiers = new String[][] {
                { "Black Lotus" },
                { "Sol Ring", "Mana Vault", "Mana Crypt" },
                { "Mox Sapphire", "Mox Jet", "Mox Ruby", "Mox Pearl", "Mox Emerald", "Mox Diamond", "Chrome Mox" },
                { "Lotus Petal", "Lion's Eye Diamond" },
                { "Skullclamp", "Sensei's Divining Top", "Winter Orb", "Howling Mine" },
        };
        for (String[] tier : tiers) {
            for (Card opt : filtered) {
                for (String name : tier) {
                    if (name.equals(opt.getName())) {
                        return opt;
                    }
                }
            }
        }
        if (!filtered.isEmpty()) {
            return ComputerUtilCard.getBestAI(filtered);
        }
        return null;
    }

    private static boolean isPureManaArtifact(final Card c) {
        return c.isArtifact() && !c.isCreature() && !ComputerUtilMana.getAIPlayableMana(c).isEmpty();
    }

    private static boolean shouldCopyManaSourceWithCopyArtifact(final Player ai) {
        int manaSources = ComputerUtilMana.getAvailableManaSources(ai, true).size();
        int spellCount = 0;
        int totalCmc = 0;
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.isLand() || "Copy Artifact".equals(c.getName())) {
                continue;
            }
            int cmc = c.getCMC();
            if (cmc <= 0) {
                continue;
            }
            if (cmc > manaSources) {
                return true;
            }
            spellCount++;
            totalCmc += cmc;
        }
        return spellCount >= 3 && totalCmc > manaSources + 2;
    }

    private static boolean isSymmetricPunisherArtifact(final Card c) {
        final String n = c.getName();
        return "Ankh of Mishra".equalsIgnoreCase(n)
                || "Manabarbs".equalsIgnoreCase(n)
                || "Sulfuric Vortex".equalsIgnoreCase(n)
                || "Black Vise".equalsIgnoreCase(n)
                || "Stormtide Leviathan".equalsIgnoreCase(n)
                || "The Rack".equalsIgnoreCase(n)
                || "Spike Weaver".equalsIgnoreCase(n);
    }

    protected Card getCloneTarget(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        Card tgtCard = host;
        if (sa.hasParam("CloneTarget")) {
            final List<Card> cloneTargets = AbilityUtils.getDefinedCards(host, sa.getParam("CloneTarget"), sa);
            if (!cloneTargets.isEmpty()) {
                tgtCard = cloneTargets.get(0);
            }
        } else if (sa.hasParam("Choices") && sa.usesTargeting()) {
            tgtCard = sa.getTargetCard();
        }

        return tgtCard;
    }

    /*
     * (non-Javadoc)
     * @see forge.ai.SpellAbilityAi#checkPhaseRestrictions(forge.game.player.Player, forge.game.spellability.SpellAbility, forge.game.phase.PhaseHandler)
     */
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        // don't use instant speed clone abilities outside computers
        // Combat_Begin step
        if (!ph.is(PhaseType.COMBAT_BEGIN)
                && ph.isPlayerTurn(ai) && !isSorcerySpeed(sa, ai)
                && !sa.hasParam("ActivationPhases") && sa.hasParam("Duration")) {
            return false;
        }

        // don't use instant speed clone abilities outside humans
        // Combat_Declare_Attackers_InstantAbility step
        if (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) || ph.isPlayerTurn(ai) || ph.getCombat().getAttackers().isEmpty()) {
            return false;
        }

        // don't activate during main2 unless this effect is permanent
        return !ph.is(PhaseType.MAIN2) || !sa.hasParam("Duration");
    }
}
