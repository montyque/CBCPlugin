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
import neonique.cbcplugin_new.gamemodes.tdm.TDMSpawn;
import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownMap;
import neonique.cbcplugin_new.mapmechanics.DashPad;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.mapmechanics.HealthPad;
import neonique.cbcplugin_new.mapmechanics.JumpPad;
import neonique.cbcplugin_new.managers.DeathMessageGenerator;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class CBCMap {

    private static final String[] SET_VALUES = new String[] {
            "MapId", "Name", "Center", "BlockSymbol", "MapBoundaryLow", "MapBoundaryHigh", "HealingPads",
                "DefaultFFASpawns", "DefaultTeamSpawns", "MapMechanics"
    };
    private static final Set<String> REQUIRED_KEYS = new HashSet<>(Arrays.asList(SET_VALUES));

    private static final String[] TEAM_IDS = new String[] {
            "red", "blue", "green", "yellow", "cyan", "orange", "magenta", "purple"
    };

    private static final Set<String> TEAM_ID_SET = new HashSet<>(Arrays.asList(TEAM_IDS));

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
    private final List<YamlConfiguration> mechanicConfigs;

    // Map options
    private final List<Vector> healthPadCoordinates; // TODO: remove later
    private final List<Vector> ffaSpawnpointCoordinates; // TODO: remove later
    private final HashMap<String, List<Vector>> defaultTeamSpawnCoordinates; // TODO: remove later
    private final boolean ignoreYInSpawnCalculations;

    // Death message overrides that override default messages only on this map
    private final HashMap<DeathCause, DeathMessageGenerator> deathMessageOverrides;

    // Firework variables
    private final int fireworkSpawnRadius;
    private final int fireworkSpawnHeight; // This is relative to the center

    private int voidPlane = 0;
    private final boolean instakillLava; // TODO: remove later
    private boolean jumpPadsEnabled = false;
    private List<Vector> jumpPadCoordinates;
    private boolean dashPadsEnabled = false;
    private final List<Vector[]> dashPadCoordinates;
    private boolean swimTimerEnabled = false; // TODO: remove later
    private int swimTimerLength; // TODO: remove later
    private boolean blocksFillAtStart = false;
    private List<Vector> blocksFillList = new ArrayList<>();
    private Material materialAtStart = null;
    private Material materialAtEnd = null;
    private final boolean canTrapdoorsOpen;
    private final boolean nightVisionAlwaysDisabled;

    private final boolean isPracticeMap;

    public Vector convertStringToVector(String string) {

        String[] splitStr = string.split(" ");
        List<String> splitStrSet = new ArrayList<>(Arrays.asList(splitStr));

        try {
            return new Vector(
                    Double.parseDouble(splitStrSet.get(0)),
                    Double.parseDouble(splitStrSet.get(1)),
                    Double.parseDouble(splitStrSet.get(2))
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Set<Vector> getVectorSetFromStrings(List<String> strings) {
        Set<Vector> vectors = new HashSet<>();
        for (String string : strings) {
            // Try and catch used to make sure that no errors occur when parsing coordinates
            Vector vector = VectorUtil.strToVec(string);
            if (vector != null) {
                // Add vector into coordinates list
                vectors.add(vector.add(new Vector(0.5, 0.0, 0.5)));
            }
        }
        return vectors;
    }

    public Set<Vector> getVectorSetFromStrings(List<String> strings, boolean addY) {
        Set<Vector> vectors = new HashSet<>();
        for (String string : strings) {
            // Try and catch used to make sure that no errors occur when parsing coordinates
            Vector vector = VectorUtil.strToVec(string);
            if (vector != null) {
                // Add vector into coordinates list
                if (addY) {
                    vectors.add(vector.add(new Vector(0.5, 0.5, 0.5)));
                } else {
                    vectors.add(vector.add(new Vector(0.5, 0.0, 0.5)));
                }
            } else {
                // Print out exception
                CBCPlugin.getPlugin().getLogger().warning("ERROR above generating Map " + id + " while trying to parse coordinates " + string);
            }
        }
        return vectors;
    }

    public List<Vector> getVectorListFromStrings(List<String> strings) {
        List<Vector> vectors = new ArrayList<>();
        for (String string : strings) {
            // Try and catch used to make sure that no errors occur when parsing coordinates
            Vector vector = VectorUtil.strToVec(string);
            if (vector != null) {
                // Add vector into coordinates list
                vectors.add(vector.add(new Vector(0.5, 0.0, 0.5)));
            } else {
                // Print out exception
                CBCPlugin.getPlugin().getLogger().warning("ERROR above generating Map " + id + " while trying to parse coordinates " + string);
            }
        }
        return vectors;
    }

    public HashMap<String, Vector> getVectorHashMapFromStrings(java.util.Map<String, Object> strings) {
        HashMap<String, Vector> vectorMap = new HashMap<>();
        for (String string : strings.keySet()) {
            if (!(strings.get(string) instanceof String)) {
                continue;
            }
            // Try and catch used to make sure that no errors occur when parsing coordinates
            Vector vector = VectorUtil.strToVec((String) strings.get(string));
            if (vector != null) {
                // Add vector into coordinates list
                vectorMap.put(string, vector.add(new Vector(0.5, 0.0, 0.5)));
            } else {
                // Print out exception
                CBCPlugin.getPlugin().getLogger().warning("ERROR above generating Map " + id + " while trying to parse coordinates " + string);
            }
        }
        return vectorMap;
    }

    public CBCMap(YamlConfiguration ymlConfig, GameManager gameManager, CombatManager combatManager) {

        worldUUID = gameManager.getWorld().getUID();

        // Get map name
        id = ymlConfig.getString("MapId");
        name = ymlConfig.getString("Name");
        isPracticeMap = ymlConfig.getBoolean("PracticeMap", true);

        // Get block that represents map in inventory menu
        blockSymbol = Material.valueOf(ymlConfig.getString("BlockSymbol"));

        // Get center coordinates
        centerCoords = VectorUtil.strToVec(Objects.requireNonNull(ymlConfig.getString("Center"))).add(new Vector(0.5, 0.0, 0.5));

        // Get boundaries of map for chunk loading
        upperBound = VectorUtil.strToVec(Objects.requireNonNull(ymlConfig.getString("MapBoundaryLow")));
        lowerBound = VectorUtil.strToVec(Objects.requireNonNull(ymlConfig.getString("MapBoundaryHigh")));

        // Get health pad coordinates
        List<String> healPadCoordStringList = ymlConfig.getStringList("HealingPads");
        healthPadCoordinates = VectorUtil.blockStrListToVecList(healPadCoordStringList);

        // Get FFA spawn point coordinates
        List<String> spawnpointStringList = ymlConfig.getStringList("DefaultFFASpawns");
        ffaSpawnpointCoordinates = VectorUtil.blockStrListToVecList(spawnpointStringList);

        // Get team spawn coordinates
        assert ymlConfig.getConfigurationSection("DefaultTeamSpawns") != null;
        defaultTeamSpawnCoordinates = new HashMap<>();
        ConfigurationSection spawnSection = ymlConfig.getConfigurationSection("DefaultTeamSpawns");
        assert spawnSection != null;
        for (String teamName : spawnSection.getValues(false).keySet()) {
            defaultTeamSpawnCoordinates.put(teamName, VectorUtil.blockStrListToVecList(spawnSection.getStringList(teamName)));
        }

        ignoreYInSpawnCalculations = ymlConfig.getBoolean("IgnoreYInSpawnCalculations", false);

        // Victory firework celebration details
        fireworkSpawnRadius = 16;
        fireworkSpawnHeight = -4;

        // Get game mechanics
        ConfigurationSection gameMechanicsList = ymlConfig.getConfigurationSection("MapMechanics");
        assert gameMechanicsList != null;

        // Game and visual mechanics
        voidPlane = gameMechanicsList.getInt("VoidDeath", 0);
        instakillLava = gameMechanicsList.getBoolean("InstantLavaKill", false);
        canTrapdoorsOpen = ymlConfig.getBoolean("canTrapdoorsOpen", true);
        nightVisionAlwaysDisabled = ymlConfig.getBoolean("NightVisionAlwaysDisabled", false);

        // Death message overrides
        ConfigurationSection deathMessagesSection = ymlConfig.getConfigurationSection("DeathMessageOverrides");
        if (deathMessagesSection != null) {
            deathMessageOverrides = DeathMessageGenerator.loadDeathMessageGenerators(deathMessagesSection);
        } else {
            deathMessageOverrides = new HashMap<>();
        }

        // Jump pad mechanic
        if (gameMechanicsList.contains("JumpPad")) {
            jumpPadsEnabled = gameMechanicsList.getBoolean("JumpPad");
            if (jumpPadsEnabled) {
                List<String> jumpPadCoordStringList = ymlConfig.getStringList("JumpPads");
                jumpPadCoordinates = VectorUtil.strListToVecList(jumpPadCoordStringList, new Vector(1, 0, 1));
            } else {
                jumpPadCoordinates = new ArrayList<>();
            }
        }

        // Dash pad mechanic
        if (gameMechanicsList.contains("DashPad")) {
            dashPadsEnabled = gameMechanicsList.getBoolean("DashPad");
            if (dashPadsEnabled) {
                dashPadCoordinates = new ArrayList<>();
                ConfigurationSection dashPadSection = ymlConfig.getConfigurationSection("DashPads");
                if (dashPadSection != null) {
                    for (String key : dashPadSection.getKeys(false)) {
                        ConfigurationSection dashPadSubSection = dashPadSection.getConfigurationSection(key);
                        if (dashPadSubSection != null) {
                            try {
                                Vector[] dashPadInfo = new Vector[]{
                                        VectorUtil.strToVec(Objects.requireNonNull(dashPadSubSection.getString("StartBlock"))).add(new Vector(0.5, 0.5, 0.5)),
                                        VectorUtil.strToVec(Objects.requireNonNull(dashPadSubSection.getString("EndBlock"))).add(new Vector(0.5, 0.5, 0.5)),
                                        VectorUtil.strToVec(Objects.requireNonNull(dashPadSubSection.getString("Velocity"))),
                                };
                                dashPadCoordinates.add(dashPadInfo);
                            } catch (NullPointerException ignored) {}
                        }
                    }
                }
            } else {
                dashPadCoordinates = new ArrayList<>();
            }
        } else {
            dashPadCoordinates = new ArrayList<>();
        }

        // Swim timer mechanic
        if (gameMechanicsList.contains("SwimTimer")) {
            swimTimerEnabled = gameMechanicsList.getBoolean("SwimTimer");
            if (swimTimerEnabled) {
                swimTimerLength = ymlConfig.getInt("SwimTimerLength", 100);
            }
        }

        // Start block fill mechanic
        if (gameMechanicsList.contains("StartBlockFill")) {
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

    public Collection<HealthPad> getHealthPads() {
        List<HealthPad> healthPads = new ArrayList<>();
        for (Vector healthPadVector : healthPadCoordinates) {
            Location location = new Location(getWorld(), healthPadVector.getX(), healthPadVector.getY(), healthPadVector.getZ());
            healthPads.add(new HealthPad(location));
        }
        return healthPads;
    }

    public boolean isJumpPadsEnabled() {
        return jumpPadsEnabled;
    }

    public Collection<JumpPad> getJumpPads() {
        if (!jumpPadsEnabled) return null;
        List<JumpPad> jumpPads = new ArrayList<>();
        for (Vector jumpPadVector : jumpPadCoordinates) {
            Location location = new Location(getWorld(), jumpPadVector.getX(), jumpPadVector.getY(), jumpPadVector.getZ());
            jumpPads.add(new JumpPad(location));
        }
        return jumpPads;
    }

    public boolean isDashPadsEnabled() {
        return dashPadsEnabled;
    }

    public Collection<DashPad> getDashPads() {
        List<DashPad> dashPads = new ArrayList<>();
        for (Vector[] vec : dashPadCoordinates) {
            Location startLoc = new Location(getWorld(), vec[0].getX(), vec[0].getY(), vec[0].getZ());
            Location endLoc = new Location(getWorld(), vec[1].getX(), vec[1].getY(), vec[1].getZ());
            dashPads.add(new DashPad(startLoc, endLoc, vec[2]));
        }
        return dashPads;
    }

    public int getVoidPlane() {
        return voidPlane;
    }

    public List<FFASpawnpoint> getSpawns() {
        List<FFASpawnpoint> spawnpoints = new ArrayList<>();
        for (Vector spawnpoint : ffaSpawnpointCoordinates) {
            spawnpoints.add(new FFASpawnpoint(new Location(getWorld(), spawnpoint.getX(), spawnpoint.getY(), spawnpoint.getZ())));
        }
        return spawnpoints;
    }

    public void fillBlocksAtStart() {
        if (!blocksFillAtStart) return;
        // Go through every vector
        for (Vector blockVector : blocksFillList) {
            Block block = getWorld().getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
            block.setType(materialAtStart);
            block.getState().update();
        }
    }

    public void fillBlocksAtEnd() {
        if (!blocksFillAtStart) return;
        // Go through every vector
        for (Vector blockVector : blocksFillList) {
            Block block = getWorld().getBlockAt(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
            block.setType(materialAtEnd);
        }
    }

    public List<Vector> getSpawnpointCoordinates () {
        return ffaSpawnpointCoordinates;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getMapCentre() {
        return new Location(getWorld(), centerCoords.getX(), centerCoords.getY(), centerCoords.getZ());
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

    public boolean isSwimTimerEnabled() {
        return swimTimerEnabled;
    }

    public int getSwimTimerLength () {
        return swimTimerLength;
    }

    public boolean isInstaKillLava() {
        return instakillLava;
    }

    public boolean isIgnoreYInSpawnCalculations() {
        return ignoreYInSpawnCalculations;
    }

    public int getFireworkSpawnHeight() {
        return fireworkSpawnHeight;
    }

    public int getFireworkSpawnRadius() {
        return fireworkSpawnRadius;
    }

    public boolean hasAllTeamsSpawns () {

        for (String teamId : TEAM_ID_SET) {
            if (!defaultTeamSpawnCoordinates.containsKey(teamId)) {
                return false;
            }
        }
        return true;

    }

    public Map<String, List<Location>> getDefaultTeamSpawns() {
        World world = getWorld();
        Map<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : defaultTeamSpawnCoordinates.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : defaultTeamSpawnCoordinates.get(teamName)) {
                spawnLocations.add(new Location(world, spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
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

    public boolean isNightVisionAlwaysDisabled() {
        return nightVisionAlwaysDisabled;
    }

    public HashMap<DeathCause, DeathMessageGenerator> getDeathMessageOverrides() {
        return deathMessageOverrides;
    }
}
