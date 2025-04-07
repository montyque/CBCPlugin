package neonique.cbcplugin_new.commands.lobbysubcommands;

import neonique.cbcplugin_new.commands.LobbyCommand;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.gameobjects.OverallPreset;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;

public class lobby_game {

    public static void run(LobbyCommand lobbyCommand, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a sub-sub command!").color(NamedTextColor.YELLOW));
            return;
        }

        Lobby lobby = lobbyCommand.lobby;
        GameManager gameManager = lobbyCommand.gameManager;
        CombatManager combatManager = lobbyCommand.gameManager.combatManager;

        String subsubcommand = args[1].toLowerCase();

        if (subsubcommand.equals("start")) {
            if (lobbyCommand.checkIfPerms(user, perms, 1)) return;

            // Check if game is already starting
            if (lobby.isGameStarting()) {
                user.sendMessage(
                        Component.text("The game is already starting! To cancel the game start, use ").color(NamedTextColor.YELLOW)
                                .append(Component.text("/lobby game cancel").color(NamedTextColor.GREEN))
                                .append(Component.text(" to cancel the start.").color(NamedTextColor.YELLOW))
                );
                return;
            }

            // Check if game can be started
            int gameCanBeStarted = lobby.canStartGame();
            if (gameCanBeStarted > 0) {
                Component errorMessage = null;

                // Game cannot be started because there is no selected gamemode
                if (gameCanBeStarted == 1) {
                    errorMessage = Component.text("You need to select a gamemode first! Use ").color(NamedTextColor.YELLOW)
                            .append(Component.text("/lobby setgamemode").color(NamedTextColor.GREEN))
                            .append(Component.text(" to select a gamemode and a map.").color(NamedTextColor.YELLOW));
                }

                // Game cannot be started because there is no selected map
                else if (gameCanBeStarted == 2) {
                    errorMessage = Component.text("You need to select a map first! Use ").color(NamedTextColor.YELLOW)
                            .append(Component.text("/lobby setgamemode").color(NamedTextColor.GREEN))
                            .append(Component.text(" to select a gamemode and a map.").color(NamedTextColor.YELLOW));
                }

                // Game cannot be started because error occured finding gamemode variables for gamemode
                else if (gameCanBeStarted == 3) {
                    errorMessage = Component.text("Could not find gamemode variables for gamemode ").color(NamedTextColor.YELLOW)
                            .append(Component.text(lobby.getGamemodeSelected().name()).color(NamedTextColor.GREEN));
                }

                // Game cannot be started because too many/not enough teams
                else if (gameCanBeStarted == 4 || gameCanBeStarted == 5) {
                    Set<LobbyTeam> teamsWithOnlinePlayers = lobby.getTeamsWithOnlinePlayers();
                    int minTeams;
                    int maxTeams;
                    if (lobby.getMapSelected().getMinTeams() == null) {
                        minTeams = gameManager.getGamemodes().get(lobby.getGamemodeSelected()).getMinTeams();
                        maxTeams = gameManager.getGamemodes().get(lobby.getGamemodeSelected()).getMaxTeams();
                    } else {
                        minTeams = lobby.getMapSelected().getMinTeams();
                        maxTeams = lobby.getMapSelected().getMaxTeams();
                    }
                    if (gameCanBeStarted == 4) {
                        errorMessage = Component.text("There are not enough teams! There are ").color(NamedTextColor.YELLOW)
                                .append(Component.text(teamsWithOnlinePlayers.size()).color(NamedTextColor.GREEN))
                                .append(Component.text(" teams with online players. ").color(NamedTextColor.YELLOW))
                                .append(Component.text(minTeams + "-" + maxTeams).color(NamedTextColor.GREEN))
                                .append(Component.text(" teams are required to start the game.").color(NamedTextColor.YELLOW));
                    } else {
                        errorMessage = Component.text("There are too many teams! There are ").color(NamedTextColor.YELLOW)
                                .append(Component.text(teamsWithOnlinePlayers.size()).color(NamedTextColor.GREEN))
                                .append(Component.text(" teams with online players. ").color(NamedTextColor.YELLOW))
                                .append(Component.text(minTeams + "-" + maxTeams).color(NamedTextColor.GREEN))
                                .append(Component.text(" teams are required to start the game.").color(NamedTextColor.YELLOW));
                    }
                }

                // Game cannot be started because a team is not valid
                else if (gameCanBeStarted == 6) {
                    Set<LobbyTeam> teamsWithOnlinePlayers = lobby.getTeamsWithOnlinePlayers();
                    Set<String> invalidTeams = new HashSet<>();
                    for (LobbyTeam team : teamsWithOnlinePlayers) {
                        if (!lobby.getMapSelected().getTeamsAllowed().contains(team.getTeamId())) {
                            invalidTeams.add(team.getTeamId());
                        }
                    }
                    errorMessage = Component.text("There are invalid teams with players in them! The teams: ").color(NamedTextColor.YELLOW)
                            .append(Component.text(String.join(", ", invalidTeams)).color(NamedTextColor.GREEN))
                            .append(Component.text(" are not allowed on this map.").color(NamedTextColor.YELLOW));
                }

                else if (gameCanBeStarted == 7) {
                    errorMessage = Component.text("There are not enough players! ").color(NamedTextColor.YELLOW)
                            .append(Component.text(lobby.getLobbyPlayersPlayingAndOnline().size()).color(NamedTextColor.GREEN))
                            .append(Component.text(" players are playing. ").color(NamedTextColor.YELLOW))
                            .append(Component.text(gameManager.getGamemodes().get(lobby.getGamemodeSelected()).getMinPlayers()).color(NamedTextColor.GREEN))
                            .append(Component.text(" players are required for this game.").color(NamedTextColor.YELLOW));
                }

                if (errorMessage != null) {
                    user.sendMessage(errorMessage);
                    return;
                }
            }

            // Game can be started
            lobby.startGameCountdown();

        }

