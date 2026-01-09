package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class PostGameStats {

    protected String mapName;
    protected int gameTimeLength;

    public abstract void sendPostGameSummary(Audience audience);

    public ItemStack generateGameSummaryItem () {
        return new ItemStack(Material.AIR);
    }

    public ItemStack generateTeamItem (CBCTeam<?> rawTeam) {
        return new ItemStack(Material.AIR);
    }

    public ItemStack generatePlayerItem (CBCPlayer rawPlayer) {
        return new ItemStack(Material.AIR);
    }

    public abstract Inventory createInventoryGui (Player user);

    public List<CBCPlayer> sortPlayers (List<CBCPlayer> playerList) {

        // Sort players on kills
        playerList.sort(Comparator.comparingInt(CBCPlayer::getKills));
        Collections.reverse(playerList);
        return playerList;

    };

    public Inventory inventoryGuiGenerate (Player user, boolean teamGame, List<CBCTeam<?>> teams, List<CBCPlayer> players) {

        // Create inventory
        Inventory inventory = Bukkit.createInventory(user, 54, Component.text("Post Game Statistics"));

        // Generate game summary item and put it in first slot
        ItemStack gameSummaryItem = generateGameSummaryItem();
        inventory.setItem(0, gameSummaryItem);

        // If this is a team based game:
        if (teamGame) {

            // Check whether to display teams horizontally or vertically
            // If there are more than 5 teams, display teams horizontally
            // If there are 5 teams or fewer, display teams vertically

            // Display teams vertically
            int teamNum = 1;

            for (CBCTeam<?> team : teams) {

                // Generate team statistical display item and put it in a slot
                ItemStack teamItem = generateTeamItem(team);
                if (teams.size() > 5) {
                    // Display teams horizontally
                    inventory.setItem(teamNum, teamItem);
                }
                else {
                    // Display teams vertically
                    inventory.setItem(teamNum * 9 , teamItem);
                }

                // Get players from team and sort them
                List<CBCPlayer> teamPlayers = new ArrayList<>(team.getPlayers());
                teamPlayers = sortPlayers(teamPlayers);

                // Go through each player and place them in the inventory list
                int teamPlayerNum = 1;
                for (CBCPlayer player : teamPlayers) {

                    // Generate player statistical display item and put it in a slot
                    ItemStack playerItem = generatePlayerItem(player);
                    if (teams.size() > 5) {
                        // Display team players vertically
                        inventory.setItem(teamPlayerNum * 9 + teamNum, playerItem);
                    }
                    else {
                        // Display team players horizontally
                        inventory.setItem(teamNum * 9 + teamPlayerNum, playerItem);
                    }

                    int teamPlayerNumMax = 8;
                    if (teams.size() > 5) {
                        teamPlayerNumMax = 5;
                    }

                    // If there are too many players to fit on one row, move the next players down a row
                    if (teamPlayerNum == teamPlayerNumMax) {
                        teamPlayerNum = 1;
                        // This is to move the next players down a row,
                        // and the next placed team down a row
                        teamNum++;
                    } else {
                        teamPlayerNum++;
                    }
                }
                teamNum++;
            }
        }

        // Not a team game, must be a free for all game
        else {

            int rowNum = 0;
            int columnNum = 1;
            List<CBCPlayer> ffaPlayers = sortPlayers(players);
            for (CBCPlayer player : ffaPlayers) {
                // Generate player statistical display item and put it in a slot
                ItemStack playerItem = generatePlayerItem(player);
                inventory.setItem(rowNum * 9 + columnNum, playerItem);
                // If there are too many players to fit on one row, move the next players down a row
                if (columnNum == 8) {
                    columnNum = 1;
                    rowNum++;
                } else {
                    columnNum++;
                }
            }
        }

        // Return inventory
        return inventory;
    }

    public void addLoreField (List<Component> loreList, String fieldName, String value, TextColor valueTextColor) {
        Component lore = Component.text(fieldName + ": ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                .append(Component.text(value).color(valueTextColor).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        loreList.add(lore);
    }

    public void addLoreBlankLine (List<Component> loreList) {
        loreList.add(Component.text(" "));
    }

    public Component createPlayerNameComponent(CBCPlayer player) {
        return player.getNameComponent();
    }

    public String getTimeFormat(int timeInSeconds) {
        return String.format("%d:%02d", timeInSeconds / 60, timeInSeconds % 60);
    }

    public List<PlayerStatObject> sortPlayerStatList (List<PlayerStatObject> list, boolean descending) {

        // Sort list
        if (descending) {
            list.sort(Comparator.comparingInt(PlayerStatObject::getValue).reversed());
        }
        else {
            list.sort(Comparator.comparingInt(PlayerStatObject::getValue));
        }

        // Set placements for statistics
        int currentValue = 0;
        int placement = 0;
        int i = 0;

        for (PlayerStatObject player : list) {

            boolean tied = false;

            boolean newPlacement;

            if (descending) {
                newPlacement = player.getValue() < currentValue;
            }
            else {
                newPlacement = player.getValue() > currentValue;
            }

            if (newPlacement || i == 0) {
                placement = i + 1;
                currentValue = player.getValue();
                if (list.size() - 1 != i) {
                    if (list.get(i + 1).getValue() == currentValue) {
                        tied = true;
                    }
                }
            }
            else if (currentValue == player.getValue()) {
                tied = true;
            }

            player.setPlacement(placement, tied);

            i++;

        }

        return list;
    }

    public List<PlayerStatObject> getPlayersInFirst (List<PlayerStatObject> list) {

        // Assuming list is already sorted
        List<PlayerStatObject> first = new ArrayList<>();

        if (list.isEmpty()) {
            return first;
        }

        // Set placements for statistics
        int firstValue = list.get(0).getValue();
        for (PlayerStatObject player : list) {
            if (player.getValue() == firstValue) {
                first.add(player);
            }
        }

        return first;

    }

    public List<CBCPlayer> getPlayersFromStatObject (List<PlayerStatObject> list) {

        List<CBCPlayer> players = new ArrayList<>();

        for (PlayerStatObject object : list) {
            players.add(object.getPlayer());
        }

        return players;

    }

    public Component getPlayerConcatenatedComponent (List<CBCPlayer> list) {

        Component component = Component.text("");
        int listSize = list.size();
        int i = 0;

        for (CBCPlayer player : list) {
            component = component.append(player.getNameComponent());
            if (listSize - 1 > i) {
                component = component.append(Component.text(", ").color(NamedTextColor.WHITE));
            }
            i++;
        }

        return component;

    }

    public String getPlayerConcatenatedString (List<CBCPlayer> list) {

        List<String> names = new ArrayList<>();
        for (CBCPlayer player : list) {
            names.add(player.getName());
        }

        return String.join(", ", names);

    }

    public Component getFirstPlaceComponent (List<PlayerStatObject> list) {

        return getPlayerConcatenatedComponent(getPlayersFromStatObject(getPlayersInFirst(list)));

    }

    public String getFirstPlaceString (List<PlayerStatObject> list) {

        return getPlayerConcatenatedString(getPlayersFromStatObject(getPlayersInFirst(list)));

    }

    public PlayerStatObject getPlayerStatObject (CBCPlayer player, List<PlayerStatObject> list) {

        for (PlayerStatObject object : list) {
            if (object.getPlayer() == player) {
                return object;
            }
        }

        return null;

    }

    public ItemStack createGameSummaryItemStack (Game<?, ?> game) {

        // Add general game stat item
        ItemStack gameSummaryItem = game.getGamemode().getGamemodeIconItem();
        ItemMeta gameSummaryMeta = gameSummaryItem.getItemMeta();

        gameSummaryMeta.displayName(
                Component.text("Game Summary").color(game.getGamemodeColor())
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE).decorate(TextDecoration.BOLD)
        );

        gameSummaryItem.setItemMeta(gameSummaryMeta);

        return gameSummaryItem;
    }
}
