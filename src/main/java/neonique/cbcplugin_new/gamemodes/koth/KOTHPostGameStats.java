package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.core.CBCTeam;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KOTHPostGameStats extends PostGameStats {

    private final KOTHGame game;

    List<KOTHPlayer> playersList;
    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByTimeInHill;
    List<PlayerStatObject> playersByTimesCapturedHill;
    List<PlayerStatObject> playersByPointsDefended;

    public KOTHPostGameStats (KOTHGame game) {

        this.game = game;

        // Sort players on statistics
        playersList = game.getPlayers();

        playersByKills = new ArrayList<>();
        playersByTimeInHill = new ArrayList<>();
        playersByTimesCapturedHill = new ArrayList<>();
        playersByPointsDefended = new ArrayList<>();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();

            for (KOTHPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's time in hill
                playersByTimeInHill.add(new PlayerStatObject(player, player.getSecondsInHill()));
                // Add player's times hill captured
                playersByTimesCapturedHill.add(new PlayerStatObject(player, player.getHillCaptures()));
                // Add player's points defend
                playersByPointsDefended.add(new PlayerStatObject(player, player.getPointsDefended()));
            }

            // Sort players by statistics
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByTimeInHill = sortPlayerStatList(playersByTimeInHill, true);
            playersByTimesCapturedHill = sortPlayerStatList(playersByTimesCapturedHill, true);
            playersByPointsDefended = sortPlayerStatList(playersByPointsDefended, true);

        }
    }

    public void sendPostGameSummary(Audience audience) {

        audience.sendMessage(Component.text("Post Game Summary").color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED).append(Component.newline()));

        audience.sendMessage(
                Component.text("Game Length: ").color(NamedTextColor.WHITE)
                        .append(Component.text(getTimeFormat(game.getGameLength())).color(NamedTextColor.GREEN))
        );

        Component finalScore = getComponent();

        audience.sendMessage(finalScore);

        if (!playersList.isEmpty()) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time in hill
            int mostTimeInHill = playersByTimeInHill.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Time In Hill: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTimeInHill))
                            .append(Component.text(" (" + getTimeFormat(mostTimeInHill) + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time survived
            int mostHillCapturesValue = playersByTimesCapturedHill.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Hill Captures: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTimesCapturedHill))
                            .append(Component.text(" (" + mostHillCapturesValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for points defended
            int mostPointsDefended = playersByPointsDefended.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Points Defended: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByPointsDefended))
                            .append(Component.text(" (" + mostPointsDefended + ")").color(NamedTextColor.WHITE))
            );

        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
    }

    private @NotNull Component getComponent() {
        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<KOTHTeam> teamsByScore = new ArrayList<>(game.getTeamsByScore());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (KOTHTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getScore()).color(team.getColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text("-").color(NamedTextColor.WHITE));
            }
        }
        return finalScore;
    }

    @Override
    public ItemStack generateGameSummaryItem () {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getMapName(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().getTeamName(), game.getWinner().getColor());
        }
        // Add final score to lore
        Component teamScoreLore = Component.text("Score: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);

        List<KOTHTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(KOTHTeam::getScore));
        Collections.reverse(teamsByScore);

        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (KOTHTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getScore()).color(team.getColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            i++;
            if (i != teamsByScore.size()) {
                teamScoreLore = teamScoreLore.append(Component.text(" - ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
        }

        loreList.add(teamScoreLore);
        loreList.add(Component.text(" "));

        // Add game length
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {

        KOTHPlayer player = (KOTHPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.getNameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in statistics
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerTimeInHill = getPlayerStatObject(player, playersByTimeInHill);
        PlayerStatObject playerHillCaptures = getPlayerStatObject(player, playersByTimesCapturedHill);
        PlayerStatObject playerPointsDefended = getPlayerStatObject(player, playersByPointsDefended);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        // Show kills
        int position = playerKills.getPlacement();
        if (position > 0) {
            addLoreField(playerLoreList, "Kills", playerKills.getValue() +
                    " (#" + position + ")", NamedTextColor.GREEN);
        }

        // Show time in hill
        position = playerTimeInHill.getPlacement();
        if (position > 0) {
            addLoreField(playerLoreList, "Time In Hill", getTimeFormat(playerTimeInHill.getValue()) +
                    " (#" + position + ")", NamedTextColor.GREEN);
        }

        // Show times hill captured
        position = playerHillCaptures.getPlacement();
        if (position > 0) {
            addLoreField(playerLoreList, "Times Hill Captured", playerHillCaptures.getValue() +
                    " (#" + position + ")", NamedTextColor.GREEN);
        }

        // Show points defended
        position = playerPointsDefended.getPlacement();
        if (position > 0) {
            addLoreField(playerLoreList, "Points Defended", playerPointsDefended.getValue() +
                    " (#" + position + ")", NamedTextColor.GREEN);
        }

        addLoreBlankLine(playerLoreList);

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;
    }

    @Override
    public ItemStack generateTeamItem(CBCTeam rawTeam) {

        KOTHTeam team = (KOTHTeam) rawTeam;

        // Create item for team statistics
        ItemStack teamItem = team.getItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Add team statistics
        addLoreField(teamLoreList, "Points Scored", String.valueOf(team.getPointsScored()), NamedTextColor.GREEN);

        addLoreField(teamLoreList, "Times Hill Captured", getTimeFormat(team.getTimesHillCaptured()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Time Hill Controlled", getTimeFormat(team.getTotalSecondsHeldHill()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Max Time Hill Controlled", String.valueOf(team.getSecondsLongestHeldHill()), NamedTextColor.GREEN);

        int teamTotalKills = 0;
        for (CBCPlayer player : team.getPlayers()) {
            teamTotalKills += player.getKills();
        }

        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);

        return teamItem;

    }

    public Inventory createInventoryGui(Player user) {
        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.getPlayers()));
    }

}