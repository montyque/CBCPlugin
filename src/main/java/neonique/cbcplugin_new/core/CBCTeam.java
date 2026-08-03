package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class CBCTeam<P extends CBCPlayer> implements TeamLike, ForwardingAudience, PlayerStore {

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

    public CBCTeam (TeamLike originalTeam, String teamIdNum) {
        this.id = originalTeam.id();
        this.name = originalTeam.name();
        this.teamColor = originalTeam.teamColor();
        this.prefix = originalTeam.prefix();
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

    public Collection<P> players () {
        return players.values();
    }

    public Optional<P> getPlayerByUUID (UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public Collection<P> alivePlayers() {
        return players().stream()
                .filter(CBCPlayer::isAlive)
                .collect(Collectors.toSet());
    }

    public Set<P> onlinePlayers() {
        return players().stream()
                .filter(CBCPlayer::isOnline)
                .collect(Collectors.toSet());
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

    public String prefix () {
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
        scoreboardTeam.unregister();
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
        if (player.team() == null) return false;
        if (player.team() == this) return true;
        return otherAlliedTeams.contains(player.team());

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
        for (P player : players()) {
            if (player.isOnline()) {
                Player playerEntity = player.getPlayer();
                playerEntity.playSound(playerEntity.getLocation(),  sound, volume, pitch);
            }
        }
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences () {
        return players.values().stream()
                .filter(CBCPlayer::isOnline)
                .map(CBCPlayer::getPlayer)
                .toList();
    }

}
