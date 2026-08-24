package neonique.cbcplugin_new.lobby_old;

import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.*;

// This class is used for storing the players in a team in the lobby
public class LobbyTeam implements TeamLike {

    private final GameManager gameManager;
    private final Lobby lobby;

    // Team information
    private final String teamIdNum;
    private final String id;
    private final String name;
    private final String prefix;
    private final TeamColor teamColor;

    private final CBCScoreboardTeam scoreboardTeam;

    private final HashMap<UUID, LobbyPlayer> playersInTeam = new HashMap<>();

    public LobbyTeam(GameManager gameManager, Lobby lobby, String teamIdNum, String id,
                     String name, String prefix, TeamColor teamColor) {

        this.gameManager = gameManager;
        this.lobby = lobby;

        this.teamIdNum = teamIdNum;
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.teamColor = teamColor;
        scoreboardTeam = registerTeam();

    }

    public CBCScoreboardTeam registerTeam () {
        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();
        CBCScoreboardTeam team = scoreboardManager.registerNewTeam(teamIdNum + id + "Lobby");
        team.setFriendlyFireEnabled(true);
        team.setPrefix(Component.text(" ■ ").color(textColor()));
        return team;
    }

    public void addPlayer(LobbyPlayer player) {
        playersInTeam.put(player.getOfflinePlayer().getUniqueId(), player);
        scoreboardTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.playerJoinTeam(this);
    }

    public Collection<LobbyPlayer> getPlayers() {
        return playersInTeam.values();
    }

    public Set<LobbyPlayer> getOnlinePlayers() {
        Set<LobbyPlayer> onlinePlayers = new HashSet<>();
        for (LobbyPlayer player : playersInTeam.values()) {
            if (player.isOnline()) onlinePlayers.add(player);
        }
        return onlinePlayers;
    }

    public boolean isPlayerInTeam(LobbyPlayer player) {
        return playersInTeam.containsValue(player);
    }

    public boolean isPlayerEntityInTeam(Player player) {
        return playersInTeam.containsKey(player.getUniqueId());
    }

    public void removePlayer(LobbyPlayer player) {
        if (!isPlayerInTeam(player)) return;
        playersInTeam.remove(player.getOfflinePlayer().getUniqueId());
        scoreboardTeam.removeEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.playerLeaveTeam();
    }

    public void removeTeam() {
        gameManager.getCbcScoreboardManager().unregisterTeam(scoreboardTeam);
    }

    public String id () {
        return id;
    }

    public String name () {
        return name;
    }

    public TeamColor teamColor () {
        return teamColor;
    }

    public String prefix () {
        return prefix;
    }

    public Collection<? extends PlayerLike> players () {
        return playersInTeam.values();
    }

}
