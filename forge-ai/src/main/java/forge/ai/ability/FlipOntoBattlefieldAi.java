package forge.ai.ability;

import java.util.Map;

import com.google.common.base.Predicate;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class FlipOntoBattlefieldAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        PhaseHandler ph = sa.getHostCard().getGame().getPhaseHandler();
        String logic = sa.getParamOrDefault("AILogic", "");

        if (!isSorcerySpeed(sa, aiPlayer) && sa.getPayCosts().hasManaCost()) {
            // Default behavior was to only fire at end-of-turn so the AI waits
            // for the best opportunity. That made Chaos Orb-style cards useless
            // when a real threat (e.g. an opposing 4/7) had to be answered
            // during the AI's own turn. Allow activation during the AI's own
            // main phases as well, so we can remove threats proactively before
            // the opponent untaps and attacks.
            if (!ph.is(PhaseType.END_OF_TURN)
                    && !(ph.isPlayerTurn(aiPlayer)
                            && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)))) {
                return false;
            }
        }

        if ("DamageCreatures".equals(logic)) {
            int maxToughness = Integer.valueOf(sa.getSubAbility().getParam("NumDmg"));
            CardCollectionView rightToughness = CardLists.filter(aiPlayer.getOpponents().getCreaturesInPlay(), new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    return card.getNetToughness() <= maxToughness && card.canBeDestroyed();
                }
            });
            return !rightToughness.isEmpty();
        }

        // Only use Chaos Orb against high-value targets since it sacrifices itself
        CardCollectionView oppPerms = CardLists.filter(aiPlayer.getOpponents().getCardsIn(ZoneType.Battlefield),
                CardPredicates.Presets.CAN_BE_DESTROYED);
        // Look for non-land permanents or non-basic lands worth destroying
        CardCollectionView highValueTargets = CardLists.filter(oppPerms, new Predicate<Card>() {
            @Override
            public boolean apply(Card card) {
                return card.isCreature() || card.isPlaneswalker() || card.isArtifact()
                        || (card.isEnchantment() && !card.isAura())
                        || (card.isLand() && !card.isBasicLand());
            }
        });
        return !highValueTargets.isEmpty();
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
