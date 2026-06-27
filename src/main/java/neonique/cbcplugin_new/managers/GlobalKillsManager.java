package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GlobalKillsManager {

    private final Map<UUID, Integer> playerKillsCache;

    public GlobalKillsManager () {

        playerKillsCache = new HashMap<>();

    }

    /**
     * Load a player's global kills from the file and add it to the cache.
     * @param player The player whose global kills is to be loaded.
     */
    public int loadPlayerGlobalKills (OfflinePlayer player) {

        File file = getFile();
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        // Get section of file that stores global kills
        ConfigurationSection playerKillsSection = getPlayerKillsSection(fileConfig);

        int globalKills = 0;
        for (String key : playerKillsSection.getValues(false).keySet()) {
            UUID keyUUID = UUID.fromString(key);
            if (keyUUID.equals(player.getUniqueId())) {
                // Get player kills for this UUID
                globalKills = playerKillsSection.getInt(key);
                break;
            }
        }

        CBCPlugin.getPlugin().getLogger().info(
                player.getName() + " has " + globalKills + " global kills logged"
        );

        playerKillsCache.put(player.getUniqueId(), globalKills);

        return globalKills;

    }

    public int getPlayerKills (OfflinePlayer player) {

        if (playerKillsCache.containsKey(player.getUniqueId())) {
            return playerKillsCache.get(player.getUniqueId());
        }
        else {
            return loadPlayerGlobalKills(player);
        }

    }

    public void addPlayersKills (Collection<? extends CBCPlayer> players) {

        File file = getFile();
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        // Get section of file that stores global kills
        ConfigurationSection playerKillsSection = getPlayerKillsSection(fileConfig);

        // For each player save amount
        for (CBCPlayer player : players) {
            OfflinePlayer playerEntity = player.getOfflinePlayer();

            int gameKills = player.getKills();
            // Get player's current kills
            int currentKills = getPlayerKills(playerEntity);
            int newGlobalKills = currentKills + gameKills;

            if (gameKills == 1) {
                CBCPlugin.getPlugin().getLogger().info(player.name() + " had 1 game kill, increasing their total to " + newGlobalKills);
            }
            else {
                CBCPlugin.getPlugin().getLogger().info(player.name() + " had " + gameKills + " game kills, increasing their total to " + newGlobalKills);
            }

            // Save player kills to the global kills section
            savePlayerKillsToSection(playerKillsSection, playerEntity, newGlobalKills);

        }

        try {
            fileConfig.save(file);
        } catch (IOException e) {
            CBCPlugin.getPlugin().getLogger().warning("Failed to save global kills of players after game is over");
        }

    }

    /**
     * Save a player's global kills to the file
     * @param player The player whose global kills are being saved.
     * @param globalKills The pattern of the trim the player has selected.
     */
    public void savePlayerKillsToSection (ConfigurationSection playerKillsSection, OfflinePlayer player, int globalKills) {

        // Save new amount
        String playerUUIDString = player.getUniqueId().toString();
        playerKillsSection.set(playerUUIDString, globalKills);

        // Save into cache
        playerKillsCache.put(player.getUniqueId(), globalKills);

    }

    public ConfigurationSection getPlayerKillsSection (YamlConfiguration fileConfig) {

        // Check if section exists
        ConfigurationSection playerKillsSection = fileConfig.getConfigurationSection("PlayerGlobalKills");
        if (playerKillsSection == null) {
            // Create section if section does not exist
            playerKillsSection = fileConfig.createSection("PlayerGlobalKills");
        }
        return playerKillsSection;

    }

    /**
     * Retrieve the file that stores all global kills information.
     * @return File
     */
    public File getFile () {

        File dataFolder = CBCPlugin.getPlugin().getDataFolder();

        String killsFileName = "globalkills.yaml";
        File globalKillsFile = new File(dataFolder, killsFileName);
        if (!globalKillsFile.exists()) {

            CBCPlugin.getPlugin().getLogger().info(killsFileName + " not detected! Attempting to create one...");

            // Create the file
            try{
                boolean works = globalKillsFile.createNewFile(); // and here
                if (!works) {
                    CBCPlugin.getPlugin().getLogger().warning("Failed to generate " + killsFileName + " file");
                } else {
                    CBCPlugin.getPlugin().getLogger().info("Successfully generated " + killsFileName + " file");
                    return globalKillsFile;
                }
            } catch(SecurityException | IOException e) {
                CBCPlugin.getPlugin().getLogger().warning("Failed to generate  " + killsFileName + " file");
            }
            return null;
        }

        // Return the file
        return globalKillsFile;
    }

}
