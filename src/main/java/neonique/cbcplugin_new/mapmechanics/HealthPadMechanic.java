package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class HealthPadMechanic implements MapMechanic {

    private final Collection<HealthPad> healthPads;

    private PlayerRegistry registry;
    private CombatManager combatManager;
    private BukkitRunnable updateTask;

    public HealthPadMechanic (Collection<HealthPad> healthPads) {
        this.healthPads = healthPads;
    }

    @Override
    public void activate (PlayerRegistry registry, CombatManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;
        enableAll();

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
        disableAll();
        updateTask.cancel();
    }

    public void update () {
        for (HealthPad healPad : healthPads) {
            if (!healPad.isEnabled()) continue;
            if (healPad.isOnline()) {
                healPad.playParticles();
                healPad.playerCheck(registry);
            } else {
                healPad.decrementTimer();
            }
        }
    }

    public void enableAll () {
        healthPads.forEach(h -> h.enable(true));
    }

    public void disableAll () {
        healthPads.forEach(HealthPad::disable);
    }

}
