package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LobbyPlayer {

    GameManager gameManager;

    Lobby lobby;
    UUID playerUUID;

    LobbyTeam assignedTeam;
    boolean spectator = false;

    public LobbyPlayer(GameManager gameManager, Lobby lobby, UUID playerUUID) {
        this.gameManager = gameManager;
        this.lobby = lobby;
        this.playerUUID = playerUUID;

        assignedTeam = null;
    }

    public void resetAllAttributes () {
        Player playerEntity = getPlayer();
        if (playerEntity == null) return;

        AttributeInstance attr = playerEntity.getAttribute(Attribute.GENERIC_SCALE);
        if (attr != null) {
            attr.setBaseValue(1.0);
        }
    }

    public String getName() {
        return getOfflinePlayer().getName();
    }

    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    public Player getPlayer() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player.getPlayer() != null) {
            return player.getPlayer();
        } else {
            return null;
        }
    }

    public boolean isOnline() {
        return getOfflinePlayer().isOnline();
    }

    public void setSpectator() {
        spectator = true;
    }

    public void setNoSpectator() {
        spectator = false;
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
