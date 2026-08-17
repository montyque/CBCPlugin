package neonique.cbcplugin_new.combat.tasks;

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
