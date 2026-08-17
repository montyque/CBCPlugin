package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.commands.cbceventsubcommands.cbcevent_player;
import neonique.cbcplugin_new.commands.cbceventsubcommands.cbcevent_team;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CBCEventCommand extends _BaseCommand {

    public GameManager gameManager;
    public Lobby lobby;

    public CBCEventCommand(GameManager gameManager) {
        this.gameManager = gameManager;
        this.lobby = gameManager.getLobby();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        CBCEventManager eventManager = gameManager.getEventManager();
        boolean eventInProgress = eventManager != null;

        int level = args.length;

        if (!(sender instanceof Player user)) {
            return true;
        }

        // Get user and their permissions
        int perms = getPerms(user);

        if (level == 0) {
            user.sendMessage(Component.text("You must include a subcommand!").color(NamedTextColor.YELLOW));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (eventInProgress) {
            // ************************************************************
            // SETGAMESTATUS COMMAND - Sets status for game (is this game part of the event?)
            switch (subcommand) {
                case "setgamestatus" -> {
                    if (checkIfPerms(user, perms, 2)) return true;

                    if (level == 1) {
                        user.sendMessage(Component.text("You must use 'current' (if you want the current game to be an event game after it ends) or " +
                                "'last' (if you want the last game to be registered as an event game).").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String mode = args[1].toLowerCase();

                    if (mode.equals("current") || mode.equals("last")) {

                        if (mode.equals("current")) {
                            // Check if game is active -- a game must be active for current to be used
                            if (gameManager.getCurrentGame() == null) {
                                user.sendMessage(Component.text("To use 'current', a game must be currently running!").color(NamedTextColor.YELLOW));
                                return true;
                            }

                            if (level == 2) {
                                user.sendMessage(Component.text("You must set the status to 'true' or 'false'!").color(NamedTextColor.YELLOW));
                                return true;
                            }

                            boolean status = Boolean.parseBoolean(args[2].toLowerCase());

                            if (status) {
                                eventManager.setCurrentGameEventGame(true);
                                user.sendMessage(Component.text("Set to true - when the current game ends, it will count towards the event").color(NamedTextColor.GREEN));
                            } else {
                                eventManager.setCurrentGameEventGame(false);
                                user.sendMessage(Component.text("Set to false - when the current game ends, it will not count towards the event").color(NamedTextColor.GREEN));
                            }
                        } else {
                            // Check if game is active -- a game must be NOT active for last to be used
                            if (gameManager.getCurrentGame() != null) {
                                user.sendMessage(Component.text("To use 'last', a game must not be currently running!").color(NamedTextColor.YELLOW));
                                return true;
                            }
                            // Check if last game exists
                            if (gameManager.getLastGame() == null) {
                                user.sendMessage(Component.text("To use 'last', a game must have existed before this one!").color(NamedTextColor.YELLOW));
                                return true;
                            }

                            Game lastGame = gameManager.getLastGame();
                            if (!(lastGame instanceof TeamGame teamGame)) {
                                user.sendMessage(Component.text("This game is not a team game, so it cannot be registered in the event!").color(NamedTextColor.YELLOW));
                                return true;
                            }

                            // Check if that game has already been put, then it will not duplicate it -- mistake proof
                            if (eventManager.getGameList().contains(teamGame)) {
                                user.sendMessage(Component.text("This game has already been added!").color(NamedTextColor.YELLOW));
                                return true;
                            }

                            eventManager.eventGameEnded(teamGame);

                            user.sendMessage(Component.text("Registered last game as an official event game.").color(NamedTextColor.GREEN));

                        }
                    } else {
                        user.sendMessage(Component.text("You must use 'current' (if you want the current game to be an event game after it ends) or " +
                                "'last' (if you want the last game to be registered as an event game).").color(NamedTextColor.YELLOW));
                        return true;
                    }
                }

                // ************************************************************
                // SETGAMEMODE COMMAND - Sets gamemode for one of the games, if needs to be overridden

                case "setgamemode" -> {
                    if (checkIfPerms(user, perms, 2)) return true;

                    if (level == 1) {
                        user.sendMessage(Component.text("You must put a game number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String game = args[1].toLowerCase();
                    int gameNum;
                    try {
                        gameNum = Integer.parseInt(game);
                        if (gameNum < 1 || gameNum > CBCEventManager.getGameAmount() + 1) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        user.sendMessage(Component.text("Invalid number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    if (level == 2) {
                        user.sendMessage(Component.text("You must include a gamemode!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String gamemodeString = args[2].toUpperCase();

                    // Choose a gamemode
                    if (!CBCGamemode.getGamemodeIds().contains(gamemodeString)) {
                        user.sendMessage(Component.text("Invalid gamemode!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    // Get gamemode
                    CBCGamemode gamemode = CBCGamemode.valueOf(gamemodeString);

                    eventManager.setGamemode(gamemode, gameNum);
                    user.sendMessage("Set gamemode for game " + gameNum + " to '" + gamemodeString + "'.");
                }
                case "setmapname" -> {
                    if (checkIfPerms(user, perms, 2)) return true;

                    if (level == 1) {
                        user.sendMessage(Component.text("You must put a game number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String game = args[1].toLowerCase();
                    int gameNum;
                    try {
                        gameNum = Integer.parseInt(game);
                        if (gameNum < 1 || gameNum > CBCEventManager.getGameAmount() + 1) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        user.sendMessage(Component.text("Invalid number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    if (level == 2) {
                        user.sendMessage(Component.text("You must include a map name!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String mapName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    eventManager.setMapName(mapName, gameNum);
                    user.sendMessage("Set gamemode for game " + gameNum + " to '" + mapName + "'.");
                }
                case "gamestats" -> {

                    if (level == 1) {
                        user.sendMessage(Component.text("You must put a game number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    String game = args[1].toLowerCase();
                    int gameNum;
                    try {
                        gameNum = Integer.parseInt(game);
                        if (gameNum < 1 || gameNum > CBCEventManager.getGameAmount() + 1) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        user.sendMessage(Component.text("Invalid number!").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    // Check if post game stats exist
                    PostGameStats gameStats = eventManager.getPostGameStats(gameNum);
                    if (gameStats == null) {
                        user.sendMessage(Component.text("Statistics for " + eventManager.getGameName(gameNum) + " are unavailable.").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    // Open inventory menu
                    Inventory lastGameStats = gameStats.createInventoryGui(user);
                    user.openInventory(lastGameStats);
                }
                case "leaderboard" -> {
                    if (eventManager.getEventWinner() == null) {
                        if (checkIfPerms(user, perms, 2)) return true;
                    }
                    // Get leaderboard
                    user.sendMessage(eventManager.generateLeaderboard());
                }
                case "player" -> {
                    if (checkIfPerms(user, perms, 2)) return true;
                    cbcevent_player.run(this, user, args, perms);
                }
                case "team" -> {
                    if (checkIfPerms(user, perms, 2)) return true;
                    cbcevent_team.run(this, user, args, perms);
                }
                case "announcemvp" -> {
                    if (checkIfPerms(user, perms, 2)) return true;

                    if (eventManager.getEventWinner() == null) {
                        user.sendMessage(Component.text("Event has not crowned a winner yet! MVP can only be announced if there is a winner.").color(NamedTextColor.YELLOW));
                        return true;
                    }

                    eventManager.announceMVP();
                }
            }
        }
        else {
            if (subcommand.equals("create")) {
                if (checkIfPerms(user, perms, 2)) return true;

                // Creating the event
                gameManager.createCBCEvent();
                user.sendMessage(Component.text("CBC event has been created!").color(NamedTextColor.GREEN));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!(sender instanceof Player user)) {
            return null;
        }

        CBCEventManager eventManager = gameManager.getEventManager();
        boolean eventInProgress = eventManager != null;

        // Get user and their permissions
        int perms = getPerms(user);

        List<String> tabCompleters = new ArrayList<>();
        int level = args.length;
        if (level == 1) {
            if (eventInProgress) {
                // Add sub commands to list
                tabCompleters.add("gamestats");

                if (perms >= 2 || eventManager.getEventWinner() != null) {
                    tabCompleters.add("leaderboard");
                }

                if (perms >= 2) {
                    tabCompleters.add("setgamestatus");
                    tabCompleters.add("setgamemode");
                    tabCompleters.add("player");
                    tabCompleters.add("team");
                    if (eventManager.getEventWinner() != null) {
                        tabCompleters.add("announcemvp");
                    }
                }
            } else {
                // Only show start
                if (perms >= 2) {
                    tabCompleters.add("create");
                }
            }
        } else if (level >= 2) {
            String subcommand = args[0].toLowerCase();
            // Show arguments for sub commands
            if (subcommand.equals("setgamestatus")) {
                if (level == 2) {
                    tabCompleters.add("current");
                    tabCompleters.add("last");
                }
                else if (level == 3) {
                    tabCompleters.add("true");
                    tabCompleters.add("false");
                }
            }
            if (subcommand.equals("gamestats")) {
                if (level == 2) {
                    for (int i = 1; i <= CBCEventManager.getGameAmount() + 1; i++) tabCompleters.add(i + "");
                }
            }
            if (subcommand.equals("setgamemode") || subcommand.equals("setmapname")) {
                if (level == 2) {
                    for (int i = 1; i <= CBCEventManager.getGameAmount() + 1; i++) tabCompleters.add(i + "");
                }
                else if (level == 3 && subcommand.equals("setgamemode")) {
                    tabCompleters.addAll(CBCGamemode.getGamemodeIds());
                }
            }
            if (subcommand.equals("player") && perms >= 2) {
                tabCompleters = cbcevent_player.getTabCompletions(this, args, perms);
            }
            if (subcommand.equals("team") && perms >= 2) {
                tabCompleters = cbcevent_team.getTabCompletions(this, args, perms);
            }
        }
        return tabCompleters;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
