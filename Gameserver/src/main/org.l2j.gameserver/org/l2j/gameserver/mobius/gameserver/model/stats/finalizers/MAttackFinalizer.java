/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2j.gameserver.mobius.gameserver.model.stats.finalizers;

import com.l2jmobius.Config;
import org.l2j.gameserver.mobius.gameserver.model.actor.L2Character;
import org.l2j.gameserver.mobius.gameserver.model.items.L2Item;
import org.l2j.gameserver.mobius.gameserver.model.stats.BaseStats;
import org.l2j.gameserver.mobius.gameserver.model.stats.IStatsFunction;
import org.l2j.gameserver.mobius.gameserver.model.stats.Stats;

import java.util.Optional;

/**
 * @author UnAfraid
 */
public class MAttackFinalizer implements IStatsFunction {
    @Override
    public double calc(L2Character creature, Optional<Double> base, Stats stat) {
        throwIfPresent(base);

        double baseValue = calcWeaponBaseValue(creature, stat);
        baseValue += calcEnchantedItemBonus(creature, stat);
        if (creature.isPlayer()) {
            // Enchanted chest bonus
            baseValue += calcEnchantBodyPart(creature, L2Item.SLOT_CHEST, L2Item.SLOT_FULL_ARMOR);
        }

        if (Config.CHAMPION_ENABLE && creature.isChampion()) {
            baseValue *= Config.CHAMPION_ATK;
        }
        if (creature.isRaid()) {
            baseValue *= Config.RAID_MATTACK_MULTIPLIER;
        }

        // Calculate modifiers Magic Attack
        final double intBonus = BaseStats.INT.calcBonus(creature);
        baseValue *= Math.pow(intBonus, 2) * Math.pow(creature.getLevelMod(), 2);
        return Math.min(Stats.defaultValue(creature, stat, baseValue), Config.MAX_MATK);
    }

    @Override
    public double calcEnchantBodyPartBonus(int enchantLevel, boolean isBlessed) {
        if (isBlessed) {
            return (2 * Math.max(enchantLevel - 3, 0)) + (2 * Math.max(enchantLevel - 6, 0));
        }

        return (1.4 * Math.max(enchantLevel - 3, 0)) + (1.4 * Math.max(enchantLevel - 6, 0));
    }
}