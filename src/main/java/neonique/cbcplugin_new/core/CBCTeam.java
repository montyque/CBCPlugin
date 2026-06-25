package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.CBCPlugin.getGameManager;

public abstract class CBCTeam<P extends CBCPlayer> implements TeamLike {

    // List of members
    private final HashMap<UUID, P> players = new HashMap<>();

    // Teams that will not be able to damage/kill players from this team
    private final Set<CBCTeam<P>> otherAlliedTeams = new HashSet<>();

    // Team data
    private final String id;
    private final String teamIdNum;
    private final String prefix;
    private final String name;
    private final TeamColor teamColor;
    private CBCScoreboardTeam scoreboardTeam;

    public CBCTeam (LobbyTeam lobbyTeam, String teamIdNum) {
        this.id = lobbyTeam.id();
        this.name = lobbyTeam.name();
        this.teamColor = lobbyTeam.teamColor();
        this.prefix = lobbyTeam.prefix();
        this.teamIdNum = teamIdNum;
    }

    public CBCTeam (String teamId, String teamIdNum, String teamName, TeamColor teamColor, String prefix) {
        this.id = teamId;
        this.name = teamName;
        this.teamColor = teamColor;
        this.prefix = prefix;
        this.teamIdNum = teamIdNum;
    }

    public void registerTeam (CBCScoreboardManager sbManager) {

        scoreboardTeam = sbManager.registerNewTeam(teamIdNum + id);
        scoreboardTeam.setSeeFriendlyInvisiblesEnabled(true);
        scoreboardTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);
        scoreboardTeam.setFriendlyFireEnabled(false); // Do not allow friendly fire
        scoreboardTeam.setColor(textColor()); // Set team color

        // If there's a team prefix, set it
        if (prefix != null) {
            scoreboardTeam.setPrefix(
                    Component.text(prefix + " ")
                            .color(textColor())
                            .decorate(TextDecoration.BOLD)
            );
        }
        sbManager.syncTeam(scoreboardTeam);

    }

    public Collection<P> getPlayers () {
        return players.values();
    }

    public Collection<P> getAlivePlayers () {
        return getPlayers().stream().filter(CBCPlayer::isAlive).collect(Collectors.toSet());
    }

    public Set<P> getOnlinePlayers () {
        return getPlayers().stream().filter(CBCPlayer::isOnline).collect(Collectors.toSet());
    }

    public String name () {
        return name;
    }

    public String id () {
        return id;
    }

    public TeamColor teamColor () {
        return teamColor;
    }

    public String getPrefix () {
        return prefix;
    }

    public CBCScoreboardTeam scoreboardTeam() {
        return scoreboardTeam;
    }

    public void addPlayer (P player) {
        players.put(player.getOfflinePlayer().getUniqueId(), player);
        scoreboardTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.setTeam(this);
    }

    public void removePlayer (P player) {
        players.remove(player.getOfflinePlayer().getUniqueId());
        scoreboardTeam.removeEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.setTeam(null);
    }

    public void removeTeam () {
        getGameManager().getCbcScoreboardManager().unregisterTeam(scoreboardTeam);
    }

    public void replacePlayerEntityKey(Player origin, Player newPlayer) {
        if (players.containsKey(origin.getUniqueId())) {
            P cbcPlayer = players.get(origin.getUniqueId());
            players.remove(origin.getUniqueId());
            players.put(newPlayer.getUniqueId(), cbcPlayer);
        }
    }

    public char getSpecialCrossbowChar() {
        return (char) ('\uE110' + getColorNumber());
    }

    public TrimMaterial getTrimMaterial () {
        return teamColor.trimMat();
    }

    public int getColorNumber() {
        return teamColor.num();
    }

    public char getSwordChar (boolean rightFacing) {
        if (rightFacing) {
            return ((char) (0xE440 + getColorNumber()));
        }
        else {
            return ((char) (0xE450 + getColorNumber()));
        }
    }

    public boolean isAlly (CBCPlayer player) {
        if (player.getTeam() == null) return false;
        if (player.getTeam() == this) return true;
        return otherAlliedTeams.contains(player.getTeam());

    }

    public void clearAlliedTeams() {
        otherAlliedTeams.clear();
    }

    public void addAlliedTeam (CBCTeam<P> team) {
        otherAlliedTeams.add(team);
    }

    public Component getTeamComponent(boolean withTeam) {
        String name = name();
        if (withTeam) {
            name += " Team";
        }
        return Component.text(name).color(textColor());
    }

    public void playGlobalSound(Sound sound, float volume, float pitch) {
        for (P player : getPlayers()) {
            if (player.isOnline()) {
                Player playerEntity = player.getPlayer();
                playerEntity.playSound(playerEntity.getLocation(),  sound, volume, pitch);
            }
        }
    }
}
