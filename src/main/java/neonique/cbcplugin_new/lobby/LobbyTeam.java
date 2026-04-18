package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static neonique.cbcplugin_new.CBCPlugin.getGameManager;

// This class is used for storing the players in a team in the lobby
public class LobbyTeam {

    private final GameManager gameManager;
    private final Lobby lobby;

    // Team information
    private String teamIdNum;
    private final String teamId;
    private final String teamName;
    private final String prefix;
    private final Material glassHead;
    private final NamedTextColor color;
    private final int colorNumber;

    // Scoreboard team
    Team teamObject;

    private final HashMap<UUID, LobbyPlayer> playersInTeam = new HashMap<>();

    public LobbyTeam(GameManager gameManager, Lobby lobby, String teamIdNum, String teamId, String teamName, String prefix,
                     Material glassHead, NamedTextColor color) {

        this.gameManager = gameManager;
        this.lobby = lobby;

        this.teamIdNum = teamIdNum;
        this.teamId = teamId;
        this.teamName = teamName;
        this.prefix = prefix;
        this.color = color;
        this.glassHead = glassHead;

        // Set scoreboard manager
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        // Team scoreboard object
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        // Create new team object
        teamObject = scoreboard.registerNewTeam(teamIdNum + teamId + "Lobby");
        teamObject.setAllowFriendlyFire(true); // Allow friendly fire
        // If there's a team prefix, set it
        if (prefix != null) {
            teamObject.prefix(
                    Component.text(" ■ ").color(color)
            );
        }

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(teamObject);
        }

        if (color == NamedTextColor.RED) colorNumber = 0;
        else if (color == NamedTextColor.BLUE) colorNumber = 1;
        else if (color == NamedTextColor.GREEN) colorNumber = 2;
        else if (color == NamedTextColor.YELLOW) colorNumber = 3;
        else if (color == NamedTextColor.AQUA) colorNumber = 4;
        else if (color == NamedTextColor.GOLD) colorNumber = 5;
        else if (color == NamedTextColor.LIGHT_PURPLE) colorNumber = 6;
        else if (color == NamedTextColor.DARK_PURPLE) colorNumber = 7;
        else colorNumber = 9;
    }

    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.WHITE_DYE);
        ItemMeta itemMeta = item.getItemMeta();

        // Set item title
        Component itemTitle = Component.text(teamName).color(color)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        List<Component> loreList = new ArrayList<>();
        itemMeta.lore(loreList);

        itemMeta.setCustomModelData(colorNumber + 1);

        item.setItemMeta(itemMeta);
        return item;
    }

    public ItemStack getGlassHead() {
        ItemStack item = new ItemStack(glassHead);
        ItemMeta itemMeta = item.getItemMeta();
        // Set item title
        Component itemTitle = Component.text(teamName + " Glass Head").color(color)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
        item.setItemMeta(itemMeta);
        return item;
    }

    public void addPlayer(LobbyPlayer player) {
        // Add player to team
        playersInTeam.put(player.getOfflinePlayer().getUniqueId(), player);

        gameManager.getCbcScoreboardManager().addTeamEntry(player.getOfflinePlayer().getName(), teamObject);

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
        // Remove player from team
        if (!isPlayerInTeam(player)) return;
        playersInTeam.remove(player.getOfflinePlayer().getUniqueId());
        getGameManager().getCbcScoreboardManager().removeTeamEntry(player.getOfflinePlayer().getName(), teamObject);

        player.playerLeaveTeam();
    }

    public void removeTeam() {
        getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(teamObject.getName());
        teamObject.unregister();
    }

    public String getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getPrefix() {
        return prefix;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public int getColorNumber() {
        return colorNumber;
    }

}
