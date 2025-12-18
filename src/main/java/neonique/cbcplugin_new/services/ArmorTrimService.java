package neonique.cbcplugin_new.services;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.listeners.lobby.TrimSelectHandler;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static neonique.cbcplugin_new.util.StringUtil.firstLetterUpper;

public class ArmorTrimService {

    public final static String playerTrimsFileName = "playertrims.yaml";

    private final Map<UUID, TrimPattern> playerTrims;
    private final TrimSelectHandler trimSelectHandler;

    private final TrimPattern defaultTrim = TrimPattern.COAST;

    public ArmorTrimService() {

        CBCPlugin plugin = CBCPlugin.getPlugin();

        playerTrims = new HashMap<>();

        // Start tasks and listeners
        trimSelectHandler = new TrimSelectHandler(this);
        plugin.getServer().getPluginManager().registerEvents(trimSelectHandler, plugin);

    }

    /**
     * Set's the player's selected trim pattern.
     * @param player The player who has selected the trim pattern.
     * @param trimPattern The pattern of the trim the player has selected.
     */
    public void setTrim (Player player, TrimPattern trimPattern) {

        // Add player's trim to trims map
        playerTrims.put(player.getUniqueId(), trimPattern);

        // Save player's trim (asynchronously, to not cause any issues)
        new BukkitRunnable() {
            @Override
            public void run () {
                savePlayerTrimToFile(player, trimPattern);
            }
        }.runTaskAsynchronously(CBCPlugin.getPlugin());

    }

    /**
     * Get a player's selected trim pattern.
     * @param playerUUID The UUID of player whose trim pattern is being retrieved.
     * @return The TrimPattern that the player has selected.
     */
    public TrimPattern getPlayerTrim (UUID playerUUID) {
        return playerTrims.getOrDefault(playerUUID, defaultTrim);
    }

    /**
     * Load a player's selected trim from the file and add it to the cache.
     * @param player The player whose trim will be loaded.
     */
    public void loadPlayerTrimFromFile (Player player) {

        File file = getPlayerTrimsFile();
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        String stringTrim = fileConfig.getString(player.getUniqueId().toString(), null);
        if (stringTrim == null) {
            CBCPlugin.getPlugin().getLogger().info(
                    player.getName() + " does not have a saved armor trim pattern"
            );
            return;
        }
        else {
            CBCPlugin.getPlugin().getLogger().info(
                    player.getName() + "'s saved armor trim loaded (" + firstLetterUpper(stringTrim) + ")"
            );
        }

        TrimPattern trimPattern = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN)
                .get(NamespacedKey.minecraft(stringTrim));

        playerTrims.put(player.getUniqueId(), trimPattern);

    }

    /**
     * Save a player's selected trim pattern to the file.
     * @param player The player whose trim should be saved.
     * @param trimPattern The pattern of the trim the player has selected.
     */
    public void savePlayerTrimToFile (Player player, TrimPattern trimPattern) {

        // Open the player trims file
        File file = getPlayerTrimsFile();
        YamlConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        // Find the key ID of the trim pattern
        NamespacedKey key = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).getKey(trimPattern);
        if (key == null) return;
        String trimId = key.getKey();

        // Save player UUID with trim id
        fileConfig.set(player.getUniqueId().toString(), trimId);

        try {
            fileConfig.save(file);
        } catch (IOException e) {
            CBCPlugin.getPlugin().getLogger().warning("Failed to save armor trims file after updating player "
                    + player.getName() + " armor trim to " + trimId);
        }

    }


    /**
     * Retrieve the file that stores every player's selected armor trims.
     * @return File
     */
    public File getPlayerTrimsFile () {

        File dataFolder = CBCPlugin.getPlugin().getDataFolder();

        File playerTrimsFile = new File(dataFolder, playerTrimsFileName);
        if (!playerTrimsFile.exists()) {

            CBCPlugin.getPlugin().getLogger().info(playerTrimsFileName + " not detected! Attempting to create one...");

            // Create the file
            try{
                boolean works = playerTrimsFile.createNewFile(); // and here
                if (!works) {
                    CBCPlugin.getPlugin().getLogger().warning("Failed to generate " + playerTrimsFileName + " file");
                } else {
                    CBCPlugin.getPlugin().getLogger().info("Successfully generated " + playerTrimsFileName + " file");
                    return playerTrimsFile;
                }
            } catch(SecurityException | IOException e) {
                CBCPlugin.getPlugin().getLogger().warning("Failed to generate  " + playerTrimsFileName + " file");
            }
            return null;
        }

        // Return the file
        return playerTrimsFile;
    }

}
