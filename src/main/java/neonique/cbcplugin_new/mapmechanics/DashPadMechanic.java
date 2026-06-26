package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class DashPadMechanic implements MapMechanic {

    private final Collection<DashPad> dashPads;

    private PlayerRegistry registry;
    private CombatManager combatManager;
    private BukkitRunnable updateTask;

    public DashPadMechanic (Collection<DashPad> dashPads) {
        this.dashPads = dashPads;
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
        for (DashPad dashPad : dashPads) {
            dashPad.updateCooldowns();
            Collection<CBCPlayer> onPad = dashPad.getPlayersOnPad(registry);
            onPad.forEach(dashPad::launchPlayer);
        }
    }

}
