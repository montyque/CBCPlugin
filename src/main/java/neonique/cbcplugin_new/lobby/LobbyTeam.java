package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import net.kyori.adventure.text.Component;

import java.util.*;

// This class is used for storing the players in a team in the lobby
public class LobbyTeam implements TeamLike {

    // Team information
    private final String teamIdNum;
    private final String id;
    private final String name;
    private final String prefix;
    private final TeamColor teamColor;

    private CBCScoreboardTeam scoreboardTeam;
    private final Map<UUID, LobbyPlayer> playersInTeam = new HashMap<>();

    public LobbyTeam(String teamIdNum, String id, String name, String prefix, TeamColor teamColor) {

        this.teamIdNum = teamIdNum;
        this.id = id;
        this.name = name;
        this.prefix = prefix;
        this.teamColor = teamColor;

    }

    public void registerTeam (CBCScoreboardManager scoreboardManager) {
        scoreboardTeam = scoreboardManager.registerNewTeam(teamIdNum + id + "Lobby");
        scoreboardTeam.setFriendlyFireEnabled(true);
        scoreboardTeam.setPrefix(Component.text(" ■ ").color(textColor()));
    }

    public void removeTeam() {
        scoreboardTeam.unregister();
    }

    public void addPlayer(LobbyPlayer player) {
        playersInTeam.put(player.getOfflinePlayer().getUniqueId(), player);
        scoreboardTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.playerJoinTeam(this);
    }

    public boolean isPlayerInTeam(LobbyPlayer player) {
        return playersInTeam.containsValue(player);
    }

    public void removePlayer(LobbyPlayer player) {
        if (!isPlayerInTeam(player)) return;
        playersInTeam.remove(player.getOfflinePlayer().getUniqueId());
        scoreboardTeam.removeEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.playerLeaveTeam();
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
