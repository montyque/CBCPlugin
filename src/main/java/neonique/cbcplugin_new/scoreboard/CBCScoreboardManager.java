package neonique.cbcplugin_new.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class CBCScoreboardManager {

    // Is scoreboard manager currently active
    private boolean active = false;

    private static CBCScoreboardManager instance;

    private ScoreboardManager scoreboardManager;

    private final Scoreboard mainScoreboard;

    private Map<String, CBCScoreboardTeam> teams;
    private final Map<UUID, Scoreboard> playerScoreboards;

    public CBCScoreboardManager (ScoreboardManager scoreboardManager) {

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

        // Assign individual scoreboard to all players currently online
        playerScoreboards.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPlayer(player);
        }

        // Add all existing bukkit teams to the main scoreboard
        teams = new HashMap<>();
        for (Team bukkitTeam : mainScoreboard.getTeams()) {
            CBCScoreboardTeam newTeam = CBCScoreboardTeam.fromBukkitTeam(this, bukkitTeam);
            registerTeam(newTeam);
        }

        active = true;

    }

    public Collection<Scoreboard> getAllScoreboards () {
        Set<Scoreboard> scoreboards = new HashSet<>(playerScoreboards.values());
        scoreboards.add(mainScoreboard);
        return scoreboards;
    }

    public CBCScoreboardTeam registerNewTeam (String name) {
        CBCScoreboardTeam team = new CBCScoreboardTeam(this, name);
        registerTeam(team);
        return team;
    }

    public void registerTeam (CBCScoreboardTeam team) {
        teams.put(team.name(), team);
        syncTeam(team);
    }

    public void unregisterTeam (CBCScoreboardTeam team) {
        if (!teams.containsKey(team.name())) throw new IllegalArgumentException("Team with id " + team + " not registered in scoreboard manager");
        teams.remove(team.name());
        getAllScoreboards().forEach(team::unregisterFromScoreboard);
    }

    public void syncTeam (CBCScoreboardTeam team) {
        if (!teams.containsKey(team.name())) throw new IllegalArgumentException("Team with id " + team + " not registered in scoreboard manager");
        getAllScoreboards().forEach(team::syncToScoreboard);
    }

    public void deactivate () {

        if (!active) return;

        // Assign
        playerScoreboards.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(mainScoreboard);
        }

        active = false;

    }

    public void addPlayer (Player player) {

        UUID playerUUID = player.getUniqueId();
        Scoreboard newScoreboard = scoreboardManager.getNewScoreboard();

        player.setScoreboard(newScoreboard);
        playerScoreboards.put(playerUUID, newScoreboard);

        // Register teams on main scoreboard to client scoreboard
        teams.values().forEach(t -> t.syncToScoreboard(newScoreboard));

        Objective healthObjective = newScoreboard.registerNewObjective("health", "health", Component.text("❤"), RenderType.HEARTS);
        Objective healthObjective2 = newScoreboard.registerNewObjective("health2", "health", Component.text("❤"), RenderType.HEARTS);

        healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        healthObjective2.setDisplaySlot(DisplaySlot.PLAYER_LIST);

    }

    public void removePlayer (Player player) {

        // Remove player from player scoreboards
        UUID playerUUID = player.getUniqueId();

        if (playerScoreboards.containsKey(playerUUID)) {

            playerScoreboards.remove(playerUUID);
            player.setScoreboard(mainScoreboard);

        }

    }

    public Scoreboard getPlayerScoreboard (UUID uuid) {
        return playerScoreboards.getOrDefault(uuid, null);
    }

    public boolean isActive () {
        return active;
    }

}
