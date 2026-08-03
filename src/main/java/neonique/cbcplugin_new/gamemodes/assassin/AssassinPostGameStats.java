package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.core.CBCPlayer;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AssassinPostGameStats extends PostGameStats {

    private final AssassinGame game;

    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByTargetsKilled;
    List<PlayerStatObject> playersByTargetDeaths;

    public AssassinPostGameStats (AssassinGame game) {

        this.game = game;

        // Sort players on statistics
        List<AssassinPlayer> playersList = game.players();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            playersByTargetsKilled = new ArrayList<>();
            playersByTargetDeaths = new ArrayList<>();

            for (AssassinPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's targets killed
                playersByTargetsKilled.add(new PlayerStatObject(player, player.getTargetKills()));
                // Add player's times died as target
                playersByTargetDeaths.add(new PlayerStatObject(player, player.getTargetDeaths()));
            }

            // Sort players by kills and time alive
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByTargetsKilled = sortPlayerStatList(playersByTargetsKilled, true);
            playersByTargetDeaths = sortPlayerStatList(playersByTargetDeaths, false);

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
        List<AssassinPlayer> playersList = game.players();

        if (!playersList.isEmpty()) {

            // Show which players were in first for target kills
            int mostTargetKillsValue = playersByTargetsKilled.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Target Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTargetsKilled).color(NamedTextColor.AQUA))
                            .append(Component.text(" (" + mostTargetKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills).color(NamedTextColor.AQUA))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time alive
            int leastTargetDeathsValue = playersByTargetDeaths.get(0).getValue();

            audience.sendMessage(
                    Component.text("Least Deaths As Target: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTargetDeaths).color(NamedTextColor.AQUA))
                            .append(Component.text(" (" + leastTargetDeathsValue + ")").color(NamedTextColor.WHITE))
            );
        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
    }

    @Override
    public Inventory createInventoryGui(Player user) {
        return inventoryGuiGenerate(user, false,
                new ArrayList<>(), new ArrayList<>(game.players()));
    }

    @Override
    public ItemStack generateGameSummaryItem() {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getName(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().name(), NamedTextColor.GOLD);
        }

        loreList.add(Component.text(" "));
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);

        loreList.add(Component.text(" "));

        // Display most target kills
        int mostTargetKills = playersByTargetsKilled.get(0).getValue();
        addLoreField(loreList, "Most Target Kills", getFirstPlaceString(playersByTargetsKilled)
                + " (" + mostTargetKills + ")", NamedTextColor.GREEN);

        // Display most kills
        int mostKills = playersByKills.get(0).getValue();
        addLoreField(loreList, "Most Kills", getFirstPlaceString(playersByKills)
                + " (" + mostKills + ")", NamedTextColor.GREEN);

        // Display least target kills
        int leastTargetDeaths = playersByKills.get(0).getValue();
        addLoreField(loreList, "Least Deaths as Target", getFirstPlaceString(playersByTargetDeaths)
                + " (" + leastTargetDeaths + ")", NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {

        AssassinPlayer player = (AssassinPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.nameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in stats
        PlayerStatObject playerTargetsKilled = getPlayerStatObject(player, playersByTargetsKilled);
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerTargetDeaths = getPlayerStatObject(player, playersByTargetDeaths);

        // Show player target kills
        int positionInTargetsKilled = playerTargetsKilled.getPlacement();
        if (positionInTargetsKilled > 0) {
            addLoreField(playerLoreList, "Targets Killed", player.getTargetKills() +
                    " (#" + positionInTargetsKilled + ")", NamedTextColor.GREEN);
        }

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInDeathsAsTarget = playerTargetDeaths.getPlacement();
        if (positionInDeathsAsTarget > 0) {
            addLoreField(playerLoreList, "Deaths as Target", player.getTargetDeaths() +
                    " (#" + positionInDeathsAsTarget + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;

    }

    @Override
    public List<CBCPlayer> sortPlayers (List<CBCPlayer> playerList) {

        List<AssassinPlayer> assassinPlayers = new ArrayList<>();
        for (CBCPlayer player : playerList) {
            if (player instanceof AssassinPlayer) {
                assassinPlayers.add((AssassinPlayer) player);
            }
        }

        // Sort players on target kills
        assassinPlayers.sort(Comparator.comparingInt(AssassinPlayer::getTargetKills));
        Collections.reverse(assassinPlayers);

        // Convert list back into CBC players
        return new ArrayList<>(assassinPlayers);

    }

}
