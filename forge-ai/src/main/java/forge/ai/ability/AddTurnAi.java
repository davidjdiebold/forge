/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai.ability;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * <p>
 * AbilityFactory_Turns class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AddTurnAi extends SpellAbilityAi {

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
        Player opp = targetableOpps.min(PlayerPredicates.compareByLife());

        if (sa.usesTargeting()) {
            sa.resetTargets();
            if (sa.canTarget(ai) && (mandatory || !ai.getGame().getReplacementHandler().wouldExtraTurnBeSkipped(ai))) {
                sa.getTargets().add(ai);
            } else if (mandatory) {
            	for (final Player ally : ai.getAllies()) {
                    if (sa.canTarget(ally)) {
                    	sa.getTargets().add(ally);
                    	break;
                    }
            	}
                if (!sa.getTargetRestrictions().isMinTargetsChosen(sa.getHostCard(), sa) && opp != null) {
                    sa.getTargets().add(opp);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            final List<Player> tgtPlayers = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);
            for (final Player p : tgtPlayers) {
                if (p.isOpponentOf(ai) && !mandatory) {
                    return false;
                }
            }
            // TODO: improve ai for Sage of Hours
            return StringUtils.isNumeric(sa.getParam("NumTurns"));
            // not sure if the AI should be playing with cards that give the
            // Human more turns.
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // An extra-turn spell with no creatures on board is mostly wasted —
        // we just untap, draw and play a land. If the AI has a creature in
        // hand it can actually cast this turn, prefer to deploy that creature
        // first so the extra turn translates into real damage. Only defer
        // when we have neither board presence nor an opposing planeswalker
        // we can pressure.
        if (sa.isSpell() && sa.getActivatingPlayer() != null
                && sa.getActivatingPlayer().equals(aiPlayer)
                && aiPlayer.getCreaturesInPlay().isEmpty()
                && !hasOpposingPlaneswalker(aiPlayer)
                && hasCastableBoardDevelopingCardInHand(aiPlayer, sa)) {
            return false;
        }
        return doTriggerAINoCost(aiPlayer, sa, false);
    }

    private static boolean hasOpposingPlaneswalker(final Player ai) {
        for (Player opp : ai.getOpponents()) {
            for (Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                if (c.isPlaneswalker()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCastableBoardDevelopingCardInHand(final Player ai, final SpellAbility selfSa) {
        final Card selfHost = selfSa != null ? selfSa.getHostCard() : null;
        for (Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.equals(selfHost) || c.isLand()) {
                continue;
            }
            // Accept anything that develops the board: a creature, or a
            // permanent that produces / interacts with creatures (e.g.
            // The Hive's wasp generator, planeswalkers, build-around
            // enchantments). Pure instants/sorceries don't help an extra
            // turn so we ignore them here.
            if (!c.isPermanent()) {
                continue;
            }
            for (SpellAbility ability : c.getSpellAbilities()) {
                if (!ability.isSpell()) {
                    continue;
                }
                ability.setActivatingPlayer(ai, true);
                if (ComputerUtilCost.canPayCost(ability, ai, false)) {
                    return true;
                }
            }
        }
        return false;
    }

}
