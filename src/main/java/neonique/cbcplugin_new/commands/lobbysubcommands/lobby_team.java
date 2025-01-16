package neonique.cbcplugin_new.commands.lobbysubcommands;

import neonique.cbcplugin_new.commands.LobbyCommand;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class lobby_team {

    public static void run(LobbyCommand lobbyCommand, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a sub-sub command!").color(NamedTextColor.YELLOW));
            return;
        }

        Lobby lobby = lobbyCommand.lobby;

        String subsubcommand = args[1].toLowerCase();

        // Randomize sub command - brings up GUI menu for randomisation
        switch (subsubcommand) {
            case "randomize":
                if (lobbyCommand.checkIfPerms(user, perms, 1)) return;
                lobby.openTeamRandomizeMenu(user);
                break;

            // Randomize sub command - brings up GUI menu for randomisation
            case "clearall":
                if (lobbyCommand.checkIfPerms(user, perms, 1)) return;
                lobby.clearAllTeams();

                user.sendMessage(
                        Component.text("All teams cleared!").color(NamedTextColor.GREEN)
                );

                break;

            // Swap sub command - swaps two players
            case "swap":
                if (lobbyCommand.checkIfPerms(user, perms, 1)) return;
                // Check if there are an ample amount of args
                if (args.length < 4) {
                    user.sendMessage(Component.text("You must include two player names!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Check if the args are player names
                String playerName1 = args[2];
                String playerName2 = args[3];
                Player player1Targeted = Bukkit.getServer().getPlayer(playerName1);
                Player player2Targeted = Bukkit.getServer().getPlayer(playerName2);

                if (player1Targeted == null && player2Targeted == null) {
                    user.sendMessage(
                            Component.text("Players ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(playerName1).color(NamedTextColor.GREEN))
                                    .append(Component.text(" and ").color(NamedTextColor.YELLOW))
                                    .append(Component.text(playerName2).color(NamedTextColor.GREEN))
                                    .append(Component.text(" were not found!").color(NamedTextColor.YELLOW))
                    );
                    return;
                } else if (player1Targeted == null) {
                    user.sendMessage(
                            Component.text("Player ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(playerName1).color(NamedTextColor.GREEN))
                                    .append(Component.text(" was not found!").color(NamedTextColor.YELLOW))
                    );
                    return;
                } else if (player2Targeted == null) {
                    user.sendMessage(
                            Component.text("Player ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(playerName2).color(NamedTextColor.GREEN))
                                    .append(Component.text(" was not found!").color(NamedTextColor.YELLOW))
                    );
                    return;
                }

                // Both players are in game, check if players are both in the lobby
                LobbyPlayer player1 = lobby.getLobbyPlayer(player1Targeted);
                LobbyPlayer player2 = lobby.getLobbyPlayer(player2Targeted);
                if (player1 == null || player2 == null) {
                    user.sendMessage(Component.text("Either one or both players is not in lobby!").color(NamedTextColor.YELLOW));
                    return;
                }

                LobbyTeam player1Team = player1.getAssignedTeam();
                LobbyTeam player2Team = player2.getAssignedTeam();

                // Check that both players are not on the same team
                if (player1Team == player2Team) {
                    user.sendMessage(Component.text("Both players are in the same team!").color(NamedTextColor.YELLOW));
                    return;
                }
                // Swap players teams
                lobby.playerLeaveTeam(player1, false);
                lobby.playerLeaveTeam(player2, false);

                if (player1Team != null) {
                    lobby.playerJoinTeam(player2, player1Team, true);
                }
                if (player2Team != null) {
                    lobby.playerJoinTeam(player1, player2Team, true);
                }

                user.sendMessage(
                        Component.text("Swapped the teams of ").color(NamedTextColor.GREEN)
                                .append(Component.text(playerName1).color(NamedTextColor.GREEN))
                                .append(Component.text(" and ").color(NamedTextColor.GREEN))
                                .append(Component.text(playerName2).color(NamedTextColor.GREEN))
                                .append(Component.text("!").color(NamedTextColor.GREEN))
                );

                // Update sidebar
                lobby.updateClientSidebars();
                break;

            // Join sub command - joins a player into a team
            case "join":
                if (lobbyCommand.checkIfPerms(user, perms, 1)) return;
                // Check if there are an ample amount of args
                if (args.length < 4) {
                    user.sendMessage(Component.text("You must include a player name and the team they will be put in!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Check if the args are player names
                String playerName = args[2];
                Player playerTargeted = Bukkit.getServer().getPlayer(playerName);

                if (playerTargeted == null) {
                    user.sendMessage(
                            Component.text("Player ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(playerName).color(NamedTextColor.GREEN))
                                    .append(Component.text(" was not found!").color(NamedTextColor.YELLOW))
                    );
                    return;
                }

                // Both players are in game, check if players are both in the lobby
                LobbyPlayer player = lobby.getLobbyPlayer(playerTargeted);
                if (player == null) {
                    user.sendMessage(
                            Component.text("Player ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(playerName).color(NamedTextColor.GREEN))
                                    .append(Component.text(" is not registered in lobby!").color(NamedTextColor.YELLOW))
                    );
                    return;
                }

                // Find the team mentioned
                Collection<LobbyTeam> teamCollection = lobby.getTeamsSet();
                LobbyTeam teamFound = null;
                for (LobbyTeam team : teamCollection) {
                    if (team.getTeamId().equals(args[3])) {
                        teamFound = team;
                        break;
                    }
                }

                // Check if team is found
                if (teamFound == null) {
                    user.sendMessage(
                            Component.text("Team with id ").color(NamedTextColor.YELLOW)
                                    .append(Component.text(args[3]).color(NamedTextColor.GREEN))
                                    .append(Component.text(" was not found!").color(NamedTextColor.YELLOW))
                    );
                    return;
                }

                // Check that both players are not on the same team
                if (teamFound.isPlayerInTeam(player)) {
                    user.sendMessage(Component.text("The player is already on this team!").color(NamedTextColor.YELLOW));
                    return;
                }

                // Get player to join team
                lobby.playerJoinTeam(player, teamFound, true);

                user.sendMessage(
                        Component.text(playerName).color(teamFound.getColor())
                                .append(Component.text(" has been added to team ").color(NamedTextColor.WHITE))
                                .append(Component.text(teamFound.getTeamName()).color(teamFound.getColor()))
                                .append(Component.text("!").color(NamedTextColor.WHITE))
                );

                lobby.updateClientSidebars();
                break;
        }
    }

    public static List<String> getTabCompletions (LobbyCommand lobbyCommand, String[] args, int perms) {

        int level = args.length;
        Lobby lobby = lobbyCommand.lobby;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            // Show all subcommands
            if (perms >= 1) tabCompletions = new ArrayList<>(Arrays.asList("join", "randomize", "swap", "clearall"));
        }
        else if (level >= 3) {
            // Fill out all arguments for swap command
            if (args[1].equals("swap") && perms >= 1) {
                if (level == 3 || level == 4) {
                    Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
                    Bukkit.getServer().getOnlinePlayers().toArray(players);
                    for (Player player : players) {
                        tabCompletions.add(player.getName());
                    }
                }
            }
            // Fill out all arguments for join command
            else if (args[1].equals("join") && perms >= 1) {
                if (level == 3) {
                    Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
                    Bukkit.getServer().getOnlinePlayers().toArray(players);
                    for (Player player : players) {
                        tabCompletions.add(player.getName());
                    }
                }
                else if (level == 4) {
                    for (LobbyTeam team : lobby.getTeamsSet()) {
                        tabCompletions.add(team.getTeamId());
                    }
                }
            }
        }

        return tabCompletions;
    }
}
