package neonique.cbcplugin_new.combat.tasks;

/*
public class VoidTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public VoidTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {
        if (!combatManager.voidEnabled()) return;
        Set<CBCPlayer> playerSet = gameManager.getAlivePlayers();
        for (CBCPlayer player : playerSet) {
            if (!player.isOnline()) continue;
            checkPlayerVoid(player);
        }
    }

    public void checkPlayerVoid (CBCPlayer player) {
        double voidPlane = combatManager.getVoidPlane();
        // Check if player is below void line
        if (player.getPlayer().getLocation().getY() < voidPlane) {
            if (combatManager.isVoidKill()) {
                combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.VOID, false);
            } else {
                player.getPlayer().teleport(combatManager.getVoidTeleport());
            }
        }
    }
}*/
