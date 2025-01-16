package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlagRushPostGameStats extends PostGameStats {

    private final FlagRushGame game;

    List<FlagRushPlayer> playersList;
    List<PlayerStatObject> playersByKills;
    List<PlayerStatObject> playersByDKills;
    List<PlayerStatObject> playersByFlagsCaptured;

    public FlagRushPostGameStats (FlagRushGame game) {
        this.game = game;

        // Sort players on statistics
        playersList = new ArrayList<>();
        for (CBCPlayer player : game.getPlayers().values()) {
            if (player instanceof FlagRushPlayer) {
                playersList.add((FlagRushPlayer) player);
            }
        }

        if (playersList.size() > 0) {

            playersByKills = new ArrayList<>();
            playersByDKills = new ArrayList<>();
            playersByFlagsCaptured = new ArrayList<>();

            for (FlagRushPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
                // Add player's defensive kills
                playersByDKills.add(new PlayerStatObject(player, player.getDefensiveKills()));
                // Add player's flags captured
                playersByFlagsCaptured.add(new PlayerStatObject(player, player.getFlagsCaptured()));
            }

            // Sort players by kills and time alive
            playersByKills = sortPlayerStatList(playersByKills, true);
            playersByDKills = sortPlayerStatList(playersByDKills, true);
            playersByFlagsCaptured = sortPlayerStatList(playersByFlagsCaptured, true);
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

        if (playersList.size() > 0) {

            // Show which players were in first for kills
            int mostKillsValue = playersByKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByKills))
                            .append(Component.text(" (" + mostKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for defensive kills
            int mostDKillsValue = playersByDKills.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Defensive Kills: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByDKills))
                            .append(Component.text(" (" + mostDKillsValue + ")").color(NamedTextColor.WHITE))
            );

            // Show which players were in first for flags captured
            int mostFlagsCaptValue = playersByFlagsCaptured.get(0).getValue();

            audience.sendMessage(
                    Component.text("Most Flags Captured: ").color(NamedTextColor.WHITE)
                            .append(getFirstPlaceComponent(playersByFlagsCaptured))
                            .append(Component.text(" (" + mostFlagsCaptValue + ")").color(NamedTextColor.WHITE))
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
        List<FlagRushTeam> teamsByScore = new ArrayList<>(game.getFlagRushTeams());
        teamsByScore.sort(Comparator.comparingInt(FlagRushTeam::getScore));
        Collections.reverse(teamsByScore);
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (FlagRushTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getScore()).color(team.getColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            i++;
            if (i != teamsByScore.size()) {
                teamScoreLore = teamScoreLore.append(Component.text(" - ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
        }

        addLoreBlankLine(loreList);
        addLoreField(loreList, "Game Length", getTimeFormat(game.getGameLength()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        gameSummaryMeta.lore(loreList);
        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;

    }

    @Override
    public ItemStack generateTeamItem (CBCTeam rawTeam) {

        CTFTeam team = (CTFTeam) rawTeam;

        // Create item for team statistics
        ItemStack teamItem = team.getItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        // Find team's total kills and total defensive kills

        int teamTotalKills = 0;
        int teamTotalDKills = 0;
        int teamTotalCaptures = 0;
        for (CBCPlayer player : team.getPlayers()) {
            teamTotalKills += player.getKills();
            teamTotalDKills += ((CTFPlayer) player).getDefensiveKills();
            teamTotalCaptures += ((CTFPlayer) player).getFlagsCaptured();
        }

        // Add team statistics
        addLoreField(teamLoreList, "Time In Game", getTimeFormat(team.getTeamTimeAlive()), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Defensive Kills", String.valueOf(teamTotalDKills), NamedTextColor.GREEN);
        addLoreField(teamLoreList, "Total Captures", String.valueOf(teamTotalCaptures), NamedTextColor.GREEN);
        addLoreBlankLine(teamLoreList);

        addLoreField(teamLoreList, "Times Flag Picked Up", String.valueOf(team.getTimesFlagPickedUp()), NamedTextColor.RED);
        addLoreField(teamLoreList, "Times Flag Captured", String.valueOf(team.getTimesFlagCaptured()), NamedTextColor.RED);

        if (team.getTimesFlagPickedUp() > 0) {
            int flagHolderKillPercentage = Math.round(
                    (
                            (float) (team.getTimesFlagPickedUp() - team.getTimesFlagCaptured())
                                    / (float) team.getTimesFlagPickedUp()
                    ) * 100);
            addLoreField(teamLoreList, "Flag-holder Kill %", flagHolderKillPercentage + "%", NamedTextColor.GREEN);
        }

        // Get list of teams (this is converting the CBC players to showdown players)
        List<CTFPlayer> teamPlayersList = new ArrayList<>();
        for (CBCPlayer player : team.getPlayers()) {
            if (player instanceof CTFPlayer) {
                teamPlayersList.add((CTFPlayer) player);
            }
        }

        addLoreBlankLine(teamLoreList);

        // Sort team players on statistics
        List<CTFPlayer> teamPlayersByKills = new ArrayList<>(teamPlayersList);
        teamPlayersByKills.sort(Comparator.comparingInt(CTFPlayer::getKills).reversed());

        List<CTFPlayer> teamPlayersByDKills = new ArrayList<>(teamPlayersList);
        teamPlayersByDKills.sort(Comparator.comparingInt(CTFPlayer::getDefensiveKills).reversed());

        List<CTFPlayer> teamPlayersByFlagsCaptured = new ArrayList<>(teamPlayersList);
        teamPlayersByFlagsCaptured.sort(Comparator.comparingInt(CTFPlayer::getFlagsCaptured).reversed());

        if (teamPlayersList.size() > 0) {
            // Add team most kills and team most time alive
            CTFPlayer mostKillsPlayer = teamPlayersByKills.get(0);
            addLoreField(teamLoreList, "Most Kills", mostKillsPlayer.getName()
                    + " (" + mostKillsPlayer.getKills() + ")", NamedTextColor.GREEN);

            // Add team most kills and team most time alive
            CTFPlayer mostDKillsPlayer = teamPlayersByDKills.get(0);
            addLoreField(teamLoreList, "Most Defensive Kills", mostDKillsPlayer.getName()
                    + " (" + mostDKillsPlayer.getDefensiveKills() + ")", NamedTextColor.GREEN);

            // Add team most kills and team most time alive
            CTFPlayer mostFlagsCaptPlayer = teamPlayersByFlagsCaptured.get(0);
            addLoreField(teamLoreList, "Most Flags Captured", mostFlagsCaptPlayer.getName()
                    + " (" + mostFlagsCaptPlayer.getFlagsCaptured() + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);
        return teamItem;
    }

    @Override
    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {

        CTFPlayer player = (CTFPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.getNameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        // Get player's position in stats
        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);
        PlayerStatObject playerDKills = getPlayerStatObject(player, playersByDKills);
        PlayerStatObject playerFlagsCapt = getPlayerStatObject(player, playersByFlagsCaptured);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", String.valueOf(player.getMaxKillStreak()), NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", String.valueOf(player.getDeaths()), NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        addLoreBlankLine(playerLoreList);

        int positionInDKills = playerDKills.getPlacement();
        if (positionInDKills > 0) {
            addLoreField(playerLoreList, "Defensive Kills", player.getDefensiveKills() +
                    " (#" + positionInDKills + ")", NamedTextColor.GREEN);
        }

        addLoreField(playerLoreList, "Flags Picked Up", String.valueOf(player.getFlagsPickedUp()), NamedTextColor.GREEN);

        int positionInFlagsCapt = playerFlagsCapt.getPlacement();
        if (positionInFlagsCapt > 0) {
            addLoreField(playerLoreList, "Flags Captured", player.getFlagsCaptured() +
                    " (#" + positionInFlagsCapt + ")", NamedTextColor.GREEN);
        }

        if (player.getFlagsPickedUp() > 0) {
            int captureSuccessPercentage = Math.round(((float) (player.getFlagsCaptured()) / (float) player.getFlagsPickedUp()) * 100);
            addLoreField(playerLoreList, "Capture Success %", captureSuccessPercentage + "%", NamedTextColor.GREEN);
        }

        addLoreField(playerLoreList, "Morale Boosts Given", String.valueOf(player.getmBoostsGiven()), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;

    }

    @Override
    public Inventory createInventoryGui (Player user) {

        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getFlagRushTeams()), new ArrayList<>(game.getPlayers().values()));
    }

}
