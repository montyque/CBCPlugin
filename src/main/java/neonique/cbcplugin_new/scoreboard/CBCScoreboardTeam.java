package neonique.cbcplugin_new.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.UUID;
import java.util.stream.Collectors;

public class CBCScoreboardTeam {

    private final CBCScoreboardManager manager;

    private final String name;
    private final Set<UUID> entities = new HashSet<>();

    private Component displayName;
    private NamedTextColor color;
    private Component prefix;
    private Component suffix;
    private boolean friendlyFireEnabled;
    private boolean seeFriendlyInvisiblesEnabled;
    private final Map<Team.Option, Team.OptionStatus> options;

    public CBCScoreboardTeam (CBCScoreboardManager manager, String name) {

        this.manager = manager;
        this.name = name;

        options = Arrays.stream(Team.Option.values())
                .collect(Collectors.toMap(o -> o, o -> Team.OptionStatus.ALWAYS));

    }

    /**
     * Copies the information of an existing Bukkit scoreboard team to a CBCScoreboardTeam.
     * @param manager The CBCScoreboardManager to register this team with.
     * @param bukkitTeam The Bukkit scoreboard team to copy information from.
     * @return New CBCScoreboard team.
     */
    public static CBCScoreboardTeam fromBukkitTeam (CBCScoreboardManager manager, Team bukkitTeam) {

        CBCScoreboardTeam team = new CBCScoreboardTeam(manager, bukkitTeam.getName());

        team.setDisplayName(bukkitTeam.displayName());
        if (bukkitTeam.hasColor()) team.setColor(NamedTextColor.nearestTo(bukkitTeam.color()));
        team.setPrefix(bukkitTeam.prefix());
        team.setSuffix(bukkitTeam.suffix());
        team.setFriendlyFireEnabled(bukkitTeam.allowFriendlyFire());
        team.setSeeFriendlyInvisiblesEnabled(bukkitTeam.canSeeFriendlyInvisibles());

        for (Team.Option o : Team.Option.values()) {
            team.setOption(o, bukkitTeam.getOption(o));
        }

        // Preserve player and entity entries
        for (String entry : bukkitTeam.getEntries()) {
            try {
                team.addEntityUUID(UUID.fromString(entry));
            } catch (IllegalArgumentException e) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(entry);
                team.addEntityUUID(player.getUniqueId());
            }
        }

        return team;

    }
    
    /**
     * Retrieves the scoreboard API Team object associated with this team for a given scoreboard.
     * If the team does not exist on this scoreboard, it will be registered.
     * @param scoreboard The scoreboard to get team from.
     * @return Team from this scoreboard.
     */
    public Team toBukkitTeam (Scoreboard scoreboard) {
        if (scoreboard == null) throw new IllegalArgumentException("Scoreboard must exist");
        if (scoreboard.getTeam(name) != null) {
            return scoreboard.getTeam(name);
        } else {
            return scoreboard.registerNewTeam(name);
        }
    }

    /**
     * Creates/modifies a Team object in the given Scoreboard with the same team name.
     * The created team will be given the same attributes (i.e. display name, color, etc.) as this team.
     * @param scoreboard The scoreboard to sync with.
     */
    public void syncToScoreboard (Scoreboard scoreboard) {

        Team team = toBukkitTeam(scoreboard);
        if (team == null) return;

        for (UUID entityUUID : entities) {
            Player player = Bukkit.getPlayer(entityUUID);
            String entry = player != null ? player.getName() : entityUUID.toString();
            if (!team.hasEntry(entry)) {
                team.addEntry(entityUUID.toString());
            }
        }

        team.displayName(displayName);
        team.color(color);
        team.prefix(prefix);
        team.suffix(suffix);
        team.setAllowFriendlyFire(friendlyFireEnabled);
        team.setCanSeeFriendlyInvisibles(seeFriendlyInvisiblesEnabled);

    }

    public void unregisterFromScoreboard (Scoreboard scoreboard) {
        Team team = scoreboard.getTeam(name);
        if (team == null) return;
        team.unregister();
    }

    public void addEntityUUID (UUID entityUUID) {
        entities.add(entityUUID);
        manager.syncTeam(this);
    }

    public boolean hasEntityUUID (UUID entityUUID) {
        return entities.contains(entityUUID);
    }

    public void removeEntityUUID (UUID entityUUID) {
        entities.remove(entityUUID);
        manager.syncTeam(this);
    }

    public String name() {
        return name;
    }

    public Component displayName() {
        return displayName;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
        manager.syncTeam(this);
    }

    public NamedTextColor color() {
        return color;
    }

    public void setColor(NamedTextColor color) {
        this.color = color;
        manager.syncTeam(this);
    }

    public Component prefix() {
        return prefix;
    }

    public void setPrefix(Component prefix) {
        this.prefix = prefix;
        manager.syncTeam(this);
    }

    public Component suffix() {
        return suffix;
    }

    public void setSuffix(Component suffix) {
        this.suffix = suffix;
        manager.syncTeam(this);
    }

    public boolean friendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) {
        this.friendlyFireEnabled = friendlyFireEnabled;
        manager.syncTeam(this);
    }

    public boolean seeFriendlyInvisiblesEnabled() {
        return seeFriendlyInvisiblesEnabled;
    }

    public void setSeeFriendlyInvisiblesEnabled(boolean seeFriendlyInvisiblesEnabled) {
        this.seeFriendlyInvisiblesEnabled = seeFriendlyInvisiblesEnabled;
        manager.syncTeam(this);
    }

    public void setOption (Team.Option option, Team.OptionStatus status) {
        options.put(option, status);
        manager.syncTeam(this);
    }


}
