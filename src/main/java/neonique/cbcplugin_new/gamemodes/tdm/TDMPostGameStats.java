package neonique.cbcplugin_new.gamemodes.tdm;

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

public class TDMPostGameStats extends PostGameStats {

    private final TDMGame game;

    List<TDMPlayer> playersList;
    List<PlayerStatObject> playersByKills;

    public TDMPostGameStats (TDMGame game) {
        this.game = game;

        // Sort players on statistics
        playersList = game.getPlayers();

        if (!playersList.isEmpty()) {

            playersByKills = new ArrayList<>();
            for (TDMPlayer player : playersList) {
                // Add player's kills
                playersByKills.add(new PlayerStatObject(player, player.getKills()));
            }

            // Sort players by kills and time alive
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

        Component finalScore = Component.text("Final Score: ").color(NamedTextColor.WHITE);
        List<TDMTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(TDMTeam::getKills));
        Collections.reverse(teamsByScore);
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TDMTeam team : teamsByScore) {
            finalScore = finalScore.append(Component.text(team.getKills()).color(team.textColor()));
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
        addLoreField(loreList, "Map", game.getMap().getName(), NamedTextColor.GREEN);
        if (game.getWinner() != null) {
            addLoreField(loreList, "Winner", game.getWinner().name(), game.getWinner().textColor());
        }
        // Add final score to lore
        Component teamScoreLore = Component.text("Score: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        List<TDMTeam> teamsByScore = new ArrayList<>(game.getTeams());
        teamsByScore.sort(Comparator.comparingInt(TDMTeam::getKills));
        Collections.reverse(teamsByScore);
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TDMTeam team : teamsByScore) {
            teamScoreLore = teamScoreLore.append(Component.text(team.getKills()).color(team.textColor()).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
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
    public ItemStack generateTeamItem (CBCTeam<?> rawTeam) {

        TDMTeam team = game.getTypedTeam(rawTeam);

        // Create item for team statistics
        ItemStack teamItem = team.getIconItem().clone();
        ItemMeta teamItemMeta = teamItem.getItemMeta();
        List<Component> teamLoreList = new ArrayList<>();

        int teamTotalKills = 0;
        for (CBCPlayer player : team.players()) {
            teamTotalKills += player.getKills();
        }

        addLoreField(teamLoreList, "Total Kills", String.valueOf(teamTotalKills), NamedTextColor.GREEN);

        // Sort players by kills
        List<TDMPlayer> teamPlayersByKills = new ArrayList<>(team.players());
        teamPlayersByKills.sort(Comparator.comparingInt(TDMPlayer::getKills).reversed());

        // Add team most kills and team most time alive
        if (!teamPlayersByKills.isEmpty()) {
            addLoreBlankLine(teamLoreList);
            TDMPlayer mostKillsPlayer = teamPlayersByKills.get(0);
            addLoreField(teamLoreList, "Most Kills", mostKillsPlayer.name()
                    + " (" + mostKillsPlayer.getKills() + ")", NamedTextColor.GREEN);
        }

        // Set the item lore and the item meta then add item to inventory
        teamItemMeta.lore(teamLoreList);
        teamItem.setItemMeta(teamItemMeta);
        return teamItem;
    }

    @Override
    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {
        TDMPlayer player = (TDMPlayer) rawPlayer;

        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerItemMeta = (SkullMeta) playerItem.getItemMeta();

        playerItemMeta.setOwningPlayer(player.getOfflinePlayer());

        playerItemMeta.displayName(
                player.nameComponent().decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
        );

        List<Component> playerLoreList = new ArrayList<>();

        PlayerStatObject playerKills = getPlayerStatObject(player, playersByKills);

        int positionInKills = playerKills.getPlacement();
        if (positionInKills > 0) {
            addLoreField(playerLoreList, "Kills", player.getKills() + " (#" + positionInKills + ")", NamedTextColor.GREEN);
        }
        addLoreField(playerLoreList, "Highest Kill Streak", player.getMaxKillStreak() + "", NamedTextColor.GREEN);
        addLoreField(playerLoreList, "Deaths", player.getDeaths() + "", NamedTextColor.RED);

        double kdRatio = (double) player.getKills() / (double) player.getDeaths();

        addLoreField(playerLoreList, "KD Ratio", String.format("%.2f", kdRatio), NamedTextColor.GREEN);

        // Set the item lore and the item meta then add item to inventory
        playerItemMeta.lore(playerLoreList);
        playerItem.setItemMeta(playerItemMeta);
        return playerItem;
    }

    public Inventory createInventoryGui(Player user) {
        return inventoryGuiGenerate(user, true,
                new ArrayList<>(game.getTeams()), new ArrayList<>(game.getPlayers()));
    }

}
