package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class KOTHMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "TeamSpawns", "HillSettings"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    private boolean onlyTeamSpawns; // True if players should only spawn in their team spawns after death
    private Map<String, List<Vector>> teamSpawns; // The locations of each team's spawns
    private List<Vector> randomSpawns; // If onlyTeamSpawns

    private Map<String, List<Vector>> blocksOnCapture; // The locations of each team's spawns

    private ConfigurationSection hillSettingsSection;

    public KOTHMap (YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                  GameManager gameManager, CombatManager combatManager) {

        super(ymlConfig, gameManager, combatManager);

        Set<String> keys = gamemodeYml.getKeys(false);

        // Get the minimum and maximum teams for this map
        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        // Get valid teams that can be played on this map
        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        // Team spawns - where the teams will first spawn
        assert gamemodeYml.getConfigurationSection("TeamSpawns") != null;
        teamSpawns = new HashMap<>();
        ConfigurationSection baseSpawnSection = gamemodeYml.getConfigurationSection("TeamSpawns");
        assert baseSpawnSection != null;
        for (String teamName : baseSpawnSection.getValues(false).keySet()) {
            teamSpawns.put(teamName, VectorUtil.blockStrListToVecList(baseSpawnSection.getStringList(teamName)));
        }

        // Random spawns - where players will spawn after death
        // Uses team spawns for normal spawns if random spawns are not specified
        List<String> randomSpawnsStringList = ymlConfig.getStringList("RandomSpawns");
        if (!randomSpawnsStringList.isEmpty()) {
            randomSpawns = VectorUtil.blockStrListToVecList(randomSpawnsStringList);
            onlyTeamSpawns = false;
        }
        else {
            onlyTeamSpawns = true;
        }

        // Find hill settings section, which has all the data for the hill
        assert gamemodeYml.getConfigurationSection("HillSettings") != null;
        hillSettingsSection = gamemodeYml.getConfigurationSection("HillSettings");

        // Blocks to change -- blocks that change color when the point is captured
        blocksOnCapture = new HashMap<>();
        ConfigurationSection blocksOnCaptureSection = gamemodeYml.getConfigurationSection("BlocksOnCapture");
        if (blocksOnCaptureSection != null) {
            for (String material : blocksOnCaptureSection.getValues(false).keySet()) {
                blocksOnCapture.put(material, VectorUtil.blockStrListToVecList(blocksOnCaptureSection.getStringList(material)));
            }
        }

    }

    public List<Location> getRandomSpawns () {
        List<Location> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new Location(getWorld(), spawnVector.getX(),
                    spawnVector.getY(), spawnVector.getZ()));
        }
        return spawns;
    }

    public Map<String, List<Location>> getTeamSpawns () {
        Map<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamSpawns.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : teamSpawns.get(teamName)) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public KOTHHill getHill () {

        // Get shape of zone
        HillShape zoneShape;
        try {
            zoneShape = HillShape.valueOf(hillSettingsSection
                    .getString("shape", "circle").toUpperCase()); // Radius of zone
        }
        // If invalid zone shape provided
        catch (IllegalArgumentException e) {
            zoneShape = HillShape.CIRCLE;
        }

        // Get center of zone
        Location zoneCenter;
        String zoneCenterString = hillSettingsSection.getString("center", "");
        if (zoneCenterString.isEmpty()) {
            // No center was given, so set it to the center of the map
            zoneCenter = getMapCentre();
        }
        else {
            Vector zoneCenterVector = VectorUtil.strToVec(zoneCenterString);
            zoneCenter = new Location(getWorld(), zoneCenterVector.getX() + 0.5, zoneCenterVector.getY(), zoneCenterVector.getZ() + 0.5);
        }

        double zoneRadius = hillSettingsSection.getDouble("radius", 7); // Radius of hill zone
        double zoneHeight = hillSettingsSection.getDouble("height", 15); // Height that zone extends from bottom of hill

        return new KOTHHill(zoneCenter, zoneShape, (float) zoneRadius, (float) zoneHeight);

    }

    public Map<String, List<Vector>> getBlocksOnCapture() {
        return blocksOnCapture;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

}
