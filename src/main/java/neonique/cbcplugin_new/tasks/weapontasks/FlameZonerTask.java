package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FlameZonerTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public FlameZonerTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        Set<CBCPlayer> playerSet = gameManager.getAlivePlayers();

        for (CBCPlayer player : playerSet) {

            if (player.isImmune()) continue;
            if (!player.isOnline()) continue;
            Player playerEntity = player.getPlayer();

            Location playerLocation = playerEntity.getLocation();

            // Get a list of players nearby
            Collection<Arrow> arrowsNearby = playerLocation.getNearbyEntitiesByType(Arrow.class, combatManager.getFlameRadius());
            List<Arrow> enemyFlameArrowsNearby = new ArrayList<>();

            // Check if there are actually any flame zoner arrows nearby
            if (arrowsNearby.isEmpty()) {
                // Set player to no flame zoner
                player.setFlameZonerDamageSource(null);
                continue;
            }

            // Check for each arrow if they are an enemy flame zoner arrow
            for (Arrow arrow : arrowsNearby) {
                // Check if arrow is a flame zone arrow
                if (arrow.getScoreboardTags().contains("flameArrow")) {
                    if (!player.isEntityAlly(arrow)) {
                        enemyFlameArrowsNearby.add(arrow);
                    }
                }
            }

            // Give player damage effect if in flame zone
            if (enemyFlameArrowsNearby.size() > 0) {

                CBCPlayer playerDamager;
                CBCPlayer currentPlayerDamager = player.getInFlameZoneOfPlayer();

                // Check if multiple flame zoners nearby
                if (enemyFlameArrowsNearby.size() >= 2) {

                    double lowestDistSqrdToPlayer = 100000;
                    CBCPlayer damager = null;

                    for (Arrow flameArrow : enemyFlameArrowsNearby) {
                        CBCPlayer arrowPlayer = getShooterOfArrow(flameArrow);

                        if (arrowPlayer == null) continue;

                        double arrowDistSqrdToPlayer = playerLocation.distanceSquared(flameArrow.getLocation());
                        if (arrowDistSqrdToPlayer < lowestDistSqrdToPlayer) {
                            lowestDistSqrdToPlayer = arrowDistSqrdToPlayer;
                            damager = arrowPlayer;
                        }

                        if (arrowPlayer == currentPlayerDamager) {
                            damager = arrowPlayer;
                            break;
                        }
                    }
                    playerDamager = damager;
                }
                else {
                    playerDamager = getShooterOfArrow(enemyFlameArrowsNearby.get(0));
                }

                if (playerDamager != null) {

                    boolean inFlameZonerBefore = player.isInFlameZoner();
                    player.setFlameZonerDamageSource(playerDamager);

                    if (!inFlameZonerBefore && player.flamezoneFireTicks == 0) {
                        player.flamezoneFireTicks = 1;
                        FlameZonerDamageTask fzDamageTask = new FlameZonerDamageTask(gameManager, combatManager, player);
                        fzDamageTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 10L);
                    }
                }
            } else {
                player.setFlameZonerDamageSource(null);
            }
        }
    }

    public CBCPlayer getShooterOfArrow (Arrow flameArrow) {
        ProjectileSource source = flameArrow.getShooter();
        if (source instanceof Player arrowFiredBy) {
            // Check if arrow fired by player
            return gameManager.getPlayer(arrowFiredBy);
        }
        else {
            return null;
        }
    }
}
