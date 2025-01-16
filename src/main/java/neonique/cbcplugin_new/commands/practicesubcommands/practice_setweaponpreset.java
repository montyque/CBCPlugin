package neonique.cbcplugin_new.commands.practicesubcommands;

import neonique.cbcplugin_new.commands.PracticeCommand;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.gameobjects.CreeperPreset;
import neonique.cbcplugin_new.gameobjects.FlamePreset;
import neonique.cbcplugin_new.gameobjects.XbowPreset;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class practice_setweaponpreset {

    public static void run(PracticeCommand command, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a weapon to change the preset to!").color(NamedTextColor.YELLOW));
            return;
        }

        GameManager gameManager = command.gameManager;
        CombatManager combatManager = gameManager.combatManager;

        String weaponType = args[1];
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
        else {
            user.sendMessage(Component.text("Invalid weapon! Weapon must be either 'creeper', 'flame' or 'xbow'."));
            return;
        }

        // Check if player wants to know current preset
        if (args.length < 3) {
            // Send current preset id
            user.sendMessage(Component.text("Current preset for " + weapon + " is ").append(
                    Component.text(combatManager.getWeaponVariables(weapon).getPresetName()).color(weaponColor)
            ));
        }
        else {
            // Player wants to set preset
            String presetId = args[2].toUpperCase();

            // Get valid ids
            List<String> presetIds = combatManager.getPresetIds(weapon);

            if (!presetIds.contains(presetId)) {
                // Invalid presetId
                user.sendMessage(Component.text("Invalid preset id!"));
                return;
            }

            // Set preset
            if (weapon == WeaponType.CREEPER) {

                CreeperPreset preset = combatManager.getCreeperPresetById(presetId);
                combatManager.setCreeperWeaponVariables(preset);

                user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").append(
                        Component.text(preset.getPresetName()).color(weaponColor)
                ));

            }
            else if (weapon == WeaponType.FLAME) {

                FlamePreset preset = combatManager.getFlamePresetById(presetId);
                combatManager.setFlameWeaponVariables(preset);

                user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").append(
                        Component.text(preset.getPresetName()).color(weaponColor)
                ));

            }
            else {

                XbowPreset preset = combatManager.getXbowPresetById(presetId);
                combatManager.setXbowWeaponVariables(preset);

                user.sendMessage(Component.text("Current preset for " + weapon + " has been set to ").append(
                        Component.text(preset.getPresetName()).color(weaponColor)
                ));

            }
        }
    }

    public static List<String> getTabCompletions (PracticeCommand command, String[] args, int perms) {

        int level = args.length;
        GameManager gameManager = command.gameManager;
        CombatManager combatManager = gameManager.combatManager;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            if (perms >= 1) {
                // Show all potential weapons
                tabCompletions.add("creeper");
                tabCompletions.add("flame");
                tabCompletions.add("xbow");
            }
        }
        else if (level == 3) {
            if (perms >= 1) {
                if (args[1].equalsIgnoreCase("creeper")) {
                    tabCompletions = combatManager.getPresetIds(WeaponType.CREEPER);
                }
                else if (args[1].equalsIgnoreCase("flame")) {
                    tabCompletions = combatManager.getPresetIds(WeaponType.FLAME);
                }
                else if (args[1].equalsIgnoreCase("xbow")) {
                    tabCompletions = combatManager.getPresetIds(WeaponType.XBOW);
                }
            }
        }

        return tabCompletions;
    }

}
