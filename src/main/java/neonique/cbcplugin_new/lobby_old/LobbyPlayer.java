package neonique.cbcplugin_new.lobby_old;

import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.UUID;

public class LobbyPlayer implements PlayerLike {

    private GameManager gameManager;

    private Lobby lobby;
    private UUID playerUUID;
    private LobbyTeam assignedTeam;
    private boolean spectator = false;

    public LobbyPlayer(GameManager gameManager, Lobby lobby, Player player) {
        this.gameManager = gameManager;
        this.lobby = lobby;
        this.playerUUID = player.getUniqueId();
        assignedTeam = null;
    }

    public void resetPlayer () {

        if (!isOnline()) return;
        Player entity = getPlayer();

        resetAllAttributes();

        for (PotionEffect effect : entity.getActivePotionEffects()) {
            entity.removePotionEffect(effect.getType());
        }

        entity.setGameMode(GameMode.ADVENTURE);
        entity.getInventory().clear();
        entity.updateInventory();
        entity.setHealth(20);
        entity.removeScoreboardTag("NVDisable");

    }

    public void resetAllAttributes () {

        Player playerEntity = getPlayer();
        if (playerEntity == null) return;
        AttributeInstance attr = playerEntity.getAttribute(Attribute.GENERIC_SCALE);
        if (attr != null) {
            attr.setBaseValue(1.0);
        }

    }

    public UUID uuid () {
        return playerUUID;
    }

    public void setSpectator (boolean b) {
        spectator = b;
    }

    public void playerJoinTeam(LobbyTeam team) { assignedTeam = team; }

    public void playerLeaveTeam() { assignedTeam = null; }

    public LobbyTeam getAssignedTeam() {
        return assignedTeam;
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setNewPlayer(Player newPlayer) {
        playerUUID = newPlayer.getUniqueId();
    }
}
