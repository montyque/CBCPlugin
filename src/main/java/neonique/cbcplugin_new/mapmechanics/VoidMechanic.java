package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class VoidMechanic implements MapMechanic {

    private PlayerRegistry registry;
    private CombatManager combatManager;
    private BukkitRunnable updateTask;

    private final Location teleportLocation;
    private final double voidPlaneHeight;

    private boolean killOnVoid = true;

    public VoidMechanic (Location teleportLocation) {
        this(teleportLocation, -64);
    }

    public VoidMechanic (Location teleportLocation, double voidPlaneHeight) {
        this.teleportLocation = teleportLocation;
        this.voidPlaneHeight = voidPlaneHeight;
    }

    @Override
    public void activate (PlayerRegistry registry, CombatManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;

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
        for (CBCPlayer player : registry.getPlayers()) {
            if (!player.isAlive()) continue;
            if (player.getPlayer().getLocation().getY() < voidPlaneHeight) {
                if (killOnVoid) {
                    combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.VOID, false);
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