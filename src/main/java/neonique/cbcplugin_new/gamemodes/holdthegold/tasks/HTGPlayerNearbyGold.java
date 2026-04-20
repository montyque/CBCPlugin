package neonique.cbcplugin_new.gamemodes.holdthegold.tasks;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class HTGPlayerNearbyGold extends BukkitRunnable {

    private final HTGGame game;

    public HTGPlayerNearbyGold (HTGGame game) {
        this.game = game;
    }

    @Override
    public void run () {

        // Check if game is over, and if so cancel
        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // Check if gold is not held by player
        if (game.isGoldHeld()) {
            game.getGoldHolder().setLastValidPosition();
            return;
        }

        if (game.getWinner() != null) return;

        // Check if gold armor stand exists
        if (game.getGoldArmorStand() == null) {
            game.summonGoldArmorStand(game.getMap().getGoldSpawn());
        }

        if (game.getGoldArmorStand().isDead()) {
            game.summonGoldArmorStand(game.getMap().getGoldSpawn());
        }

        Location goldLocation = game.getGoldLocation();

        // Check for all players that are nearby
        HTGPlayer nearestPlayer = null;
        for (Player playerEntity : goldLocation.getNearbyPlayers(1.5)) {

            if (game.getPlayer(playerEntity) == null) continue;
            HTGPlayer player = game.getPlayer(playerEntity);
            if (!player.isAlive()) continue;
            if (!player.isOnline()) continue;

            if (nearestPlayer != null) {
                if (goldLocation.distanceSquared(playerEntity.getLocation()) > goldLocation.distanceSquared(nearestPlayer.getPlayer().getLocation())) {
                    continue;
                }
            }
            nearestPlayer = player;
        }

        // Pickup gold if a player within the range exists
        if (nearestPlayer != null) {
            game.playerPickupGold(nearestPlayer);
        }
    }
}