        if (subsubcommand.equals("cancel")) {
            // Check if game has not even started yet
            if (!lobby.isGameStarting()) {
                user.sendMessage(
                        Component.text("The start countdown hasn't started yet! ").color(NamedTextColor.YELLOW)
                );
                return;
            }

            // Cancel the countdown
            lobby.cancelGameCountdown(user, 1);
        }

        if (subsubcommand.equals("vars")) {

            if (lobbyCommand.checkIfPerms(user, perms, 1)) return;

            if (args.length < 3) {
                user.sendMessage(
                        Component.text("You need to provide a game variable name!").color(NamedTextColor.YELLOW)
                );
                return;
            }

            String gameVar = args[2];
            // Check if game var is in valid list
            if (!lobby.getAllGameVarKeys().contains(gameVar)) {
                user.sendMessage(
                        Component.text("There is no '" + gameVar + "' game variable!").color(NamedTextColor.YELLOW)
                );
                return;
            }

            // Check if there is no value
            if (args.length == 3) {
                // This means that the user only wants to see what the game variable is set to
                String value = null;
                if (lobby.getBooleanGameVar(gameVar) != null) {
                    value = lobby.getBooleanGameVar(gameVar).toString();
                }
                else if (lobby.getIntGameVar(gameVar) != null) {
                    value = lobby.getIntGameVar(gameVar).toString();
                }
                else if (lobby.getStringGameVar(gameVar) != null) {
                    value = lobby.getStringGameVar(gameVar);
                }

                if (value != null) {
                    user.sendMessage(
                            Component.text("Game variable '" + gameVar + "' is currently set to: " + value).color(NamedTextColor.GREEN)
                    );
                }
            } else {
                // This means that the user wants to set the game variable
                String value = null;
                if (lobby.getBooleanGameVar(gameVar) != null) {
                    if (args[3].equals("true")) {
                        lobby.setBooleanGameVar(gameVar, true);
                        value = "true";
                    }
                    else if (args[3].equals("false")) {
                        lobby.setBooleanGameVar(gameVar, false);
                        value = "false";
                    }
                    else {
                        user.sendMessage(
                                Component.text("Game variable '" + gameVar + "' is boolean, so you can only set it to 'true' or 'false'").color(NamedTextColor.YELLOW)
                        );
                        return;
                    }
                }
                else if (lobby.getIntGameVar(gameVar) != null) {
                    try {
                        value = args[3];
                        int intValue = Integer.parseInt(value);
                        lobby.setIntGameVar(gameVar, intValue);
                    } catch (NumberFormatException e) {
                        user.sendMessage(
                                Component.text("Game variable '" + gameVar + "' is integer, so you must input an integer").color(NamedTextColor.YELLOW)
                        );
                        return;
                    }
                }
                else if (lobby.getStringGameVar(gameVar) != null) {
                    List<String> stringList = new ArrayList<>(Arrays.asList(args).subList(3, args.length));
                    value = String.join(" ", stringList);
                    lobby.setStringGameVar(gameVar, value);
                }

                if (value != null) {
                    user.sendMessage(
                            Component.text("Game variable '" + gameVar + "' has been set to: " + value).color(NamedTextColor.GREEN)
                    );
                }
            }
        }

