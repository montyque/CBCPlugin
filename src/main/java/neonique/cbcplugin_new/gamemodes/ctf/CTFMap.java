package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.mechanics.DeathBorderShape;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class CTFMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "RandomBases",
            "FlagLocations", "BaseSpawns", "DKillRadius", "RespawnTimes"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Team bases
    private final boolean randomBases;
    private final boolean canMoveAtGameStart;
    private Map<String, Vector> teamFlagsWithKeys;
    private List<Vector> teamFlags;
    private Map<String, List<Vector>> teamSpawnsWithKeys;
    private List<List<Vector>> teamSpawns;

    // Respawn timers
    private final HashMap<Integer, Integer> respawnTimes;

    // Defensive kill radius
    private final int dKillRadius;

    // Sudden death variables
    private final boolean suddenDeathEnabled;
    private DeathBorderShape borderShape;
    private int startingBorderRadius;
    private int finalBorderRadius;
    private int borderShrinkRate;
    private int borderBottomY;
    private int borderTopY;

    public CTFMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                  GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);

        Set<String> keys = gamemodeYml.getKeys(false);

        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        dKillRadius = gamemodeYml.getInt("DKillRadius");

        // Setup flag vectors and spawns
        randomBases = gamemodeYml.getBoolean("RandomBases");
        canMoveAtGameStart = gamemodeYml.getBoolean("CanMoveAtGameStart", false);
        if (randomBases) {
            teamFlags = VectorUtil.blockStrListToVecList(gamemodeYml.getStringList("FlagLocations"));

            teamSpawns = new ArrayList<>();
            List<?> rawSpawnsList = gamemodeYml.getList("BaseSpawns");
            assert rawSpawnsList != null;

            for (Object obj : rawSpawnsList) {

                if (!(obj instanceof List<?> list)) continue;

                List<String> teamSpawnList = new ArrayList<>();
                for (Object e : list) {
                    if (e instanceof String s) {
                        teamSpawnList.add(s);
                    }
                }
                teamSpawns.add(VectorUtil.blockStrListToVecList(teamSpawnList));

            }

        } else {
            assert gamemodeYml.getConfigurationSection("FlagLocations") != null;
            teamFlagsWithKeys = VectorUtil.blockStrMapToVecMap(Objects.requireNonNull(gamemodeYml.getConfigurationSection("FlagLocations")).getValues(false));

            teamSpawnsWithKeys = new HashMap<>();
            assert gamemodeYml.getConfigurationSection("BaseSpawns") != null;
            ConfigurationSection baseSpawnSection = gamemodeYml.getConfigurationSection("BaseSpawns");
            assert baseSpawnSection != null;
            for (String teamName : baseSpawnSection.getValues(false).keySet()) {
                teamSpawnsWithKeys.put(teamName, VectorUtil.blockStrListToVecList(baseSpawnSection.getStringList(teamName)));
            }
        }

        // Setup respawn times
        respawnTimes = new HashMap<>();
        assert gamemodeYml.getConfigurationSection("RespawnTimes") != null;
        java.util.Map<String, Object> respawnTimeMap = Objects.requireNonNull(gamemodeYml.getConfigurationSection("RespawnTimes")).getValues(false);
        for (String playerCount : respawnTimeMap.keySet()) {
            respawnTimes.put(Integer.parseInt(playerCount), (Integer) respawnTimeMap.get(playerCount));
        }

        // Setup sudden death variables
        suddenDeathEnabled = gamemodeYml.getBoolean("SuddenDeathEnabled", false);
        System.out.println(suddenDeathEnabled);
        if (suddenDeathEnabled) {
            borderShape = DeathBorderShape.valueOf(gamemodeYml.getString("BorderShape", "CIRCLE"));
            startingBorderRadius = gamemodeYml.getInt("StartingBorderRadius", 120);
            finalBorderRadius = gamemodeYml.getInt("FinalBorderRadius", 15);
            borderShrinkRate = gamemodeYml.getInt("BorderShrinkRate", 12);
            borderBottomY = gamemodeYml.getInt("BorderBottomY", getMapCentre().getBlockY() - 12);
            borderTopY = gamemodeYml.getInt("BorderTopY", getMapCentre().getBlockY() + 24);
        }
    }

    public boolean isRandomBases() {
        return randomBases;
    }

    public List<Location> getFlagLocations() {
        List<Location> flagLocations = new ArrayList<>();
        for (Vector flagLocation : teamFlags) {
            flagLocations.add(new Location(getWorld(), flagLocation.getX(), flagLocation.getY(), flagLocation.getZ()));
        }
        return flagLocations;
    }

    public HashMap<String, Location> getFlagLocationsWithKeys() {
        HashMap<String, Location> flagLocations = new HashMap<>();
        for (String teamAssignedFlag : teamFlagsWithKeys.keySet()) {
            Vector vector = teamFlagsWithKeys.get(teamAssignedFlag);
            flagLocations.put(teamAssignedFlag, new Location(getWorld(),
                    vector.getX(), vector.getY(), vector.getZ()));
        }
        return flagLocations;
    }

    public List<List<Location>> getBaseSpawns() {
        List<List<Location>> spawnLocationList = new ArrayList<>();
        for (List<Vector> spawnSet : teamSpawns) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : spawnSet) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationList.add(spawnLocations);
        }
        return spawnLocationList;
    }

    public Map<String, List<Location>> getBaseSpawnsWithKeys() {
        Map<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamSpawnsWithKeys.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : teamSpawnsWithKeys.get(teamName)) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public int getdKillRadius() {
        return dKillRadius;
    }

    public HashMap<Integer, Integer> getRespawnTimes() {
        return respawnTimes;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

    public boolean isCanMoveAtGameStart() {
        return canMoveAtGameStart;
    }

    public boolean isSuddenDeathEnabled() {
        return suddenDeathEnabled;
    }

    public DeathBorderShape getBorderShape() {
        return borderShape;
    }

    public int getBorderTopY() {
        return borderTopY;
    }

    public int getBorderBottomY() {
        return borderBottomY;
    }

    public int getBorderShrinkRate() {
        return borderShrinkRate;
    }

    public int getStartingBorderRadius() {
        return startingBorderRadius;
    }

    public int getFinalBorderRadius() {
        return finalBorderRadius;
    }
}
