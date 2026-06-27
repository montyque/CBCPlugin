package neonique.cbcplugin_new.gamemodes.crossbowtag;

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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TagPostGameStats extends PostGameStats {

    private final TagGame game;

    List<PlayerStatObject> playersByPointsScored;
    List<PlayerStatObject> playersByTimeSurvived;
    List<PlayerStatObject> playersByEvaderKills;
    List<PlayerStatObject> playersByKills;

    public TagPostGameStats (TagGame game) {
        this.game = game;

        // Sort players on statistics
        List<TagPlayer> playersList = game.getPlayers();

        if (!playersList.isEmpty()) {

            playersByPointsScored = new ArrayList<>();
            playersByTimeSurvived = new ArrayList<>();
            playersByEvaderKills = new ArrayList<>();
            playersByKills = new ArrayList<>();

            for (TagPlayer player : playersList) {

                // Add player's points scored
                playersByPointsScored.add(new PlayerStatObject(player, player.getIntPointsScored()));

                // Add player's time survived
                playersByTimeSurvived.add(new PlayerStatObject(player, player.getSecondsSurvived()));

                // Add player's evader kills
                playersByEvaderKills.add(new PlayerStatObject(player, player.getEvadersKilled()));

                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
            }

            // Sort players by kills and time alive
            playersByPointsScored = sortPlayerStatList(playersByPointsScored, true);
            playersByTimeSurvived = sortPlayerStatList(playersByTimeSurvived, true);
            playersByEvaderKills = sortPlayerStatList(playersByEvaderKills, true);
            playersByKills = sortPlayerStatList(playersByKills, true);
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

        // Sort players on statistics
        List<TagPlayer> playersList = game.getPlayers();
        if (!playersList.isEmpty()) {

            // Show which players were in first for points scored
            int mostPointsValue = playersByPointsScored.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Points Scored: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByPointsScored))
                            .append(Component.text(" (" + mostPointsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time survived
            int timeSurvivedValue = playersByTimeSurvived.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Time Survived: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByTimeSurvived))
                            .append(Component.text(" (" + getTimeFormat(timeSurvivedValue) + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for evaders kills
            int mostEvaderKills = playersByEvaderKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Evaders Killed: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByEvaderKills))
                            .append(Component.text(" (" + mostEvaderKills + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
    }

    private @NotNull Component getComponent() {
        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<TagTeam> teamsByScore = game.getTeamsByScore();

        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TagTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getIntScore()).color(team.textColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text("-").color(NamedTextColor.WHITE));
            }
        }
        return finalScore;
    }

    @Override
    public Inventory createInventoryGui (Player user) {
        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.getPlayers()));
    }

    @Override
    public ItemStack generateGameSummaryItem() {

        // Add general game stat item
        ItemStack gameSummaryItem = createGameSummaryItemStack(game);
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        // Adding lore fields
        List<Component> loreList = new ArrayList<>();
        addLoreField(loreList, "Map", game.getMap().getMapName(), NamedTextColor.GREEN);
        addLoreField(loreList, "Rounds", String.valueOf(game.getRoundNumber()), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().name(), game.getWinner().textColor());
        }

        // Add final score to lore
        Component teamScoreLore = Component.text("Score: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        List<TagTeam> teamsByScore = game.getTeamsByScore();
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TagTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getIntScore()).color(team.textColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            i++;
            if (i != teamsByScore.size()) {
                teamScoreLore = teamScoreLore.append(Component.text(" - ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
        }

        loreList.add(teamScoreLore);
        loreList.add(Component.text(" "));
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public ItemStack generatePlayerItem(CBCPlayer rawPlayer) {

        TagPlayer player = (TagPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.nameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in kills and time alive
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerPointsScored = getPlayerStatObject(player, playersByPointsScored);
        PlayerStatObject playerTimeSurvived = getPlayerStatObject(player, playersByTimeSurvived);
        PlayerStatObject playerEvadersKilled = getPlayerStatObject(player, playersByEvaderKills);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInPointsScored = playerPointsScored.getPlacement();
        if (positionInPointsScored > 0) {
            addLoreField(playerLoreList, "Points Scored", playerPointsScored.getValue() +
                    " (#" + positionInPointsScored + ")", NamedTextColor.GREEN);
        }

        int positionInTimeSurvived = playerTimeSurvived.getPlacement();
        if (positionInTimeSurvived > 0) {
            addLoreField(playerLoreList, "Time Survived", getTimeFormat(playerTimeSurvived.getValue()) +
                    " (#" + positionInTimeSurvived + ")", NamedTextColor.GREEN);
        }

        int positionInEvadersKilled = playerEvadersKilled.getPlacement();
        if (positionInEvadersKilled > 0) {
            addLoreField(playerLoreList, "Evaders Killed", playerEvadersKilled.getValue() +
                    " (#" + positionInEvadersKilled + ")", NamedTextColor.GREEN);
        }

        addLoreBlankLine(playerLoreList);
        addLoreField(playerLoreList, "Rounds Survived", player.getRoundsSurvived() + "/" + player.getRoundsAsEvaderPlayed(), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);

        return playerItem;

    }

    @Override
    public ItemStack generateTeamItem(CBCTeam<?> rawTeam) {

        TagTeam team = game.getTypedTeam(rawTeam);

        // Create item for team statistics
        ItemStack teamItem = team.getIconItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Add team statistics
        addLoreField(teamLoreList, "Score", String.valueOf(team.getIntScore()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Tagger Points", String.valueOf(team.getIntTaggerPoints()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Evader Points", String.valueOf(team.getIntEvaderPoints()), NamedTextColor.GREEN);

        int teamTotalKills = 0;
        for (TagPlayer player : team.players()) {
            teamTotalKills += player.getKills();
        }

        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);

        // Get list of teams (this is converting the CBC players to tag players)
        List<TagPlayer> teamPlayersList = new ArrayList<>(team.players());

        // Sort players by points scored
        List<TagPlayer> teamPlayersByPointsScored = new ArrayList<>(teamPlayersList);
        teamPlayersByPointsScored.sort(Comparator.comparingInt(TagPlayer::getIntPointsScored).reversed());

        // Sort players by time survived
        List<TagPlayer> teamPlayersByTimeSurvived = new ArrayList<>(teamPlayersList);
        teamPlayersByTimeSurvived.sort(Comparator.comparingInt(TagPlayer::getSecondsSurvived).reversed());

        // Sort players by time survived
        List<TagPlayer> teamPlayersByEvaderKills = new ArrayList<>(teamPlayersList);
        teamPlayersByEvaderKills.sort(Comparator.comparingInt(TagPlayer::getEvadersKilled).reversed());

        // Add team most kills and team most time alive
        if (!teamPlayersList.isEmpty()) {
            teamLoreList.add(Component.text(" "));

            TagPlayer mostPointsScoredPlayer = teamPlayersByPointsScored.get(0);
            addLoreField(teamLoreList, "Most Points Scored", mostPointsScoredPlayer.name()
                    + " (" + mostPointsScoredPlayer.getIntPointsScored() + ")", NamedTextColor.GREEN);

            TagPlayer mostTimeSurvivedPlayer = teamPlayersByTimeSurvived.get(0);
            addLoreField(teamLoreList, "Most Time Survived", mostTimeSurvivedPlayer.name()
                    + " (" + getTimeFormat(mostTimeSurvivedPlayer.getSecondsSurvived()) + ")", NamedTextColor.GREEN);

            TagPlayer mostEvadersKilledPlayer = teamPlayersByEvaderKills.get(0);
            addLoreField(teamLoreList, "Most Evaders Killed", mostEvadersKilledPlayer.name()
                    + " (" + mostEvadersKilledPlayer.getEvadersKilled() + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);

        return teamItem;

    }

}
