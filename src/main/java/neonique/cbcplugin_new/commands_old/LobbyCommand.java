package neonique.cbcplugin_new.commands_old;

import neonique.cbcplugin_new.commands_old.lobbysubcommands.lobby_game;
import neonique.cbcplugin_new.commands_old.lobbysubcommands.lobby_setgamemode;
import neonique.cbcplugin_new.commands_old.lobbysubcommands.lobby_spectator;
import neonique.cbcplugin_new.commands_old.lobbysubcommands.lobby_team;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.lobby_old.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LobbyCommand extends _BaseCommand {

    public GameManager gameManager;
    public Lobby lobby;

    public LobbyCommand(GameManager gameManager) {
        this.gameManager = gameManager;
        this.lobby = gameManager.getLobby();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player)) {
            return true;
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        if (level == 0) {
            user.sendMessage(Component.text("You must include a subcommand!").color(NamedTextColor.YELLOW));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (gameManager.getGameState() == GameState.LOBBY) {
            // ************************************************************
            // TEAM COMMAND - Handles the current lobby teams and players
            if (subcommand.equals("team")) {
                if (lobby.isGameStarting()) {
                    user.sendMessage(Component.text("You cannot run this command as the game is starting already!").color(NamedTextColor.YELLOW));
                    return true;
                }
                lobby_team.run(this, user, args, perms);
            }
            // ************************************************************
            // SPECTATOR COMMAND - Handles the spectators in the lobby
            if (subcommand.equals("spectator")) {
                if (lobby.isGameStarting()) {
                    user.sendMessage(Component.text("You cannot run this command as the game is starting already!").color(NamedTextColor.YELLOW));
                    return true;
                }
                lobby_spectator.run(this, user, args, perms);
            }
            // ************************************************************
            // SETGAMEMODE COMMAND - Sets gamemode
            if (subcommand.equals("setgamemode")) {
                if (lobby.isGameStarting()) {
                    user.sendMessage(Component.text("You cannot run this command as the game is starting already!").color(NamedTextColor.YELLOW));
                    return true;
                }
                if (checkIfPerms(user, perms, 1)) return true;
                lobby_setgamemode.run(this, user, args, perms);
            }
            // ************************************************************
            // GAME COMMAND - Handles game start and game variables
            switch (subcommand) {
                case "game":
                    if (checkIfPerms(user, perms, 1)) return true;
                    lobby_game.run(this, user, args, perms);
                    break;
                // ******************************************************
                // LOBBY TELEPORT COMMAND - Teleports user back to lobby
                case "teleport":
                    user.teleport(lobby.getLobbySpawn());
                    user.sendMessage(Component.text("You have been teleported back to the lobby.").color(NamedTextColor.YELLOW));
                    break;
                // ******************************************************
                // STOP LOBBY COMMAND - Stops the current lobby
                case "stop":
                    if (checkIfPerms(user, perms, 1)) return true;
                    gameManager.stopLobby();
                    break;
            }
        }
        else {
            if (subcommand.equals("start")) {
                if (checkIfPerms(user, perms, 1)) return true;

                // Starting/creating the lobby
                // Check that there is no active lobby right now or that game is active
                if (gameManager.getGameState() == GameState.LOBBY) {
                    user.sendMessage(Component.text("You are already in the lobby!").color(NamedTextColor.YELLOW));
                    return true;
                } else if (gameManager.getGameState() == GameState.ACTIVE || gameManager.getGameState() == GameState.STARTING) {
                    user.sendMessage(Component.text("You cannot start the lobby as " +
                            "a game is already active!").color(NamedTextColor.YELLOW));
                    return true;
                } else if (gameManager.getGameState() == GameState.GAMEOVER) {
                    user.sendMessage(Component.text("To go back to lobby, use the /game end command!").color(NamedTextColor.YELLOW));
                    return true;
                }

                // Lobby can be activated
                gameManager.startLobby();
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return null;
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        List<String> tabCompleters = new ArrayList<>();
        int level = args.length;
        if (level == 1) {
            if (gameManager.getGameState() == GameState.LOBBY) {
                // Add sub commands to list
                tabCompleters = new ArrayList<>(Arrays.asList("team", "spectator", "game", "teleport"));
                if (perms >= 1) {
                    tabCompleters.add("setgamemode");
                    tabCompleters.add("stop");
                }
            } else if (gameManager.getGameState() == GameState.DISABLED) {
                // Only show start
                if (perms >= 1) {
                    tabCompleters.add("start");
                }
            }
        } else if (level >= 2 && gameManager.getGameState() == GameState.LOBBY) {
            String subcommand = args[0].toLowerCase();
            // Show arguments for sub commands
            switch (subcommand) {
                case "team":
                    tabCompleters = lobby_team.getTabCompletions(this, args, perms);
                    break;
                case "spectator":
                    tabCompleters = lobby_spectator.getTabCompletions(this, args, perms);
                    break;
                case "game":
                    tabCompleters = lobby_game.getTabCompletions(this, args, perms);
                    break;
            }
        }
        return tabCompleters;
    }
}
