package neonique.cbcplugin_new.gamemodes.showdown;

/*
import neonique.cbcplugin_new.core.CBCTeam;
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

public class ShowdownPostGameStats extends PostGameStats {

    private final ShowdownGame game;

    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByTimeAlive;

    public ShowdownPostGameStats (ShowdownGame game) {
        this.game = game;

        // Sort players on statistics
        List<ShowdownPlayer> playersList = game.players();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            playersByTimeAlive = new ArrayList<>();

            for (ShowdownPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's time alive
                playersByTimeAlive.add(new PlayerStatObject(player, player.getPlayerSecondsAlive()));
            }

            // Sort players by kills and time alive
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByTimeAlive = sortPlayerStatList(playersByTimeAlive, true);
        }
    }

    public void sendPostGameSummary(Audience audience) {

        audience.sendMessage(Component.text("Post Game Summary").color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED).append(Component.newline()));

        audience.sendMessage(
                Component.text("Game Length: ").color(NamedTextColor.WHITE)
                        .append(Component.text(getTimeFormat(game.getGameLength())).color(NamedTextColor.GREEN))
        );

        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<ShowdownTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(ShowdownTeam::getRoundsWon));
        Collections.reverse(teamsByScore);
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (ShowdownTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getRoundsWon()).color(team.textColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text("-").color(NamedTextColor.WHITE));
            }
        }

        audience.sendMessage(finalScore);

        // Sort players on statistics
        List<ShowdownPlayer> playersList = game.players();

        if (!playersList.isEmpty()) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time alive
            int mostTimeAliveValue = playersByTimeAlive.get(0).getValue();

            audience.sendMessage(
                    Component.text("Longest Time Alive: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTimeAlive))
                            .append(Component.text(" (" + getTimeFormat(mostTimeAliveValue) + ")").color(NamedTextColor.WHITE))
            );
        }

        audience.sendMessage(Component.newline().append(Component.text(
                "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
    }

    @Override
    public Inventory createInventoryGui (Player user) {
        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.players()));
    }

    @Override
    public ItemStack generateGameSummaryItem() {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().name(), NamedTextColor.GREEN);
        addLoreField(loreList, "Rounds", String.valueOf(game.getRoundNumber()), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().name(), game.getWinner().textColor());
        }
        // Add final score to lore
        Component teamScoreLore = Component.text("Score: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        List<ShowdownTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(ShowdownTeam::getRoundsWon));
        Collections.reverse(teamsByScore);
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (ShowdownTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getRoundsWon()).color(team.textColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            i++;
            if (i != teamsByScore.size()) {
                teamScoreLore = teamScoreLore.append(Component.text(" - ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
        }
        loreList.add(teamScoreLore);
        loreList.add(Component.text(" "));
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);
        int averageRoundTime = game.getGameInRoundTime() / game.getRoundNumber();
        addLoreField(loreList, "Average Round Length", getTimeFormat(averageRoundTime), NamedTextColor.GREEN);
        addLoreField(loreList, "Sudden Death Rounds", String.valueOf(game.getSuddenDeathRounds()), NamedTextColor.RED);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public ItemStack generatePlayerItem(CBCPlayer rawPlayer) {

        ShowdownPlayer player = game.getTypedPlayer(rawPlayer);

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.nameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in kills and time alive
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
    public ItemStack generateTeamItem(CBCTeam<?> rawTeam) {

        ShowdownTeam team = game.getTypedTeam(rawTeam);

        // Create item for team statistics
        ItemStack teamItem = team.getIconItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Add team statistics
        addLoreField(teamLoreList, "Rounds Won", String.valueOf(team.getRoundsWon()), NamedTextColor.GREEN);

        int teamTotalKills = 0;
        for (CBCPlayer player : team.players()) {
            teamTotalKills += player.getKills();
        }

        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);

        List<ShowdownPlayer> teamPlayersList = new ArrayList<>(team.players());

        // Sort players by kills
        List<ShowdownPlayer> teamPlayersByKills = new ArrayList<>(team.players());
        teamPlayersByKills.sort(Comparator.comparingInt(ShowdownPlayer::getKills).reversed());

        List<ShowdownPlayer> teamPlayersByTimeAlive = new ArrayList<>(team.players());
        teamPlayersByTimeAlive.sort(Comparator.comparingInt(ShowdownPlayer::getPlayerSecondsAlive).reversed());

        // Add team most kills and team most time alive
        if (!teamPlayersList.isEmpty()) {

            teamLoreList.add(Component.text(" "));
            ShowdownPlayer mostKillsPlayer = teamPlayersByKills.get(0);
            addLoreField(teamLoreList, "Most Kills", mostKillsPlayer.name()
                    + " (" + mostKillsPlayer.getKills() + ")", NamedTextColor.GREEN);

            ShowdownPlayer mostTimeAlivePlayer = teamPlayersByTimeAlive.get(0);
            addLoreField(teamLoreList, "Most Time Alive", mostTimeAlivePlayer.name()
                    + " (" + getTimeFormat(mostTimeAlivePlayer.getPlayerSecondsAlive()) + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);

        return teamItem;

    }
}
*/