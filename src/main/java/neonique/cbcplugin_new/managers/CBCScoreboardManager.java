package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.CrossbowWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class CBCScoreboardManager {

    // Is scoreboard manager currently active
    private boolean active = false;

    private static CBCScoreboardManager instance;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private Scoreboard mainScoreboard;
    private HashMap<UUID, Scoreboard> playerScoreboards;

    public CBCScoreboardManager (GameManager gameManager, ScoreboardManager scoreboardManager) {

        this.gameManager = gameManager;

        this.scoreboardManager = scoreboardManager;

        mainScoreboard = scoreboardManager.getMainScoreboard();
        mainScoreboard.getTeams();

        playerScoreboards = new HashMap<>();

        instance = this;

    }

    public static CBCScoreboardManager getInstance() {
        return instance;
    }

    public void activate () {

        if (active) return;

        // Activate scoreboard manager
        playerScoreboards.clear();

        // Go through all players and add them
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }

        active = true;
    }

    public void deactivate () {

        if (!active) return;

        // Deactivate scoreboard manager
        playerScoreboards.clear();

        // Set all players back to main scoreboard
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(mainScoreboard);
        }

        active = false;
    }

    public void addPlayer (Player player) {

        // Add player to player scoreboards
        UUID playerUUID = player.getUniqueId();

        // Create new scoreboard
        Scoreboard newScoreboard = scoreboardManager.getNewScoreboard();

        // Register current teams in the main scoreboard, so they are on the new scoreboard
        for (Team team : mainScoreboard.getTeams()) {
            registerTeamForClient(newScoreboard, team);
        }

        // Set player scoreboard to this new scoreboard
        player.setScoreboard(newScoreboard);

        // Add to scoreboard list
        playerScoreboards.put(playerUUID, newScoreboard);

        // Create health variable at top
        Objective healthObjective = newScoreboard.registerNewObjective("health", "health", Component.text("❤"), RenderType.HEARTS);
        Objective healthObjective2 = newScoreboard.registerNewObjective("health2", "health", Component.text("❤"), RenderType.HEARTS);

        healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        healthObjective2.setDisplaySlot(DisplaySlot.PLAYER_LIST);

    }

    public void removePlayer (Player player) {

        // Remove player from player scoreboards
        UUID playerUUID = player.getUniqueId();

        if (playerScoreboards.containsKey(playerUUID)) {

            // Remove player scoreboard from data
            playerScoreboards.remove(playerUUID);

            // Reset player scoreboard to main scoreboard
            player.setScoreboard(mainScoreboard);
        }

    }

    // Register team for all current scoreboards
    public void registerTeamForAllClients (Team team) {
        if (!active) return;
        // Register current teams in the main scoreboard, so they are on the new scoreboard
        for (Scoreboard scoreboard : getAllClientScoreboards()) {
            registerTeamForClient(scoreboard, team);
        }
    }

    // Register team for scoreboard
    public void registerTeamForClient (Scoreboard scoreboard, Team team) {

        Team newTeam;
        try {
            newTeam = scoreboard.registerNewTeam(team.getName());
        } catch (IllegalArgumentException e) {
            newTeam = scoreboard.getTeam(team.getName());
        }

        if (newTeam == null) return;

        try {
            NamedTextColor color = (NamedTextColor) team.color();
            newTeam.color(color);
        } catch (IllegalStateException e) {
            newTeam.color(NamedTextColor.WHITE);
        }
        newTeam.displayName(team.displayName());

        newTeam.prefix(team.prefix());
        newTeam.suffix(team.suffix());
        newTeam.setAllowFriendlyFire(team.allowFriendlyFire());
        newTeam.setCanSeeFriendlyInvisibles(team.canSeeFriendlyInvisibles());

        team.setOption(Team.Option.COLLISION_RULE, team.getOption(Team.Option.COLLISION_RULE));

        // Add entries to team
        for (String entry : team.getEntries()) {
            newTeam.addEntry(entry);
        }
    }

    public void setTeamOption (Team team, Team.Option option, Team.OptionStatus status) {

        String teamId = team.getName();

        Team mainScoreboardTeam = mainScoreboard.getTeam(team.getName());
        if (mainScoreboardTeam != null) {
            mainScoreboardTeam.setOption(option, status);
        }

        for (Scoreboard scoreboard : getAllClientScoreboards()) {
            Team clientTeam = scoreboard.getTeam(teamId);
            if (clientTeam == null) continue;

            clientTeam.setOption(option, status);
        }
    }

    // Register team for all current scoreboards
    public void unregisterTeamForAllClients (String teamId) {
        if (!active) return;
        // Register current teams in the main scoreboard, so they are on the new scoreboard
        for (Scoreboard scoreboard : getAllClientScoreboards()) {
            unregisterTeamForClient(scoreboard, teamId);
        }
    }

    // Register team for scoreboard
    public void unregisterTeamForClient (Scoreboard scoreboard, String teamId) {

        Team newTeam = scoreboard.getTeam(teamId);
        if (newTeam == null) return;

        try {
            newTeam.unregister();
        } catch (IllegalArgumentException ignored) {}
    }

    // Add entry to team for scoreboard
    public void addTeamEntry (String entry, String teamId) {

        Team team = mainScoreboard.getTeam(teamId);
        if (team == null) return;
        addTeamEntry(entry, team);

    }

    // Add entry to team for scoreboard
    public void addTeamEntry (String entry, Team team) {

        // Get team ID
        String teamId = team.getName();

        // Add entry to team
        team.addEntry(entry);

        if (!active) return;

        // Go through each scoreboard
        for (Scoreboard clientScoreboard : getAllClientScoreboards()) {
            // Find the equivalent team in that scoreboard
            Team clientTeam = clientScoreboard.getTeam(teamId);
            if (clientTeam != null) {
                clientTeam.addEntry(entry);
            }
        }
    }

    public void removeTeamEntry (String entry, Team team) {

        // Get team ID
        String teamId = team.getName();

        // Add entry to team
        team.removeEntry(entry);

        if (!active) return;

        // Go through each scoreboard
        for (Scoreboard clientScoreboard : getAllClientScoreboards()) {
            // Find the equivalent team in that scoreboard
            Team clientTeam = clientScoreboard.getTeam(teamId);
            if (clientTeam != null) {
                clientTeam.removeEntry(entry);
            }
        }
    }

    public Collection<Scoreboard> getAllClientScoreboards () {
        return playerScoreboards.values();
    }

    public Scoreboard getPlayerScoreboard (UUID uuid) {
        return playerScoreboards.getOrDefault(uuid, null);
    }

    public boolean isActive () {
        return active;
    }
}
