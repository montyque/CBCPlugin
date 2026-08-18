package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class VoidMechanic implements MapMechanic {

    private CombatContext combatContext;
    private BukkitRunnable updateTask;

    private final Location teleportLocation;
    private final double voidPlaneHeight;

    private boolean killOnVoid = true;

    public VoidMechanic (Location teleportLocation, double voidPlaneHeight) {
        this.teleportLocation = teleportLocation;
        this.voidPlaneHeight = voidPlaneHeight;
    }

    @Override
    public void activate (CombatContext combatContext) {
        this.combatContext = combatContext;

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 1);

    }

    @Override
    public void deactivate() {
        updateTask.cancel();
    }

    public void update () {
        for (CBCPlayer player : combatContext.players().players()) {
            if (!player.isOnline()) continue;
            if (!player.isAlive()) continue;
            if (player.getPlayer().getLocation().getY() < voidPlaneHeight) {
                if (killOnVoid) {
                    combatContext.playerDeath(player, DeathCause.VOID);
                } else {
                    player.getPlayer().teleport(teleportLocation);
                }
            }
        }
    }

    public void setKillOnVoid (boolean b) {
        this.killOnVoid = b;
    }

}