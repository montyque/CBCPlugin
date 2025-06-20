package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ThrowdownPostGameStats extends PostGameStats {

    private final ThrowdownGame game;

    private List<PlayerStatObject> playersByRoundsWon;
    private List<PlayerStatObject> playersByKills;
    private List<PlayerStatObject> playersByTimeAlive;

    public ThrowdownPostGameStats(ThrowdownGame game) {

        this.game = game;

        // Sort players on statistics
        List<ThrowdownPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : game.getPlayers().values()) {
            if (player instanceof ThrowdownPlayer) {
                playersList.add((ThrowdownPlayer) player);
            }
        }

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            playersByTimeAlive = new ArrayList<>();
            playersByRoundsWon = new ArrayList<>();

            for (ThrowdownPlayer player : playersList) {
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                playersByTimeAlive.add(new PlayerStatObject(player, player.getPlayerSecondsAlive()));
                playersByRoundsWon.add(new PlayerStatObject(player, player.getRoundsWon()));
            }

            // Sort players by kills and time alive
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByTimeAlive = sortPlayerStatList(playersByTimeAlive, true);
            playersByRoundsWon = sortPlayerStatList(playersByRoundsWon, true);

        }

    }

    @Override
    public void sendPostGameSummary(Audience audience) {

        audience.sendMessage(Component.text("Post Game Summary").color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED).append(Component.newline()));

        audience.sendMessage(
                Component.text("Game Length: ").color(NamedTextColor.WHITE)
                        .append(Component.text(getTimeFormat(game.getGameLength())).color(NamedTextColor.GREEN))
        );

        // Sort players on statistics
        List<ThrowdownPlayer> playersList = new ArrayList<>(game.getThrowdownPlayers());

        if (!playersList.isEmpty()) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();
            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills).color(NamedTextColor.AQUA))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time alive
            int mostTimeAliveValue = playersByTimeAlive.get(0).getValue();
            audience.sendMessage(
                    Component.text("Longest Time Alive: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTimeAlive).color(NamedTextColor.AQUA))
                            .append(Component.text(" (" + getTimeFormat(mostTimeAliveValue) + ")").color(NamedTextColor.WHITE))
            );

        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );

    }

    @Override
    public ItemStack generatePlayerItem(CBCPlayer rawPlayer) {

        ThrowdownPlayer player = (ThrowdownPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());
        playerItemMeta.displayName(
                player.getNameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in kills and time alive
        PlayerStatObject playerRoundsWon = getPlayerStatObject(player, playersByRoundsWon);
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerTimeAlive = getPlayerStatObject(player, playersByTimeAlive);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInRoundsWon = playerRoundsWon.getPlacement();
        if (positionInRoundsWon > 0) {
            addLoreField(playerLoreList, "Rounds Won", playerRoundsWon.getValue() +
                    " (#" + positionInRoundsWon + ")", NamedTextColor.GREEN);
        }

        int positionInTimeAlive = playerTimeAlive.getPlacement();
        if (positionInTimeAlive > 0) {
            addLoreField(playerLoreList, "Time Alive", getTimeFormat(player.getPlayerSecondsAlive()) +
                    " (#" + positionInTimeAlive + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);

        return playerItem;

    }

    @Override
    public ItemStack generateGameSummaryItem() {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getMapName(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().getName(), NamedTextColor.GOLD);
        }

        addLoreBlankLine(loreList);

        // Game length information
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);
        int averageRoundTime = game.getGameInRoundTime() / game.getRoundNumber();
        addLoreField(loreList, "Average Round Length", getTimeFormat(averageRoundTime), NamedTextColor.GREEN);
        addLoreField(loreList, "Sudden Death Rounds", String.valueOf(game.getSuddenDeathRounds()), NamedTextColor.RED);

        addLoreBlankLine(loreList);

        // Display most kills
        addLoreField(loreList, "Most Kills", getFirstPlaceString(playersByKills)
                + " (" + playersByKills.get(0).getValue() + ")", NamedTextColor.GREEN);

        // Display most time alive
        addLoreField(loreList, "Most Time Alive", getFirstPlaceString(playersByTimeAlive)
                + " (" + playersByTimeAlive.get(0).getValue() + ")", NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public Inventory createInventoryGui(Player user) {
        return inventoryGuiGenerate(user, false,
                new ArrayList<>(), new ArrayList<>(game.getPlayers().values()));
    }

    @Override
    public List<CBCPlayer> sortPlayers (List<CBCPlayer> playerList) {

        List<ThrowdownPlayer> throwdownPlayers = new ArrayList<>();
        for (CBCPlayer player : playerList) {
            if (player instanceof ThrowdownPlayer) {
                throwdownPlayers.add((ThrowdownPlayer) player);
            }
        }

        // Sort players on the amount of rounds they won
        throwdownPlayers.sort(Comparator.comparingInt(ThrowdownPlayer::getRoundsWon).reversed().thenComparing(
                Comparator.comparingInt(ThrowdownPlayer::getKills).reversed()
        ));

        // Convert list back into CBC players
        return new ArrayList<>(throwdownPlayers);

    }

}
