package handlers.effecthandlers;

import org.l2j.gameserver.engine.skill.api.Skill;
import org.l2j.gameserver.engine.skill.api.SkillEffectFactory;
import org.l2j.gameserver.enums.ShotType;
import org.l2j.gameserver.model.StatsSet;
import org.l2j.gameserver.model.actor.Attackable;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.effects.AbstractEffect;
import org.l2j.gameserver.model.effects.EffectType;
import org.l2j.gameserver.model.items.instance.Item;
import org.l2j.gameserver.model.stats.Formulas;
import org.l2j.gameserver.model.stats.Stat;

import static org.l2j.gameserver.util.GameUtils.isAttackable;
import static org.l2j.gameserver.util.GameUtils.isPlayer;

/**
 * Physical Attack effect implementation. <br>
 * Current formulas were tested to be the best matching retail, damage appears to be identical: <br>
 * For melee skills: 70 * graciaSkillBonus1.10113 * (patk * lvlmod + power) * crit * ss * skillpowerbonus / pdef <br>
 * For ranged skills: 70 * (patk * lvlmod + power + patk + power) * crit * ss * skillpower / pdef <br>
 * @author Nik
 * @author JoeAlisson
 */
public final class PhysicalAttack extends AbstractEffect {
	private final double power;
	private final double pAtkMod;
	private final double pDefMod;
	private final double criticalChance;
	private final boolean ignoreShieldDefence;
	private final boolean overHit;

	private PhysicalAttack(StatsSet params) {
		power = params.getDouble("power", 0);
		pAtkMod = params.getDouble("attack-mod", 1.0);
		pDefMod = params.getDouble("defense-mod", 1.0);
		criticalChance = params.getDouble("critical-chance", 0);
		ignoreShieldDefence = params.getBoolean("ignore-shield", false);
		overHit = params.getBoolean("over-hit", false);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcPhysicalSkillEvasion(effector, effected, skill);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item) {
		if (effector.isAlikeDead()) {
			return;
		}
		
		if (isPlayer(effected) && effected.getActingPlayer().isFakeDeath()) {
			effected.stopFakeDeath(true);
		}
		
		if (overHit && isAttackable(effected)) {
			((Attackable) effected).overhitEnabled(true);
		}
		
		final double attack = effector.getPAtk() * pAtkMod;
		double defence = effected.getPDef() * pDefMod;
		
		if (!ignoreShieldDefence) {
			switch (Formulas.calcShldUse(effector, effected)) {
				case Formulas.SHIELD_DEFENSE_SUCCEED -> defence += effected.getShldDef();
				case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK -> defence = -1;
			}
		}
		
		double damage = 1;
		final boolean critical = Formulas.calcCrit(criticalChance, effected, effector, skill);
		
		if (defence != -1) {
			// Trait, elements
			final double weaponTraitMod = Formulas.calcWeaponTraitBonus(effector, effected);
			final double generalTraitMod = Formulas.calcGeneralTraitBonus(effector, effected, skill.getTrait(), true);
			final double weaknessMod = Formulas.calcWeaknessBonus(effector, effected, skill.getTrait());
			final double attributeMod = Formulas.calcAttributeBonus(effector, effected, skill);
			final double pvpPveMod = Formulas.calculatePvpPveBonus(effector, effected, skill, true);
			final double randomMod = effector.getRandomDamageMultiplier();
			
			// Skill specific mods.
			final double weaponMod = effector.getAttackType().isRanged() ? 70 : 77;
			final double power = this.power + effector.getStats().getValue(Stat.SKILL_POWER_ADD, 0);
			final double rangedBonus = effector.getAttackType().isRanged() ? attack + power : 0;
			final double critMod = critical ? Formulas.calcCritDamage(effector, effected, skill) : 1;
			double ssmod = 1;
			if (skill.useSoulShot()) {
				if (effector.isChargedShot(ShotType.SOULSHOTS))
				{
					ssmod = 2 * effector.getStats().getValue(Stat.SOUL_SHOTS_BONUS); // 2.04 for dual weapon?
				}
				else if (effector.isChargedShot(ShotType.BLESSED_SOULSHOTS))
				{
					ssmod = 4 * effector.getStats().getValue(Stat.SOUL_SHOTS_BONUS);
				}
			}
			
			// ...................____________Melee Damage_____________......................................___________________Ranged Damage____________________
			// ATTACK CALCULATION 77 * ((pAtk * lvlMod) + power) / pdef            RANGED ATTACK CALCULATION 70 * ((pAtk * lvlMod) + power + patk + power) / pdef
			// ```````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^``````````````````````````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
			final double baseMod = (weaponMod * ((attack * effector.getLevelMod()) + power + rangedBonus)) / defence;
			damage = baseMod  * ssmod * critMod * weaponTraitMod * generalTraitMod * weaknessMod * attributeMod * pvpPveMod * randomMod;
			damage *= effector.getStats().getValue(Stat.PHYSICAL_SKILL_POWER, 1);
		}
		
		effector.doAttack(damage, effected, skill, false, false, critical, false);
	}

	public static class Factory implements SkillEffectFactory {

		@Override
		public AbstractEffect create(StatsSet data) {
			return new PhysicalAttack(data);
		}

		@Override
		public String effectName() {
			return "physical-attack";
		}
	}
}
