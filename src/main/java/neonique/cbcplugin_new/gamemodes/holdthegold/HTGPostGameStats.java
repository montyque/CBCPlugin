package neonique.cbcplugin_new.gamemodes.holdthegold;

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

public class HTGPostGameStats extends PostGameStats {

    private final HTGGame game;

    // Sort players on statistics
    List<HTGPlayer> playersList;
    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByGoldScore;

    public HTGPostGameStats (HTGGame HTGGame) {

        this.game = HTGGame;

        // Sort players on statistics
        playersList = game.getPlayers();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            playersByGoldScore = new ArrayList<>();

            for (HTGPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's gold score
                playersByGoldScore.add(new PlayerStatObject(player, player.getGoldScore()));
            }

            // Sort players by kills and time alive
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByGoldScore = sortPlayerStatList(playersByGoldScore, true);
        }
    }

    public ItemStack generateGameSummaryItem () {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getMapName(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().name(), game.getWinner().textColor());
        }

        // Get final score
        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.GRAY);
        List<HTGTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(HTGTeam::getScore));
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (HTGTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getScore()).color(team.textColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text(" - ").color(NamedTextColor.WHITE));
            }
        }

        loreList.add(finalScore.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        addLoreBlankLine(loreList);
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;
    }

    public ItemStack generateTeamItem (CBCTeam<?> rawTeam) {

        HTGTeam team = game.getTypedTeam(rawTeam);

        // Create item for team statistics
        ItemStack teamItem = team.getIconItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Find team's total kills and total defensive kills
        int teamTotalKills = 0;
        for (CBCPlayer player : team.players()) {
            teamTotalKills += player.getKills();
        }

        // Add team statistics
        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Gold Score", String.valueOf(team.getGoldScore()), NamedTextColor.GREEN);
        addLoreBlankLine(teamLoreList);

        List<HTGPlayer> teamPlayersList = new ArrayList<>(team.players());

        addLoreBlankLine(teamLoreList);

        // Sort team players on statistics
        List<HTGPlayer> teamPlayersByKills = new ArrayList<>(teamPlayersList);
        teamPlayersByKills.sort(Comparator.comparingInt(HTGPlayer::getKills));
        Collections.reverse(teamPlayersByKills);

        List<HTGPlayer> teamPlayersByGoldScore = new ArrayList<>(teamPlayersList);
        teamPlayersByGoldScore.sort(Comparator.comparingInt(HTGPlayer::getGoldScore));
        Collections.reverse(teamPlayersByGoldScore);

        // Add team most kills and team most time alive
        if (!teamPlayersList.isEmpty()) {
            HTGPlayer mostKillsPlayer = teamPlayersByKills.get(0);
            addLoreField(teamLoreList, "Most Kills", mostKillsPlayer.name()
                    + " (" + mostKillsPlayer.getKills() + ")", NamedTextColor.GREEN);

            // Add team most kills and team most time alive
            HTGPlayer mostGoldScorePlayer = teamPlayersByGoldScore.get(0);
            addLoreField(teamLoreList, "Most Gold Score", mostGoldScorePlayer.name()
                    + " (" + mostGoldScorePlayer.getGoldScore() + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);
        return teamItem;

    }

    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {

        HTGPlayer player = (HTGPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.nameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in kills and time alive
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerGoldScore = getPlayerStatObject(player, playersByGoldScore);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInGoldScore = playerGoldScore.getPlacement();
        if (positionInGoldScore > 0) {
            addLoreField(playerLoreList, "Gold Score", player.getGoldScore() +
                    " (#" + positionInGoldScore + ")", NamedTextColor.GREEN);
        }

        addLoreField(playerLoreList, "Times Picked Up Gold", String.valueOf(player.getTimesGoldPickedUp()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Gold Holders Killed", String.valueOf(player.getGoldHoldersKilled()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;
    }

    @Override
    public void sendPostGameSummary(Audience audience) {

        audience.sendMessage(Component.text("Post Game Summary").color(NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED).append(Component.newline()));

        audience.sendMessage(
                Component.text("Game Length: ").color(NamedTextColor.WHITE)
                        .append(Component.text(getTimeFormat(game.getGameLength())).color(NamedTextColor.GREEN)));

        // Get final score
        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<HTGTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(HTGTeam::getScore));
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (HTGTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getScore()).color(team.textColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text("-").color(NamedTextColor.WHITE));
            }
        }

        audience.sendMessage(finalScore);

        // Sort players on statistics
        if (!playersList.isEmpty()) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time alive
            int mostGoldScoreValue = playersByGoldScore.get(0).getValue();

            audience.sendMessage(
                    Component.text("Highest Gold Score: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByGoldScore))
                            .append(Component.text(" (" + mostGoldScoreValue + ")").color(NamedTextColor.WHITE))
            );
        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
    }

    @Override
    public Inventory createInventoryGui(Player user) {
        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.getPlayers()));
    }
}