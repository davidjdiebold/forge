package forge.ai.ability;

import forge.ai.ComputerUtilAbility;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.game.keyword.Keyword;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends PermanentAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if ("PithingNeedle".equals(aiLogic)) {
            // Make sure theres something in play worth Needlings.
            // Planeswalker or equipment or something

            CardCollection oppPerms = CardLists.getValidCards(ai.getOpponents().getCardsIn(ZoneType.Battlefield), "Card.OppCtrl+hasNonManaActivatedAbility", ai, sa.getHostCard(), sa);
            if (oppPerms.isEmpty()) {
                return false;
            }

            Card card = ComputerUtilCard.getBestPlaneswalkerAI(oppPerms);
            if (card != null) {
                return true;
            }

            // 5 percent chance to cast per opposing card with a non mana ability
            return MyRandom.getRandom().nextFloat() <= .05 * oppPerms.size();
        }

        return super.checkAiLogic(ai, sa, aiLogic);
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!super.checkApiLogic(ai, sa))
            return false;

        final Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        if ("Mana Vault".equals(sourceName) && shouldHoldManaVault(ai, sa)) {
            return false;
        }

        if ("The Abyss".equals(sourceName) && shouldAvoidCastingTheAbyss(ai)) {
            return false;
        }

        // Check for valid targets before casting
        if (host.hasSVar("OblivionRing")) {
            SpellAbility effectExile = AbilityFactory.getAbility(host.getSVar("TrigExile"), host);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParamOrDefault("Origin", "Battlefield")).get(0);
            effectExile.setActivatingPlayer(ai, true);
            CardCollection targets = CardLists.getTargetableCards(game.getCardsIn(origin), effectExile);
            if (sourceName.equals("Suspension Field") 
                    || sourceName.equals("Detention Sphere")) {
                // existing "exile until leaves" enchantments only target opponent's permanents
                // TODO: consider replacing the condition with host.hasSVar("OblivionRing")
                targets = CardLists.filterControlledBy(targets, ai.getOpponents());
            }
            // AiPlayDecision.AnotherTime
            return !targets.isEmpty();
        }
        return true;
    }

    private static boolean shouldHoldManaVault(final Player ai, final SpellAbility sa) {
        final int availableMana = ComputerUtilMana.getAvailableManaSources(ai, true).size();
        final int manaAfterVault = availableMana + 2;
        boolean unlocksDefensivePayoff = false;

        for (Card handCard : ai.getCardsIn(ZoneType.Hand)) {
            if (handCard.equals(sa.getHostCard()) || handCard.isLand()) {
                continue;
            }
            final SpellAbility cast = handCard.getFirstSpellAbility();
            if (cast == null || cast.getPayCosts() == null || cast.getPayCosts().getTotalMana() == null) {
                continue;
            }

            final int cmc = cast.getPayCosts().getTotalMana().getCMC();
            if (cmc <= availableMana || cmc > manaAfterVault) {
                continue;
            }

            if (isManaVaultThreatPayoff(handCard)) {
                return false;
            }
            if (isManaVaultDefensivePayoff(handCard)) {
                unlocksDefensivePayoff = true;
            }
        }

        return !unlocksDefensivePayoff || !isUnderDangerousCreaturePressure(ai);
    }

    private static boolean isManaVaultThreatPayoff(final Card card) {
        return card.isCreature() || card.isPlaneswalker();
    }

    private static boolean isManaVaultDefensivePayoff(final Card card) {
        final String name = card.getName();
        return "The Abyss".equals(name) || "Icy Manipulator".equals(name);
    }

    private static boolean isUnderDangerousCreaturePressure(final Player ai) {
        for (Player opp : ai.getOpponents()) {
            for (Card c : opp.getCreaturesInPlay()) {
                final int power = c.getNetPower();
                final boolean evasion = c.hasKeyword(Keyword.FLYING)
                        || c.hasKeyword(Keyword.SHADOW)
                        || c.hasKeyword(Keyword.FEAR)
                        || c.hasKeyword(Keyword.HORSEMANSHIP)
                        || c.hasKeyword(Keyword.SKULK)
                        || c.hasKeyword(Keyword.INTIMIDATE)
                        || c.hasKeyword(Keyword.TRAMPLE)
                        || c.hasKeyword(Keyword.MENACE);
                if (power >= 4 || (power >= 3 && evasion)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldAvoidCastingTheAbyss(final Player ai) {
        boolean aiHasVulnerableCreature = false;
        for (Card c : ai.getCreaturesInPlay()) {
            if (!c.isArtifact()) {
                aiHasVulnerableCreature = true;
                break;
            }
        }
        if (!aiHasVulnerableCreature) {
            return false;
        }

        for (Player opp : ai.getOpponents()) {
            for (Card c : opp.getCreaturesInPlay()) {
                if (!c.isArtifact()) {
                    return false;
                }
            }
        }
        return true;
    }
}
