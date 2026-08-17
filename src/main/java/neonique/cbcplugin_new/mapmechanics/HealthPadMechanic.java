package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerSession;
import neonique.cbcplugin_new.core.PlayerStore;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class HealthPadMechanic implements MapMechanic {

    private final Collection<HealthPad> healthPads;

    private CombatContext combatContext;
    private BukkitRunnable updateTask;

    public HealthPadMechanic (Collection<HealthPad> healthPads) {
        this.healthPads = healthPads;
    }

    @Override
    public void activate (CombatContext combatContext) {
        this.combatContext = combatContext;
        enableAll();

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(combatContext.plugin(), 0, 1);
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
                healPad.playerCheck(combatContext.players());
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
