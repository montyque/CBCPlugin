package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ThrowdownPostGameStats extends PostGameStats {

    private final ThrowdownGame game;

    public ThrowdownPostGameStats(ThrowdownGame game) {
        this.game = game;
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
        List<ThrowdownPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : game.getPlayers().values()) {
            if (player instanceof ThrowdownPlayer) {
                playersList.add((ThrowdownPlayer) player);
            }
        }

        List<ThrowdownPlayer> playersByKills = new ArrayList<>(playersList);
        playersByKills.sort(Comparator.comparingInt(ThrowdownPlayer::getKills));
        Collections.reverse(playersByKills);

        List<ThrowdownPlayer> playersByTimeAlive = new ArrayList<>(playersList);
        playersByTimeAlive.sort(Comparator.comparingInt(ThrowdownPlayer::getPlayerSecondsAlive));
        Collections.reverse(playersByTimeAlive);

        ThrowdownPlayer mostKills = playersByKills.get(0);
        audience.sendMessage(
                Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                        .append(createPlayerNameComponent(mostKills))
                        .append(Component.text(" (" + mostKills.getKills() + ")").color(NamedTextColor.WHITE))
        );

        ThrowdownPlayer mostTimeAlive = playersByKills.get(0);
        String timeAliveString = getTimeFormat(mostTimeAlive.getPlayerSecondsAlive());
        audience.sendMessage(
                Component.text("Most Time Alive: ").color(NamedTextColor.WHITE)
                        .append(createPlayerNameComponent(mostKills))
                        .append(Component.text(" (" + timeAliveString + ")").color(NamedTextColor.WHITE))
        );

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );

    }

    @Override
    public Inventory createInventoryGui(Player user) {

        Inventory inventory = Bukkit.createInventory(user, 54, Component.text("Post Game Statistics"));

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getMapName(), NamedTextColor.GREEN);
        addLoreField(loreList, "Rounds", "" + game.getRoundNumber(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().getName(), NamedTextColor.GREEN);
        }

        addLoreBlankLine(loreList);

        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);
        int averageRoundTime = game.getGameInRoundTime() / game.getRoundNumber();
        addLoreField(loreList, "Average Round Length", getTimeFormat(averageRoundTime), NamedTextColor.GREEN);
        addLoreField(loreList, "Sudden Death Rounds", "" + game.getSuddenDeathRounds(), NamedTextColor.RED);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);
        inventory.setItem(0, gameSummaryItem);

        // Sort players on statistics
        List<ThrowdownPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : game.getPlayers().values()) {
            if (player instanceof ThrowdownPlayer) {
                playersList.add((ThrowdownPlayer) player);
            }
        }
        playersList.sort(Comparator.comparingInt(ThrowdownPlayer::getRoundsWon).reversed()
                .thenComparing(Comparator.comparingInt(ThrowdownPlayer::getKills).reversed())
                .thenComparing(Comparator.comparingInt(ThrowdownPlayer::getPlayerSecondsAlive).reversed())
        );

        List<ThrowdownPlayer> playersByKills = new ArrayList<>(playersList);
        playersByKills.sort(Comparator.comparingInt(ThrowdownPlayer::getKills));
        Collections.reverse(playersByKills);

        List<ThrowdownPlayer> playersByTimeAlive = new ArrayList<>(playersList);
        playersByTimeAlive.sort(Comparator.comparingInt(ThrowdownPlayer::getPlayerSecondsAlive));
        Collections.reverse(playersByTimeAlive);

        // Go through each team
        int playerSlot = 0;
        for (ThrowdownPlayer player : playersList) {

            ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

            playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

            playerItemMeta.displayName(
                    player.getNameComponent().color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            );

            List<Component> playerLoreList = new ArrayList<>();

            addLoreField(playerLoreList, "Rounds Won", player.getRoundsWon() + "", NamedTextColor.GREEN);

            int positionInKills = playersByKills.indexOf(player) + 1;
            if (positionInKills > 0) {
                addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
            }
            addLoreField(playerLoreList, "Highest Kill Streak", player.getMaxKillStreak() + "", NamedTextColor.GREEN);
            addLoreField(playerLoreList, "Deaths", player.getDeaths() + "", NamedTextColor.RED);

            double kdRatio = (double) player.getKills() / (double) player.getDeaths();

            addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

            addLoreBlankLine(playerLoreList);

            int positionInTimeAlive = playersByTimeAlive.indexOf(player) + 1;
            if (positionInTimeAlive > 0) {
                addLoreField(playerLoreList, "Time Alive", getTimeFormat(player.getPlayerSecondsAlive()) +
                        " (#" + positionInTimeAlive + ")", NamedTextColor.GREEN);
            }

            // Set the item lore and the item meta then add item to inventory
            playerSlot++;
            if ((playerSlot % 9) == 0) {
                playerSlot++;
            }

            playerItemMeta.lore(playerLoreList);
            playerItem.setItemMeta(playerItemMeta);
            inventory.setItem(playerSlot, playerItem);
        }
        return inventory;
    }
}
