package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.CBCPlugin.getGameManager;

public abstract class CBCTeam<P extends CBCPlayer> {

    // List of members
    private final HashMap<UUID, P> players = new HashMap<>();

    // Teams that will not be able to damage/kill players from this team
    private final Set<CBCTeam<P>> otherAlliedTeams = new HashSet<>();

    // Team data
    private final String id;
    private final String teamIdNum;
    private final String prefix;
    private final String displayName;
    private final NamedTextColor color;
    private final ItemStack item;
    private final ItemStack glassHead;
    private final char specialCrossbowChar;
    private final int colorNumber;
    private final TrimMaterial trimMaterial;

    private final Team teamObject;

    public CBCTeam(String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                   String prefix, ItemStack item, ItemStack glassHead) {
        this.id = teamId;
        this.displayName = teamName;
        this.color = teamColor;
        this.teamIdNum = teamIdNum;
        this.prefix = prefix;
        this.item = item;
        this.glassHead = glassHead;

        // Set scoreboard manager
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        // Team scoreboard object
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // Create new team
        teamObject = scoreboard.registerNewTeam(teamIdNum + teamId);
        teamObject.setCanSeeFriendlyInvisibles(true);
        teamObject.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.FOR_OTHER_TEAMS);
        teamObject.setAllowFriendlyFire(false); // Do not allow friendly fire
        teamObject.color(teamColor); // Set team color
        // If there's a team prefix, set it
        if (prefix != null) {
            teamObject.prefix(
                    Component.text(prefix + " ").color(color).decorate(TextDecoration.BOLD)
            );
        }

        GameManager gameManager = getGameManager();

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(teamObject);
        }


        if (teamColor == NamedTextColor.RED) specialCrossbowChar = '\uE110';
        else if (teamColor == NamedTextColor.BLUE) specialCrossbowChar = '\uE111';
        else if (teamColor == NamedTextColor.GREEN) specialCrossbowChar = '\uE112';
        else if (teamColor == NamedTextColor.YELLOW) specialCrossbowChar = '\uE113';
        else if (teamColor == NamedTextColor.AQUA) specialCrossbowChar = '\uE114';
        else if (teamColor == NamedTextColor.GOLD) specialCrossbowChar = '\uE115';
        else if (teamColor == NamedTextColor.LIGHT_PURPLE) specialCrossbowChar = '\uE116';
        else if (teamColor == NamedTextColor.DARK_PURPLE) specialCrossbowChar = '\uE117';
        else specialCrossbowChar = '\uE219';

        if (teamColor == NamedTextColor.RED) colorNumber = 0;
        else if (teamColor == NamedTextColor.BLUE) colorNumber = 1;
        else if (teamColor == NamedTextColor.GREEN) colorNumber = 2;
        else if (teamColor == NamedTextColor.YELLOW) colorNumber = 3;
        else if (teamColor == NamedTextColor.AQUA) colorNumber = 4;
        else if (teamColor == NamedTextColor.GOLD) colorNumber = 5;
        else if (teamColor == NamedTextColor.LIGHT_PURPLE) colorNumber = 6;
        else if (teamColor == NamedTextColor.DARK_PURPLE) colorNumber = 7;
        else colorNumber = 9;

        if (teamColor == NamedTextColor.RED) trimMaterial = TrimMaterial.REDSTONE;
        else if (teamColor == NamedTextColor.BLUE) trimMaterial = TrimMaterial.LAPIS;
        else if (teamColor == NamedTextColor.GREEN) trimMaterial = TrimMaterial.EMERALD;
        else if (teamColor == NamedTextColor.YELLOW) trimMaterial = TrimMaterial.GOLD;
        else if (teamColor == NamedTextColor.AQUA) trimMaterial = TrimMaterial.DIAMOND;
        else if (teamColor == NamedTextColor.GOLD) trimMaterial = TrimMaterial.COPPER;
        else if (teamColor == NamedTextColor.LIGHT_PURPLE) trimMaterial = TrimMaterial.IRON;
        else if (teamColor == NamedTextColor.DARK_PURPLE) trimMaterial = TrimMaterial.AMETHYST;
        else trimMaterial = null;
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

    public String getTeamName () {
        return displayName;
    }

    public String getPrefix () {
        return prefix;
    }

    public NamedTextColor getColor () {
        return color;
    }

    public Team getTeamObject () {
        return teamObject;
    }

    public void addPlayer (P player) {
        players.put(player.getOfflinePlayer().getUniqueId(), player);
        getGameManager().getCbcScoreboardManager().addTeamEntry(player.getName(), teamObject);
        player.setTeam(this);
    }

    public void removePlayer (P player) {
        players.remove(player.getOfflinePlayer().getUniqueId());
        getGameManager().getCbcScoreboardManager().removeTeamEntry(player.getName(), teamObject);
        player.setTeam(null);
    }

    public void removeTeam () {
        if (getGameManager().getCbcScoreboardManager().isActive()) {
            getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(teamObject.getName());
        }
        teamObject.unregister();
    }

    public void replacePlayerEntityKey(Player origin, Player newPlayer) {
        if (players.containsKey(origin.getUniqueId())) {
            P cbcPlayer = players.get(origin.getUniqueId());
            players.remove(origin.getUniqueId());
            players.put(newPlayer.getUniqueId(), cbcPlayer);
        }
    }

    public ItemStack getItem() {
        return item;
    }

    public ItemStack getGlassHead() {
        return glassHead;
    }

    public char getSpecialCrossbowChar() {
        return specialCrossbowChar;
    }

    public TrimMaterial getTrimMaterial () {
        return trimMaterial;
    }

    public int getColorNumber() {
        return colorNumber;
    }

    public char getSwordChar (boolean rightFacing) {
        if (rightFacing) {
            return ((char) (0xE440 + getColorNumber()));
        }
        else {
            return ((char) (0xE450 + getColorNumber()));
        }
    }

    public String getTeamId() {
        return id;
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
        String name = getTeamName();
        if (withTeam) {
            name += " Team";
        }
        return Component.text(name).color(color);
    }

    public void playGlobalSound(Sound sound, float volume, float pitch) {
        for (CBCPlayer player : getPlayers()) {
            if (player.isOnline()) {
                Player playerEntity = player.getPlayer();
                playerEntity.playSound(playerEntity.getLocation(),  sound, volume, pitch);
            }
        }
    }
}
