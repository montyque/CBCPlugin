package neonique.cbcplugin_new.mapconfig;

// This class represents a CBC map - with a name, heal pads, other game mechanics, etc.

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.gamemodes.assassin.AssassinMap;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagMap;
import neonique.cbcplugin_new.gamemodes.ctf.CTFMap;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGMap;
import neonique.cbcplugin_new.gamemodes.kmation.KMationMap;
import neonique.cbcplugin_new.gamemodes.koth.KOTHMap;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousMap;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownMap;
import neonique.cbcplugin_new.gamemodes.tdm.TDMMap;
import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownMap;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.managers.DeathMessageGenerator;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class CBCMap {

    private static final String[] SET_VALUES = new String[] {
            "MapId", "Name", "Center", "BlockSymbol", "MapBoundaryLow", "MapBoundaryHigh", "HealingPads",
                "DefaultFFASpawns", "DefaultTeamSpawns", "MapMechanics"
    };
    private static final Set<String> REQUIRED_KEYS = new HashSet<>(Arrays.asList(SET_VALUES));

    // Important map information
    private final String id;
    private final String name;
    private final Material blockSymbol;

    private Integer minTeams; // TODO: remove later
    private Integer maxTeams; // TODO: remove later
    private List<String> teamsAllowed; // TODO: remove later

    // Map coordinates
    private final UUID worldUUID;
    private final Vector centerCoords;
    private final Vector lowerBound;
    private final Vector upperBound;

    // Gameplay information
    private final List<Vector> defaultSpawnCoords;
    private final Map<TeamColor, List<Vector>> defaultTeamSpawns;
    private final List<ConfigurationSection> mechanicConfigs;

    // Map options
    private final MapOptions options;

    // Death message overrides that override default messages only on this map
    private final Map<DeathCause, DeathMessageGenerator> deathMessageOverrides;

    // Firework variables
    private final int fireworkSpawnRadius = 16;
    private final int fireworkSpawnHeight = -4; // This is relative to the center


    // OLD
    private boolean blocksFillAtStart = false;
    private List<Vector> blocksFillList = new ArrayList<>();
    private Material materialAtStart = null;
    private Material materialAtEnd = null;
    private final boolean canTrapdoorsOpen;
    private final boolean nightVisionAlwaysDisabled;

    private boolean isPracticeMap = true;

    public CBCMap (World world, Configuration config, MapMechanicLoader mechanicLoader) {

        this.worldUUID = world.getUID();

        id = ConfigUtil.requireString(config, "id");

        // Parse map metadata
        name = ConfigUtil.requireString(config, "name");
        blockSymbol = ConfigUtil.requireEnum(config, "name", Material.class);

        // Parse center coordinates
        centerCoords = ConfigUtil.requireVector(config, "center");

        // Parse bounding box
        List<Vector> bounds = ConfigUtil.requireVectorList(config, "bounding_box");
        if (bounds.size() != 2) throw new ConfigUtil.InvalidConfigValueException(config, "bounding_box",
                "Bounding box must be a List<Vector> of size 2");
        lowerBound = new Vector(
                Math.min(bounds.get(0).getX(), bounds.get(1).getX()),
                Math.min(bounds.get(0).getY(), bounds.get(1).getY()),
                Math.min(bounds.get(0).getZ(), bounds.get(1).getZ())
        );
        upperBound = new Vector(
                Math.max(bounds.get(0).getX(), bounds.get(1).getX()),
                Math.max(bounds.get(0).getY(), bounds.get(1).getY()),
                Math.max(bounds.get(0).getZ(), bounds.get(1).getZ())
        );

        // Parse individual and team spawns
        defaultSpawnCoords = ConfigUtil.requireVectorList(config, "default_player_spawns");
        defaultTeamSpawns = loadTeamVectorList(ConfigUtil.requireConfigurationSection(config, "default_team_spawns"));

        // Parse map options
        options = ConfigUtil.getConfigurationSection(config, "map_options")
                .map(MapOptions::fromConfig)
                .orElse(MapOptions.DEFAULTS);

        // Parse map mechanics
        List<?> mechanicsSection = ConfigUtil.getList(config, "map_mechanics").orElse(List.of());
        mechanicConfigs = mechanicsSection.stream()
                .map(o -> {
                    if (!(o instanceof ConfigurationSection)) throw new ConfigUtil.InvalidConfigValueException(config,
                            "map_mechanics", "All values in map_mechanics must be of type ConfigurationSection");
                    return (ConfigurationSection) o;})
                .toList();

        // Verify that all map mechanics parse correctly
        mechanicLoader.verifyMapMechanicConfigs(mechanicConfigs);

        // Parse death message overrides
        deathMessageOverrides = ConfigUtil.getConfigurationSection(config, "death_message_overrides")
                .map(this::loadDeathMessageOverrides)
                .orElse(Map.of());

    }

    public Map<TeamColor, List<Vector>> loadTeamVectorList (ConfigurationSection section) {
        return Arrays.stream(TeamColor.values())
                .collect(Collectors.toMap(
                        t -> t,
                        t -> ConfigUtil.requireVectorList(section, t.name().toLowerCase())
                ));
    }

    public Map<DeathCause, DeathMessageGenerator> loadDeathMessageOverrides (ConfigurationSection section) {

        Map<DeathCause, DeathMessageGenerator> overrides = new HashMap<>();

        for (String key : section.getKeys(false)) {

            ConfigurationSection deathCauseSection = section.getConfigurationSection(key);
            if (deathCauseSection == null) continue;

            // Check if key matches with a value in the DeathCause enum
            DeathCause deathCause;
            try {
                deathCause = DeathCause.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigUtil.InvalidConfigValueException(section, key, e);
            }

            // Get all death messages for this death cause in string form
            // TODO: type check
            List<String> directStrings = deathCauseSection.getStringList("DIRECT");
            List<String> indirectStrings = deathCauseSection.getStringList("INDIRECT");
            List<String> indirectNoKillerStrings = deathCauseSection.getStringList("INDIRECT_NO_KILLER");

            // Create death message generator and link it to this DeathCause enum
            DeathMessageGenerator dmGen = new DeathMessageGenerator(directStrings, indirectStrings, indirectNoKillerStrings);
            overrides.put(deathCause, dmGen);

        }

        return overrides;

    }

    /**
     * This is the OLD constructor, used to
     */
    public CBCMap(YamlConfiguration ymlConfig, GameManager gameManager, CombatManager combatManager) {

        worldUUID = gameManager.getWorld().getUID();

        // Get map name
        id = ConfigUtil.requireString(ymlConfig, "MapId");
        name = ConfigUtil.requireString(ymlConfig, "Name");
        isPracticeMap = ConfigUtil.getBoolean(ymlConfig, "PracticeMap").orElse(true);

        // Get block that represents map in inventory menu
        blockSymbol = ConfigUtil.requireEnum(ymlConfig, "BlockSymbol", Material.class);

        // Get center coordinates
        centerCoords = VectorUtil.strToBlockVec(ConfigUtil.requireString(ymlConfig, "Center"));

        // Get boundaries of map for chunk loading
        lowerBound = VectorUtil.strToBlockVec(ConfigUtil.requireString(ymlConfig, "MapBoundaryLow"));
        upperBound = VectorUtil.strToBlockVec(ConfigUtil.requireString(ymlConfig, "MapBoundaryHigh"));
        mechanicConfigs = new ArrayList<>();

        // Get FFA spawn point coordinates
        List<String> spawnpointStringList = ConfigUtil.requireStringList(ymlConfig, "DefaultFFASpawns");
        defaultSpawnCoords = VectorUtil.blockStrListToVecList(spawnpointStringList);

        // Get team spawn point coordinates
        defaultTeamSpawns = new HashMap<>();
        ConfigurationSection spawnSection = ConfigUtil.requireConfigurationSection(ymlConfig, "DefaultTeamSpawns");
        for (String teamName : spawnSection.getValues(false).keySet()) {
            defaultTeamSpawns.put(TeamColor.valueOf(teamName.toUpperCase()),
                    VectorUtil.blockStrListToVecList(spawnSection.getStringList(teamName)));
        }

        // Parse game options
        nightVisionAlwaysDisabled = ymlConfig.getBoolean("NightVisionAlwaysDisabled", false);
        options = new MapOptions(-1, !nightVisionAlwaysDisabled);

        ConfigurationSection gameMechanicsList = ConfigUtil.requireConfigurationSection(ymlConfig, "MapMechanics");

        // Parse health pad into new map mechanic format
        List<String> healPadCoordStringList = ConfigUtil.getStringList(ymlConfig, "HealingPads").orElse(List.of());
        List<Vector> healthPadCoordinates = VectorUtil.blockStrListToVecList(healPadCoordStringList);
        ConfigurationSection healthPadMechanicSection = new MemoryConfiguration();
        healthPadMechanicSection.set("type", "health_pad");
        healthPadMechanicSection.set("locations", healthPadCoordinates.stream()
                .map(v -> List.of(v.getX(), v.getY(), v.getZ()))
                .toList());
        mechanicConfigs.add(healthPadMechanicSection);

        // TODO: Parse into map option
        // ignoreYInSpawnCalculations = ymlConfig.getBoolean("IgnoreYInSpawnCalculations", false);

        // Parse void mechanic into new map mechanic format
        double voidPlaneHeight = ConfigUtil.requireDouble(gameMechanicsList, "VoidDeath");
        ConfigurationSection voidMechanicSection = new MemoryConfiguration();
        voidMechanicSection.set("type", "void");
        voidMechanicSection.set("plane_height", voidPlaneHeight);
        voidMechanicSection.set("teleport_location", List.of(centerCoords.getX(), centerCoords.getY(), centerCoords.getZ()));
        mechanicConfigs.add(voidMechanicSection);

        // Parse jump pad mechanic into new map mechanic format
        if (gameMechanicsList.getBoolean("JumpPad", false)) {
            List<String> jumpPadCoordStringList = ConfigUtil.requireStringList(ymlConfig, "JumpPads");
            List<Vector> jumpPadCoordinates = VectorUtil.blockStrListToVecList(jumpPadCoordStringList);
            ConfigurationSection jumpPadMechanicSection = new MemoryConfiguration();
            jumpPadMechanicSection.set("type", "jump_pad");
            jumpPadMechanicSection.set("locations", jumpPadCoordinates.stream()
                    .map(v -> List.of(v.getX(), v.getY(), v.getZ()))
                    .toList());
            mechanicConfigs.add(jumpPadMechanicSection);
        }

        // Parse dash pad mechanic into new map mechanic format
        if (gameMechanicsList.getBoolean("DashPad", false)) {

            ConfigurationSection dashPads = ConfigUtil.requireConfigurationSection(ymlConfig, "DashPads");
            List<ConfigurationSection> dashPadConfigs = dashPads.getKeys(false).stream()
                    .map(k -> ConfigUtil.requireConfigurationSection(dashPads, k))
                    .map(c -> {
                            ConfigurationSection dashPadConfig = new MemoryConfiguration();
                            Vector rangeStart = VectorUtil.strToBlockVec(ConfigUtil.requireString(c, "StartBlock"));
                            Vector rangeEnd = VectorUtil.strToBlockVec(ConfigUtil.requireString(c, "EndBlock"));
                            Vector velocity = VectorUtil.strToBlockVec(ConfigUtil.requireString(c, "Velocity"));
                            dashPadConfig.set("range",
                                    List.of(VectorUtil.vecToList(rangeStart), VectorUtil.vecToList(rangeEnd)));
                            dashPadConfig.set("velocity", VectorUtil.vecToList(velocity));
                            return dashPadConfig;
                    })
                    .toList();

            ConfigurationSection dashPadMechanicSection = new MemoryConfiguration();
            dashPadMechanicSection.set("type", "dash_pad");
            dashPadMechanicSection.set("dash_pads", dashPadConfigs);

        }

        // TODO: Parse into map mechanic format
        canTrapdoorsOpen = ymlConfig.getBoolean("canTrapdoorsOpen", true);

        // Death message overrides
        deathMessageOverrides = ConfigUtil.getConfigurationSection(ymlConfig, "DeathMessageOverrides")
                .map(DeathMessageGenerator::loadDeathMessageGenerators)
                .orElse(Map.of());


        // Parse swim timer mechanic into new map mechanic format
        if (gameMechanicsList.getBoolean("SwimTimer", false)) {
            double swimTimerSeconds = (double) ConfigUtil.getInt(ymlConfig, "SwimTimerLength").orElse(120) / 20;
            ConfigurationSection swimTimerMechanicSection = new MemoryConfiguration();
            swimTimerMechanicSection.set("type", "swim_timer");
            swimTimerMechanicSection.set("length", swimTimerSeconds);
            mechanicConfigs.add(swimTimerMechanicSection);
        }

        // Parse lava death mechanic into new map mechanic format
        if (gameMechanicsList.getBoolean("InstantLavaKill", false)) {
            ConfigurationSection lavaMechanicSection = new MemoryConfiguration();
            lavaMechanicSection.set("type", "lava_kill");
            mechanicConfigs.add(lavaMechanicSection);
        }

        // Start block fill mechanic
        if (gameMechanicsList.getBoolean("StartBlockFill", false)) {
            blocksFillAtStart = gameMechanicsList.getBoolean("StartBlockFill", false);
            if (blocksFillAtStart) {
                List<String> blocksFillStringList = ymlConfig.getStringList("BlocksToFill");
                blocksFillList = VectorUtil.blockStrListToVecList(blocksFillStringList);
                materialAtStart = Material.valueOf(ymlConfig.getString("MaterialAtStart"));
                materialAtEnd = Material.valueOf(ymlConfig.getString("MaterialAtEnd"));
            }
        }
    }

    public World getWorld() {
        return CBCPlugin.getPlugin().getServer().getWorld(worldUUID);
    }

    public void loadMapChunks(boolean sendMessage) {

        int startChunkX = upperBound.getBlockX() >> 4;
        int startChunkZ = upperBound.getBlockZ() >> 4;
        int endChunkX = lowerBound.getBlockX() >> 4;
        int endChunkZ = lowerBound.getBlockZ() >> 4;
        World world = getWorld();

        CBCPlugin.getPlugin().getLogger().info("Loading chunks for map '" + getId() + "'");
        if (sendMessage) {
            world.sendMessage(
                    Component.text("Chunks for map " + getName() + " loading...").color(NamedTextColor.YELLOW)
            );
        }

        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load();
            }
        }

        CBCPlugin.getPlugin().getLogger().info("Finished loading chunks for map '" + getId() + "'");
        if (sendMessage) {
            world.sendMessage(
                    Component.text("Chunks for map " + getName() + " successfully loaded!").color(NamedTextColor.YELLOW)
            );
        }
    }

    public Material getBlockSymbol () {
        return blockSymbol;
    }

    public List<FFASpawnpoint> getSpawns () {
        return defaultSpawnCoords.stream()
                .map(v -> VectorUtil.vecToLocation(v, getWorld()))
                .map(FFASpawnpoint::new)
                .toList();
    }

    // TODO: refactor into map mechanic
    public void fillBlocksAtStart() {
        if (!blocksFillAtStart) return;
        // Go through every vector
        for (Vector blockVector : blocksFillList) {
            Block block = getWorld().getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
            block.setType(materialAtStart);
            block.getState().update();
        }
    }

    // TODO: refactor into map mechanic
    public void fillBlocksAtEnd() {
        if (!blocksFillAtStart) return;
        // Go through every vector
        for (Vector blockVector : blocksFillList) {
            Block block = getWorld().getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
            block.setType(materialAtEnd);
        }
    }

    public List<Vector> getSpawnpointCoordinates () {
        return defaultSpawnCoords;
    }

    public String getId () {
        return id;
    }

    public String getName () {
        return name;
    }

    public Location getMapCentre () {
        return VectorUtil.vecToLocation(centerCoords, getWorld());
    }

    public void setMinAndMaxTeams(int minTeams, int maxTeams) {
        this.minTeams = minTeams;
        this.maxTeams = maxTeams;
    }

    public void setTeamsAllowed(List<String> teamIdsAllowed) {
        teamsAllowed = teamIdsAllowed;
    }

    public List<String> getTeamsAllowed() {
        return teamsAllowed;
    }

    // Method to check if a yml file is a valid map
    public static boolean isYmlInvalidMap(YamlConfiguration ymlConfig) {

        Set<String> ymlKeys = ymlConfig.getKeys(false);

        // Check if yml keys contains required fields
        for (String requiredKey : REQUIRED_KEYS) {
            if (!ymlKeys.contains(requiredKey)) return true;
        }
        return false;
    }

    public static boolean isYmlInvalidGamemodeMap (YamlConfiguration ymlConfig, CBCGamemode gamemode) {

        Set<String> requiredKeys;

        if (gamemode == CBCGamemode.CTF) requiredKeys = CTFMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.SHOWDOWN) requiredKeys = ShowdownMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.HOLDTHEGOLD) requiredKeys = HTGMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.TDM) requiredKeys = TDMMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.THROWDOWN) requiredKeys = ThrowdownMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.KMATION) requiredKeys = KMationMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.RENDEZVOUS) requiredKeys = RendezvousMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.ASSASSIN) requiredKeys = AssassinMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.CBCTAG) requiredKeys = TagMap.getGmYamlRequiredKeys();
        else if (gamemode == CBCGamemode.KOTH) requiredKeys = KOTHMap.getGmYamlRequiredKeys();
        else return false;

        Set<String> ymlKeys = ymlConfig.getKeys(false);
        // Check if yml keys contains required fields
        for (String requiredKey : requiredKeys) {
            if (!ymlKeys.contains(requiredKey)) return false;
        }
        return true;
    }

    public static CBCMap createMap (CBCGamemode gamemode, YamlConfiguration baseYmlMapConfig, YamlConfiguration ymlMapConfig,
                                    GameManager gameManager, CombatManager combatManager) {

        CBCMap mapObj = null;

        if (gamemode == CBCGamemode.CTF) mapObj = new CTFMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.SHOWDOWN) mapObj = new ShowdownMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.HOLDTHEGOLD) mapObj = new HTGMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.TDM) mapObj = new TDMMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.THROWDOWN) mapObj = new ThrowdownMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.KMATION) mapObj = new KMationMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.RENDEZVOUS) mapObj = new RendezvousMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.ASSASSIN) mapObj = new AssassinMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.CBCTAG) mapObj = new TagMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);
        else if (gamemode == CBCGamemode.KOTH) mapObj = new KOTHMap(baseYmlMapConfig, ymlMapConfig, gameManager, combatManager);

        return mapObj;

    }

    public Integer getMinTeams() {
        return minTeams;
    }

    public Integer getMaxTeams() {
        return maxTeams;
    }

    // TODO: refactor this
    public boolean isIgnoreYInSpawnCalculations() {
        return false;
    }

    public int getFireworkSpawnHeight() {
        return fireworkSpawnHeight;
    }

    public int getFireworkSpawnRadius() {
        return fireworkSpawnRadius;
    }


    public boolean isPracticeMap() {
        return isPracticeMap;
    }

    public boolean isMapRush() {
        return Objects.equals(id, "_maprush");
    }

    public boolean isTrapdoorsOpening () {
        return canTrapdoorsOpen;
    }

    public Map<DeathCause, DeathMessageGenerator> getDeathMessageOverrides() {
        return deathMessageOverrides;
    }

    public List<ConfigurationSection> getMechanicConfigs () {
        return mechanicConfigs;
    }

}