        if (subsubcommand.equals("setweapon")) {

            if (lobbyCommand.checkIfPerms(user, perms, 1)) return;

            if (args.length < 3) {
                user.sendMessage(
                        Component.text("You need to provide which weapon you are setting! Either 'creeper', 'flame', 'xbow', or 'all'.").color(NamedTextColor.YELLOW)
                );
                return;
            }

            String weaponType = args[2];
            WeaponType weapon;
            NamedTextColor weaponColor;

            if (weaponType.equalsIgnoreCase("creeper")) {
                weapon = WeaponType.CREEPER;
                weaponColor = NamedTextColor.GREEN;
            }
            else if (weaponType.equalsIgnoreCase("flame")) {
                weapon = WeaponType.FLAME;
                weaponColor = NamedTextColor.GOLD;
            }
            else if (weaponType.equalsIgnoreCase("xbow")) {
                weapon = WeaponType.XBOW;
                weaponColor = NamedTextColor.AQUA;
            }
            else if (weaponType.equalsIgnoreCase("all")) {
                weapon = null;
                weaponColor = NamedTextColor.WHITE;
            }
            else {
                user.sendMessage(Component.text("Invalid weapon! Weapon must be either 'creeper', 'flame', 'xbow' or 'all' if you want to set an overall preset."));
                return;
            }

            // Check if player wants to know current preset, and has not provided any other info
            if (args.length < 4) {
                if (weapon != null) {
                    // Send current preset id
                    user.sendMessage(Component.text("Current preset for " + weapon + " is ").color(NamedTextColor.YELLOW).append(
                            Component.text(lobby.getWeaponPreset(weapon).getPresetName()).color(weaponColor)
                    ));
                }
                else {
                    // Send preset id of all weapons
                    user.sendMessage(Component.text("Current preset for all weapons is ").color(NamedTextColor.YELLOW)
                    .append(
                            Component.text(lobby.getWeaponPreset(WeaponType.CREEPER).getPresetName()).color(NamedTextColor.GREEN)
                    ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                            Component.text(lobby.getWeaponPreset(WeaponType.CREEPER).getPresetName()).color(NamedTextColor.GOLD)
                    ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                            Component.text(lobby.getWeaponPreset(WeaponType.XBOW).getPresetName()).color(NamedTextColor.AQUA)
                    ));
                }
            }
            else {

                // Player wants to set preset
                // Player wants to set preset
                String presetId = args[3].toUpperCase();

                // Get valid ids
                List<String> presetIds = combatManager.getPresetIds(weapon);
                if (weapon == null) {
                    presetIds = combatManager.getOverallPresetIds();
                }

                if (!presetIds.contains(presetId)) {
                    // Invalid presetId
                    user.sendMessage(Component.text("Invalid preset id!"));
                    return;
                }

                // Find team if team listed
                String teamId = null;
                if (args.length >= 5) {
                    teamId = args[4].toLowerCase();
                    if (!lobby.getLobbyTeamIds().contains(teamId)) {
                        user.sendMessage(Component.text(teamId + " is not a valid team id!").color(NamedTextColor.YELLOW));
                        return;
                    }
                }

                // Set preset
                if (weapon == WeaponType.CREEPER) {

                    CreeperPreset preset = combatManager.getCreeperPresetById(presetId);

                    if (teamId != null) {
                        lobby.addTeamCreeperOverrides(teamId, preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW)
                                .append(
                                        Component.text(preset.getPresetName()).color(weaponColor)
                                ).append(Component.text(" for team ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(teamId).color(lobby.getColorForTeamId(teamId))
                                ));
                    }
                    else {
                        lobby.setCreeperPreset(preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW).append(
                                Component.text(preset.getPresetName()).color(weaponColor)
                        ));
                    }
                }
                else if (weapon == WeaponType.FLAME) {

                    FlamePreset preset = combatManager.getFlamePresetById(presetId);
                    if (teamId != null) {
                        lobby.addTeamFlameOverrides(teamId, preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW)
                                .append(
                                        Component.text(preset.getPresetName()).color(weaponColor)
                                ).append(Component.text(" for team ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(teamId).color(lobby.getColorForTeamId(teamId))
                                ));
                    }
                    else {
                        lobby.setFlamePreset(preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW).append(
                                Component.text(preset.getPresetName()).color(weaponColor)
                        ));
                    }

                }
                else if (weapon == WeaponType.XBOW) {

                    XbowPreset preset = combatManager.getXbowPresetById(presetId);
                    if (teamId != null) {
                        lobby.addTeamXbowOverrides(teamId, preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW)
                                .append(
                                        Component.text(preset.getPresetName()).color(weaponColor)
                                ).append(Component.text(" for team ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(teamId).color(lobby.getColorForTeamId(teamId))
                                ));
                    }
                    else {
                        lobby.setXbowPreset(preset);
                        user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").color(NamedTextColor.YELLOW).append(
                                Component.text(preset.getPresetName()).color(weaponColor)
                        ));
                    }
                }

                else {

                    OverallPreset preset = combatManager.getOverallPreset(presetId);

                    CreeperPreset creeperPreset = preset.getCreeperPreset();
                    FlamePreset flamePreset = preset.getFlamePreset();
                    XbowPreset xbowPreset = preset.getXbowPreset();

                    if (teamId != null) {

                        lobby.addTeamCreeperOverrides(teamId, creeperPreset);
                        lobby.addTeamFlameOverrides(teamId, flamePreset);
                        lobby.addTeamXbowOverrides(teamId, xbowPreset);

                        user.sendMessage(Component.text("Current preset for all weapons has been set to ").color(NamedTextColor.YELLOW)
                                .append(
                                        Component.text(creeperPreset.getPresetName()).color(NamedTextColor.GREEN)
                                ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(flamePreset.getPresetName()).color(NamedTextColor.GOLD)
                                ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(xbowPreset.getPresetName()).color(NamedTextColor.AQUA)
                                ).append(Component.text(" for team ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(teamId).color(lobby.getColorForTeamId(teamId)
                                ))
                        );
                    }
                    else {
                        lobby.setCreeperPreset(preset.getCreeperPreset());
                        lobby.setFlamePreset(preset.getFlamePreset());
                        lobby.setXbowPreset(preset.getXbowPreset());
                        user.sendMessage(Component.text("Current preset for all weapons has been set to ").color(NamedTextColor.YELLOW)
                                .append(
                                        Component.text(lobby.getWeaponPreset(WeaponType.CREEPER).getPresetName()).color(NamedTextColor.GREEN)
                                ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(lobby.getWeaponPreset(WeaponType.FLAME).getPresetName()).color(NamedTextColor.GOLD)
                                ).append(Component.text(", ").color(NamedTextColor.YELLOW)).append(
                                        Component.text(lobby.getWeaponPreset(WeaponType.XBOW).getPresetName()).color(NamedTextColor.AQUA)
                                )
                        );
                    }
                }
            }
        }
    }

    public static List<String> getTabCompletions (LobbyCommand lobbyCommand, String[] args, int perms) {

        int level = args.length;
        Lobby lobby = lobbyCommand.lobby;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            // Show all subcommands
            if (!lobby.isGameStarting()) {
                if (perms >= 1) {
                    tabCompletions.add("start");
                    tabCompletions.add("vars");
                    tabCompletions.add("setweapon");
                }
            } else {
                tabCompletions.add("cancel");
            }
        }
        else if (level >= 3) {
            String subsubcommand = args[1].toLowerCase();
            if (perms >= 1) {
                if (subsubcommand.equals("vars")) {
                    if (level == 3) {
                        // Show all game variables
                        tabCompletions = new ArrayList<>(lobby.getAllGameVarKeys());
                    }
                    else if (level == 4) {
                        String gameVar = args[2].toLowerCase();
                        tabCompletions = lobby.getGameVarTabCompletions(gameVar);
                    }
                }
                else if (subsubcommand.equals("setweapon")) {
                    GameManager gameManager = lobbyCommand.gameManager;
                    CombatManager combatManager = gameManager.combatManager;

                    if (level == 3) {
                        // Show all potential weapons
                        tabCompletions.add("creeper");
                        tabCompletions.add("flame");
                        tabCompletions.add("xbow");
                        tabCompletions.add("all");
                    }
                    else if (level == 4) {
                        if (args[2].equalsIgnoreCase("creeper")) {
                            tabCompletions = combatManager.getPresetIds(WeaponType.CREEPER);
                        }
                        else if (args[2].equalsIgnoreCase("flame")) {
                            tabCompletions = combatManager.getPresetIds(WeaponType.FLAME);
                        }
                        else if (args[2].equalsIgnoreCase("xbow")) {
                            tabCompletions = combatManager.getPresetIds(WeaponType.XBOW);
                        }
                        else if (args[2].equalsIgnoreCase("all")) {
                            tabCompletions = combatManager.getOverallPresetIds();
                        }
                    }
                    else if (level == 5) {
                        tabCompletions = lobby.getLobbyTeamIds();
                    }
                }
            }
        }

        return tabCompletions;

    }

}
