package neonique.cbcplugin_new.commands_old.cbceventsubcommands;

import neonique.cbcplugin_new.cbcevents.CBCEventTeam;
import neonique.cbcplugin_new.commands_old.CBCEventCommand;
import neonique.cbcplugin_new.commands_old._SubCommand;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.cbcevents.CBCEventPlayer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class cbcevent_player extends _SubCommand {

    public static void run(CBCEventCommand eventCommand, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a sub-sub command!").color(NamedTextColor.YELLOW));
            return;
        }

        GameManager gameManager = eventCommand.getGameManager();
        if (!gameManager.isCBCEventActive()) return;
        CBCEventManager eventManager = gameManager.getEventManager();

        String subsubcommand = args[1].toLowerCase();

        // Getting or adding players to the game
        if (subsubcommand.equals("add") || subsubcommand.equals("get")) {

            if (args.length < 3) {
                user.sendMessage(Component.text("You must include a player name!").color(NamedTextColor.YELLOW));
                return;
            }

            // Get player name
            Player player = getPlayerOrError(args[2], user);
            if (player == null) return;

            // Adding players to the game
            if (subsubcommand.equals("add")) {
                // Check if player already in
                if (eventManager.isPlayer(player)) {
                    user.sendMessage(Component.text("This player has already been added!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Add player to the event
                eventManager.addPlayer(player);
                user.sendMessage(Component.text("Added player ").color(NamedTextColor.GREEN).append(
                        Component.text(player.getName()).color(NamedTextColor.AQUA)
                ).append(
                        Component.text(" to the event!").color(NamedTextColor.GREEN)
                ));
            }
            // Getting player's team and points if they are in the game
            else {
                // Check if player already in
                if (!eventManager.isPlayer(player)) {
                    user.sendMessage(Component.text("This player is not registered in the event!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get player
                CBCEventPlayer eventPlayer = eventManager.getPlayer(player.getUniqueId());
                if (eventPlayer == null) return;

                // Tell user stats
                user.sendMessage(Component.text("Player retrieved: ").color(NamedTextColor.AQUA).append(
                        eventPlayer.getNameComponent()
                ).append(
                        eventManager.getPointsIconComponent()
                ).append(
                        Component.text(eventPlayer.getEventScore()).color(NamedTextColor.YELLOW)
                ));
            }
        }

        // List players command
        if (subsubcommand.equals("list")) {

            // Check if event has more than 0 players
            int playerAmount = eventManager.getEventPlayers().size();
            if (playerAmount == 0) {
                user.sendMessage(Component.text("There are currently no players registered in the event!").color(NamedTextColor.YELLOW));
                return;
            }

            // Check if list teams by team or list teams by score
            boolean listTeamsByScore = false;

            if (args.length >= 3) {
                if (args[2].equalsIgnoreCase("byscore")) {
                    listTeamsByScore = true;
                }
            }

            List<CBCEventPlayer> playersToList = new ArrayList<>();

            Component listComponent = Component.text("List of " + playerAmount + " players registered in the event:").color(NamedTextColor.GOLD);

            if (listTeamsByScore) {
                // List teams by score
                playersToList = eventManager.getEventPlayersSortedByScore();
            }
            else {
                // List teams by team color and then score
                for (CBCEventTeam team : eventManager.getTeams()) {
                    playersToList.addAll(eventManager.getEventPlayersOnTeamSortedByScore(team));
                }
            }

            // Go through players to list and list players
            for (CBCEventPlayer eventPlayer : playersToList) {

                // Add space to make names even
                String playerName = eventPlayer.getName();
                int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
                Component spaceComponent = getComponentSpaceOfLength(150 - playerNameLength);

                listComponent = listComponent.append(Component.newline());
                listComponent = listComponent.append(eventPlayer.getNameComponent().append(
                                spaceComponent
                        ).append(
                                eventManager.getPointsIconComponent()
                        ).append(
                                Component.text(eventPlayer.getEventScore()).color(NamedTextColor.YELLOW)
                        )
                );
            }

            user.sendMessage(listComponent);
        }
    }

    public static List<String> getTabCompletions (CBCEventCommand eventCommand, String[] args, int perms) {

        List<String> tabCompletions = new ArrayList<>();

        if (args.length < 2) {
            return tabCompletions;
        }

        int level = args.length;

        GameManager gameManager = eventCommand.getGameManager();
        if (!gameManager.isCBCEventActive()) return tabCompletions;
        CBCEventManager eventManager = gameManager.getEventManager();

        if (level == 2) {
            tabCompletions.add("add");
            tabCompletions.add("get");
            tabCompletions.add("list");
        }
        else {
            String subsubcommand = args[1].toLowerCase();
            if (subsubcommand.equals("add")) {
                if (level == 3) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!eventManager.hasPlayer(player.getUniqueId())) {
                            tabCompletions.add(player.getName());
                        }
                    }
                }
            } else if (subsubcommand.equals("get")) {
                if (level == 3) {
                    for (CBCEventPlayer eventPlayer : eventManager.getEventPlayers()) {
                        tabCompletions.add(eventPlayer.getName());
                    }
                }
            } else if (subsubcommand.equals("list")) {
                if (level == 3) {
                    tabCompletions.add("byTeam");
                    tabCompletions.add("byScore");
                }
            }
        }

        return tabCompletions;
    }

}
