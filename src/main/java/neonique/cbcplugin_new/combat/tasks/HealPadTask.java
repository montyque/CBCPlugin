package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.mapmechanics.HealthPad;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/*
public class HealPadTask extends BukkitRunnable {

    private final CombatManager combatManager;
    private final PlayerRegistry playerRegistry;

    public HealPadTask(CombatManager combatManager, PlayerRegistry playerRegistry) {
        this.combatManager = combatManager;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void run() {
        for (HealthPad healPad : combatManager.getHealthPadList()) {
            if (!healPad.isEnabled()) continue;
            if (healPad.isOnline()) {
                healPad.playParticles();
                healPad.playerCheck(playerRegistry);
            } else {
                healPad.decrementTimer();
            }
        }
    }

}*/
