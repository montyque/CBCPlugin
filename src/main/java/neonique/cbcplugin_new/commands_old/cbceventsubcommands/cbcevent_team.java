package neonique.cbcplugin_new.commands_old.cbceventsubcommands;

import neonique.cbcplugin_new.cbcevents.CBCEventTeam;
import neonique.cbcplugin_new.commands_old.CBCEventCommand;
import neonique.cbcplugin_new.commands_old._SubCommand;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.cbcevents.CBCEventPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static neonique.cbcplugin_new.util.StringUtil.firstLetterUpper;

public class cbcevent_team extends _SubCommand {

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
        switch (subsubcommand) {
            case "join" -> {

                if (args.length < 4) {
                    user.sendMessage(Component.text("You must include a player name and a team!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get player name
                Player player = getPlayerOrError(args[2], user);
                if (player == null) return;

                // Check if player already in, and if not, register player in event
                if (!eventManager.isPlayer(player)) {
                    eventManager.addPlayer(player);
                    user.sendMessage(Component.text("Registered player into the event!").color(NamedTextColor.YELLOW));
                }

                // Get player and add them to team
                CBCEventPlayer eventPlayer = eventManager.getPlayer(player.getUniqueId());
                if (eventPlayer == null) return;

                // Get team
                String teamIdString = args[3].toLowerCase();
                CBCEventTeam team = eventManager.getTeam(teamIdString);

                if (team == null) {
                    user.sendMessage(Component.text("Team with id '" + teamIdString + "' is not registered in this event!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Set player's team
                eventManager.setPlayerTeam(eventPlayer, team);

                // Add player to team
                user.sendMessage(Component.text("Added player ").color(NamedTextColor.AQUA).append(
                        eventPlayer.getNameComponent()
                ).append(
                        Component.text(" to ").color(NamedTextColor.AQUA)
                ).append(
                        team.getNameComponent(true)
                ).append(
                        Component.text("!").color(NamedTextColor.AQUA)
                ));
            }

            case "leave" -> {

                // Remove player from team
                if (args.length < 3) {
                    user.sendMessage(Component.text("You must include a player name!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get player name
                Player player = getPlayerOrError(args[2], user);
                if (player == null) return;

                // Check if player not registered in event
                CBCEventPlayer eventPlayer = eventManager.getPlayer(player.getUniqueId());
                if (eventPlayer == null) {
                    user.sendMessage(Component.text("This player is not registered in the event!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Check if player has a team
                CBCEventTeam team = eventPlayer.getTeam();
                if (team == null) {
                    user.sendMessage(Component.text("This player is not registered in an event team!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Remove player from team
                team.removePlayer(eventPlayer);

                user.sendMessage(Component.text("Removed player ").color(NamedTextColor.AQUA).append(
                        eventPlayer.getNameComponent()
                ).append(
                        Component.text(" from ").color(NamedTextColor.AQUA)
                ).append(
                        team.getNameComponent(true)
                ).append(
                        Component.text("!").color(NamedTextColor.AQUA)
                ));

            }

            case "setlobbyteams" -> {

                // Check if lobby is active
                if (!gameManager.getLobby().isActive() || gameManager.getGameState() != GameState.LOBBY) {
                    user.sendMessage(Component.text("The lobby is not currently active right now!").color(NamedTextColor.YELLOW));
                    return;
                }
                // Run pasting teams to lobby
                gameManager.getLobby().pasteEventTeams();

                user.sendMessage(Component.text("Changed lobby teams of players to the event teams!").color(NamedTextColor.GREEN));
            }

            case "register" -> {

                if (args.length < 3) {
                    user.sendMessage(Component.text("You must include a team ID!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get team
                String teamIdString = args[2].toLowerCase();
                CBCEventTeam team = eventManager.getTeam(teamIdString);

                // Check if team already registered
                if (team != null) {
                    user.sendMessage(Component.text("This team is already registered!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Check if team id is not valid
                NamedTextColor idToColor = CBCEventTeam.getTeamColorById(teamIdString);
                if (idToColor == null) {
                    user.sendMessage(Component.text("This team ID is not valid!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Create team
                String teamName = firstLetterUpper(teamIdString);
                CBCEventTeam registeredTeam = eventManager.registerTeam(teamIdString, teamName, idToColor);

                user.sendMessage(Component.text("Registered team ").color(NamedTextColor.AQUA).append(
                        registeredTeam.getNameComponent(true)
                ).append(
                        Component.text(" in the event!").color(NamedTextColor.AQUA)
                ));

            }

            case "unregister" -> {

                if (args.length < 3) {
                    user.sendMessage(Component.text("You must include a team ID!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get team
                String teamIdString = args[2].toLowerCase();
                CBCEventTeam team = eventManager.getTeam(teamIdString);

                // Check if team already registered
                if (team == null) {
                    user.sendMessage(Component.text("This team is not already registered!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Unregister team
                eventManager.unregisterTeam(team);
                user.sendMessage(Component.text("Unregistered team ").color(NamedTextColor.AQUA).append(
                        team.getNameComponent(true)
                ).append(
                        Component.text(" from the event!").color(NamedTextColor.AQUA)
                ));

            }
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
            tabCompletions.add("join");
            tabCompletions.add("leave");
            tabCompletions.add("register");
            tabCompletions.add("unregister");
            tabCompletions.add("setlobbyteams");
        }
        else {
            String subsubcommand = args[1].toLowerCase();
            if (subsubcommand.equals("join")) {
                if (level == 3) {
                    tabCompletions = getAllPlayerNames();
                }
                else if (level == 4) {
                    tabCompletions.addAll(eventManager.getAllTeamIds());
                }
            }
            else if (subsubcommand.equals("leave")) {
                if (level == 3) {
                    tabCompletions = getAllPlayerNames();
                }
            }
            else if (subsubcommand.equals("register")) {
                if (level == 3) {
                    tabCompletions = eventManager.getUnusedTeamIds();
                }
            }
            else if (subsubcommand.equals("unregister")) {
                if (level == 3) {
                    tabCompletions = eventManager.getAllTeamIds();
                }
            }
        }

        return tabCompletions;
    }

}