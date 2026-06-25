package neonique.cbcplugin_new.gamemodes.rendezvous;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RendezvousPostGameStats extends PostGameStats {

    private final RendezvousGame game;

    List<RendezvousPlayer> playersList;
    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByCheckpoints;
    List<PlayerStatObject> playersByRunnersKilled;
    List<PlayerStatObject> playersByMoraleBoostsGiven;

    public RendezvousPostGameStats (RendezvousGame game) {
        this.game = game;

        // Sort players on statistics
        playersList = game.getPlayers();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            playersByCheckpoints = new ArrayList<>();
            playersByRunnersKilled = new ArrayList<>();
            playersByMoraleBoostsGiven = new ArrayList<>();

            for (RendezvousPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's checkpoints cleared
                playersByCheckpoints.add(new PlayerStatObject(player, player.getCheckpointsCleared()));
                // Add player's enemy runners killed
                playersByRunnersKilled.add(new PlayerStatObject(player, player.getEnemyRunnersKilled()));
                // Add player's morale boosts given
                playersByMoraleBoostsGiven.add(new PlayerStatObject(player, player.getMoraleBoostsGiven()));
            }

            // Sort players by kills
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByCheckpoints = sortPlayerStatList(playersByCheckpoints, true);
            playersByRunnersKilled = sortPlayerStatList(playersByRunnersKilled, true);
            playersByMoraleBoostsGiven = sortPlayerStatList(playersByMoraleBoostsGiven, true);

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

        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<RendezvousTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(RendezvousTeam::getScore));
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (RendezvousTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getScore()).color(team.getColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(Component.text("-").color(NamedTextColor.WHITE));
            }
        }

        audience.sendMessage(finalScore);

        if (!playersList.isEmpty()) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for time alive
            int mostCheckpointsValue = playersByCheckpoints.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Checkpoints: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByCheckpoints))
                            .append(Component.text(" (" + mostCheckpointsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for runner kills
            int mostRunnersKilledValue = playersByRunnersKilled.get(0).getValue();
            audience.sendMessage(
                    Component.text("Most Runner Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByRunnersKilled))
                            .append(Component.text(" (" + mostRunnersKilledValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for morale boosts given
            int mostMoraleBoostsGivenValue = playersByMoraleBoostsGiven.get(0).getValue();
            audience.sendMessage(
                    Component.text("Most Morale Boosts Given: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByMoraleBoostsGiven))
                            .append(Component.text(" (" + mostMoraleBoostsGivenValue + ")").color(NamedTextColor.WHITE))
            );
        }

        audience.sendMessage(Component.newline().append(Component.text(
                        "In order to see more post game statistics, use the command /game lastgamestats!")
                .color(NamedTextColor.YELLOW))
        );
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
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        List<RendezvousTeam> teamsByScore = game.getTeamsByScore();
        for (RendezvousTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getScore()).color(team.getColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            i++;
            if (i != teamsByScore.size()) {
                teamScoreLore = teamScoreLore.append(Component.text(" - ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
        }

        addLoreBlankLine(loreList);
        addLoreField(loreList, "Game Length", String.format("%d:%02d", game.getGameLength() / 60, game.getGameLength() & 60), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);
        return gameSummaryItem;

    }

    @Override
    public ItemStack generateTeamItem (CBCTeam<?> rawTeam) {

        RendezvousTeam team = game.getTypedTeam(rawTeam);

        // Create item for team statistics
        ItemStack teamItem = team.getItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Find team's total kills and total defensive kills

        int teamTotalKills = 0;
        int teamTotalEnemyRunnersKilled = 0;
        int teamTotalMoraleBoostsGiven = 0;
        for (RendezvousPlayer player : team.getPlayers()) {
            teamTotalKills += player.getKills();
            teamTotalEnemyRunnersKilled += player.getEnemyRunnersKilled();
            teamTotalMoraleBoostsGiven += player.getMoraleBoostsGiven();
        }

        // Add team statistics
        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Enemy Runner Kills", String.valueOf(teamTotalEnemyRunnersKilled), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Morale Boosts Given", String.valueOf(teamTotalMoraleBoostsGiven), NamedTextColor.GREEN);
        addLoreBlankLine(teamLoreList);

        addLoreField(teamLoreList, "Checkpoints Cleared", String.valueOf(team.getCheckpointsCleared()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Runner Deaths", String.valueOf(team.getRunnerDeaths()), NamedTextColor.RED);

        // Get list of teams (this is converting the CBC players to Rendezvous players)
        List<RendezvousPlayer> teamPlayersList = new ArrayList<>(team.getPlayers());

        addLoreBlankLine(teamLoreList);

        // Sort team players on statistics
        List<RendezvousPlayer> teamPlayersByKills = new ArrayList<>(teamPlayersList);
        teamPlayersByKills.sort(Comparator.comparingInt(RendezvousPlayer::getKills).reversed());

        List<RendezvousPlayer> teamPlayersByCheckpointsCleared = new ArrayList<>(teamPlayersList);
        teamPlayersByCheckpointsCleared.sort(Comparator.comparingInt(RendezvousPlayer::getCheckpointsCleared).reversed());

        List<RendezvousPlayer> teamPlayersByEnemyRunnersKilled = new ArrayList<>(teamPlayersList);
        teamPlayersByEnemyRunnersKilled.sort(Comparator.comparingInt(RendezvousPlayer::getEnemyRunnersKilled).reversed());

        if (!teamPlayersList.isEmpty()) {
            // Add team most kills and team most time alive
            RendezvousPlayer mostKillsPlayer = teamPlayersByKills.get(0);
            addLoreField(teamLoreList, "Most Kills", mostKillsPlayer.getName()
                    + " (" + mostKillsPlayer.getKills() + ")", NamedTextColor.GREEN);

            // Add team most kills and team most time alive
            RendezvousPlayer mostCheckpointsClearedPlayer = teamPlayersByCheckpointsCleared.get(0);
            addLoreField(teamLoreList, "Most Checkpoints Cleared", mostCheckpointsClearedPlayer.getName()
                    + " (" + mostCheckpointsClearedPlayer.getCheckpointsCleared() + ")", NamedTextColor.GREEN);

            // Add team most kills and team most time alive
            RendezvousPlayer mostEnemyRunnersKilledPlayer = teamPlayersByEnemyRunnersKilled.get(0);
            addLoreField(teamLoreList, "Most Enemy Runners Killed", mostEnemyRunnersKilledPlayer.getName()
                    + " (" + mostEnemyRunnersKilledPlayer.getEnemyRunnersKilled() + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);
        return teamItem;
    }

    @Override
    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {

        RendezvousPlayer player = game.getTypedPlayer(rawPlayer);

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.getNameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in stats
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerCheckpoints = getPlayerStatObject(player, playersByCheckpoints);
        PlayerStatObject playerEnemyRunnersKilled = getPlayerStatObject(player, playersByRunnersKilled);
        PlayerStatObject playerMoraleBoostsGiven = getPlayerStatObject(player, playersByMoraleBoostsGiven);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInCheckpoints = playerCheckpoints.getPlacement();
        if (positionInCheckpoints > 0) {
            addLoreField(playerLoreList, "Checkpoints Cleared", player.getCheckpointsCleared() +
                    " (#" + positionInCheckpoints + ")", NamedTextColor.GREEN);
        }

        int positionInEnemyRunnersKilled = playerEnemyRunnersKilled.getPlacement();
        if (positionInEnemyRunnersKilled > 0) {
            addLoreField(playerLoreList, "Enemy Runners Killed", player.getEnemyRunnersKilled() +
                    " (#" + positionInEnemyRunnersKilled + ")", NamedTextColor.GREEN);
        }

        int positionInMoraleBoostsGiven = playerMoraleBoostsGiven.getPlacement();
        if (positionInMoraleBoostsGiven > 0) {
            addLoreField(playerLoreList, "Morale Boosts Given", playerMoraleBoostsGiven.getValue() +
                    " (#" + positionInMoraleBoostsGiven + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;

    }

    @Override
    public Inventory createInventoryGui (Player user) {

        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.getPlayers()));

    }

}
