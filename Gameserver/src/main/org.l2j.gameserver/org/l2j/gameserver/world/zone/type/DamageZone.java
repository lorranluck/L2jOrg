package org.l2j.gameserver.world.zone.type;

import org.l2j.commons.threading.ThreadPool;
import org.l2j.gameserver.enums.InstanceType;
import org.l2j.gameserver.instancemanager.CastleManager;
import org.l2j.gameserver.model.actor.Creature;
import org.l2j.gameserver.model.actor.instance.Player;
import org.l2j.gameserver.model.entity.Castle;
import org.l2j.gameserver.model.stats.Stats;
import org.l2j.gameserver.world.zone.AbstractZoneSettings;
import org.l2j.gameserver.world.zone.TaskZoneSettings;
import org.l2j.gameserver.world.zone.Zone;
import org.l2j.gameserver.world.zone.ZoneManager;

import static java.util.Objects.*;
import static org.l2j.gameserver.util.GameUtils.isPlayer;

/**
 * A damage zone
 *
 * @author durgus
 * @author JoeAlisson
 */
public class DamageZone extends Zone {
    private int damageHPPerSec;
    private int damageMPPerSec;

    private int castleId;
    private Castle castle;

    private int startTask;
    private int reuseTask;

    public DamageZone(int id) {
        super(id);

        // Setup default damage
        damageHPPerSec = 200;

        // Setup default start / reuse time
        startTask = 10;
        reuseTask = 5000;

        setTargetType(InstanceType.Playable); // default only playabale
        AbstractZoneSettings settings = requireNonNullElseGet(ZoneManager.getSettings(getName()), TaskZoneSettings::new);
        setSettings(settings);
    }

    @Override
    public TaskZoneSettings getSettings() {
        return (TaskZoneSettings) super.getSettings();
    }

    @Override
    public void setParameter(String name, String value) {
        if (name.equals("dmgHPSec")) {
            damageHPPerSec = Integer.parseInt(value);
        } else if (name.equals("dmgMPSec")) {
            damageMPPerSec = Integer.parseInt(value);
        } else if (name.equals("castleId")) {
            castleId = Integer.parseInt(value);
        } else if (name.equalsIgnoreCase("initialDelay")) {
            startTask = Integer.parseInt(value);
        } else if (name.equalsIgnoreCase("reuse")) {
            reuseTask = Integer.parseInt(value);
        } else {
            super.setParameter(name, value);
        }
    }

    @Override
    protected void onEnter(Creature creature) {
        if ((getSettings().getTask() == null) && ((damageHPPerSec != 0) || (damageMPPerSec != 0))) {
            final Player player = creature.getActingPlayer();
            if (getCastle() != null) // Castle zone
            {
                if (!(getCastle().getSiege().isInProgress() && (player != null) && (player.getSiegeState() != 2))) // Siege and no defender
                {
                    return;
                }
            }

            synchronized (this) {
                if (getSettings().getTask() == null) {
                    getSettings().setTask(ThreadPool.scheduleAtFixedRate(new ApplyDamage(), startTask, reuseTask));
                }
            }
        }
    }

    @Override
    protected void onExit(Creature character) {
        if (creatures.isEmpty() && (getSettings().getTask() != null)) {
            getSettings().clear();
        }
    }

    protected Castle getCastle() {
        if ((castleId > 0) && (castle == null)) {
            castle = CastleManager.getInstance().getCastleById(castleId);
        }

        return castle;
    }

    private final class ApplyDamage implements Runnable {

        @Override
        public void run() {
            if (!isEnabled()) {
                return;
            }

            if (nonNull(castle)) {
                if (!castle.getSiege().isInProgress()) {
                    getSettings().clear();
                    return;
                }
            }

            forEachCreature(this::doDamage, this::canReceiveDamage);
        }

        private boolean canReceiveDamage(Creature creature) {
            if(creature.isDead()) {
                return false;
            }

            return !isPlayer(creature) || !((Player) creature).isInSiege() || ((Player) creature).getSiegeState() != 2;
        }

        private void doDamage(Creature creature) {
            final double multiplier = 1 + (creature.getStat().getValue(Stats.DAMAGE_ZONE_VULN, 0) / 100);

            if (damageHPPerSec != 0) {
                creature.reduceCurrentHp(damageHPPerSec * multiplier, creature, null);
            }
            if (damageMPPerSec != 0) {
                creature.reduceCurrentMp(damageMPPerSec * multiplier);
            }
        }
    }
}
