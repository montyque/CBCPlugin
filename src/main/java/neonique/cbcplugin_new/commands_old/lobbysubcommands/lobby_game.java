package neonique.cbcplugin_new.commands_old.lobbysubcommands;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.commands_old.LobbyCommand;
import neonique.cbcplugin_new.core.GameSettings;
import neonique.cbcplugin_new.core.InvalidSettingValueException;
import neonique.cbcplugin_new.combat.weapons.WeaponType;
import neonique.cbcplugin_new.services.WeaponPresetService;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.mechanics.OverallPreset;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
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
            try {
                lobby.checkStartConditions();
            } catch (IllegalStateException e) {
                user.sendMessage(
                        Component.text("Could not start game: " + e.getMessage()).color(NamedTextColor.YELLOW)
                );
                return;
            }

            // Game can be started
            lobby.startGameCountdown();

        }

        if (subsubcommand.equals("cancel")) {
            // Check if game has not even started yet
            if (!lobby.isGameStarting()) {
                user.sendMessage(
                        Component.text("The start countdown hasn't started yet!").color(NamedTextColor.YELLOW)
                );
                return;
            }

            // Cancel the countdown
            lobby.cancelGameCountdown(user, 1);

        }

        if (subsubcommand.equals("vars")) {

            if (lobbyCommand.checkIfPerms(user, perms, 1)) return;

            GameSettings gameSettings = lobby.gameSettings();
            if (gameSettings == null) {
                user.sendMessage(Component.text("No game settings are not available.")
                        .color(NamedTextColor.YELLOW));
                return;
            }

            if (args.length < 3) {
                user.sendMessage(Component.text("You need to provide a game setting name! Possible settings: "
                                + String.join(", ", gameSettings.getAllSettingNames()))
                        .color(NamedTextColor.YELLOW));
                return;
            }

            String gameSetting = args[2];

            if (args.length == 3) {

                // Display user current value of game variable
                try {
                    user.sendMessage(Component.text("Game variable: " + gameSetting + "' is currently set to "
                            + gameSettings.getSetting(gameSetting).valueString()).color(NamedTextColor.GREEN));
                } catch (IllegalArgumentException e) {
                    user.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.YELLOW));
                }

            } else {

                // Parse the user's value
                String settingValue = args[3];
                try {
                    gameSettings.setSetting(gameSetting, settingValue);
                    user.sendMessage(
                            Component.text("Game variable '" + gameSettings + "' has been set to "
                                            + gameSettings.getSetting(gameSetting).valueString())
                                    .color(NamedTextColor.GREEN)
                    );
                } catch (IllegalArgumentException | InvalidSettingValueException e) {
                    user.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.YELLOW));
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
            WeaponPresetService presetService = CBCPlugin.getPlugin().getWeaponPresetService();
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
                List<String> presetIds = CBCPlugin.getPlugin().getWeaponPresetService().getPresetIds(weapon);
                if (weapon == null) {
                    presetIds = CBCPlugin.getPlugin().getWeaponPresetService().getOverallPresetIds();
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

                    CreeperPreset preset = CBCPlugin.getPlugin().getWeaponPresetService().getCreeperPresetById(presetId);

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

                    FlamePreset preset = presetService.getFlamePresetById(presetId);
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

                    XbowPreset preset = presetService.getXbowPresetById(presetId);
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

                    OverallPreset preset = presetService.getOverallPreset(presetId);

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
                        tabCompletions = lobby.gameSettings() != null ?
                                List.copyOf(lobby.gameSettings().getAllSettingNames()) : List.of();
                    }
                    else if (level == 4) {
                        String gameVar = args[2].toLowerCase();
                        tabCompletions = lobby.gameSettings() != null ?
                                lobby.gameSettings().getSettingTabCompletions(gameVar) : List.of();
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
                            tabCompletions = CBCPlugin.getPlugin().getWeaponPresetService().getPresetIds(WeaponType.CREEPER);
                        }
                        else if (args[2].equalsIgnoreCase("flame")) {
                            tabCompletions = CBCPlugin.getPlugin().getWeaponPresetService().getPresetIds(WeaponType.FLAME);
                        }
                        else if (args[2].equalsIgnoreCase("xbow")) {
                            tabCompletions = CBCPlugin.getPlugin().getWeaponPresetService().getPresetIds(WeaponType.XBOW);
                        }
                        else if (args[2].equalsIgnoreCase("all")) {
                            tabCompletions = CBCPlugin.getPlugin().getWeaponPresetService().getOverallPresetIds();
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
