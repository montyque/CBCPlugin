package neonique.cbcplugin_new.cbcevents;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

import static neonique.cbcplugin_new.util.StringUtil.firstLetterUpper;

public class CBCEventTeam {

    private CBCEventManager eventManager;

    // Team information
    private final String teamId;
    private final String teamName;
    private final NamedTextColor teamColor;

    // Player information
    private final Set<CBCEventPlayer> players = new HashSet<>();

    private static HashMap<String, NamedTextColor> idToColor = new HashMap<>();

    static {
        idToColor.put("red", NamedTextColor.RED);
        idToColor.put("blue", NamedTextColor.BLUE);
        idToColor.put("green", NamedTextColor.GREEN);
        idToColor.put("yellow", NamedTextColor.YELLOW);
        idToColor.put("cyan", NamedTextColor.AQUA);
        idToColor.put("orange", NamedTextColor.GOLD);
        idToColor.put("magenta", NamedTextColor.LIGHT_PURPLE);
        idToColor.put("purple", NamedTextColor.DARK_PURPLE);
    }

    public CBCEventTeam (CBCEventManager eventManager, String teamId, String teamName, NamedTextColor teamColor) {

        this.eventManager = eventManager;
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamColor = teamColor;

    }

    public void addPlayer (CBCEventPlayer player) {
        players.add(player);
    }

    public Set<CBCEventPlayer> getPlayers () {
        return players;
    }

    public Set<UUID> getPlayerUUIDs () {
        Set<UUID> uuids = new HashSet<>();
        for (CBCEventPlayer player : players) {
            uuids.add(player.getPlayerUUID());
        }
        return uuids;
    }

    public Set<Player> getOnlinePlayerEntities () {
        Set<Player> playerEntities = new HashSet<>();
        for (CBCEventPlayer player : players) {
            if (!player.isOnline()) continue;
            playerEntities.add(player.getPlayer());
        }
        return playerEntities;
    }

    public Set<CBCEventPlayer> getOnlinePlayers () {
        Set<CBCEventPlayer> onlinePlayers = new HashSet<>();
        for (CBCEventPlayer player : players) {
            if (!player.isOnline()) continue;
            onlinePlayers.add(player);
        }
        return onlinePlayers;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public NamedTextColor getTeamColor() {
        return teamColor;
    }

    public static NamedTextColor getTeamColorById (String teamId) {
        return idToColor.getOrDefault(teamId, null);
    }

    public static List<String> getAllTeamIds () {
        return new ArrayList<>(idToColor.keySet());
    }

    public void removePlayer(CBCEventPlayer player) {
        players.remove(player);
        player.setTeam(null);
    }

    public Component getNameComponent (boolean withTeam) {

        String text = teamName;
        if (withTeam) {
            text += " Team";
        }

        return Component.text(text).color(teamColor);
    }
}
