package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

// This class is used for storing the players in a team in the lobby
public class LobbyTeam {

    private final GameManager gameManager;
    private final Lobby lobby;

    // Team information
    private final String teamIdNum;
    private final String teamId;
    private final String teamName;
    private final String prefix;
    private final Material glassHead;
    private final NamedTextColor color;
    private final int colorNumber;

    private final CBCScoreboardTeam scoreboardTeam;

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

        scoreboardTeam = registerTeam();

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

    public CBCScoreboardTeam registerTeam () {

        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();
        CBCScoreboardTeam team = scoreboardManager.registerNewTeam(teamIdNum + teamId + "Lobby");
        team.setFriendlyFireEnabled(true);
        team.setPrefix(Component.text(" ■ ").color(color));
        return team;

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
