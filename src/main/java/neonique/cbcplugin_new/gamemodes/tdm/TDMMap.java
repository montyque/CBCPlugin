package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class TDMMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "RandomSpawns", "TeamSpawns"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    private final Map<String, List<Vector>> teamSpawns;

    private final boolean randomSpawnsEnabled;
    // Ally radius and enemy radius -- THIS ONLY APPLIES IF "RandomSpawns" IS ENABLED
    private int allyRadius;
    private int enemyRadius;
    private int enemyTargetDistance;

    public TDMMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                  GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);

        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

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

        // Check if random spawns is enabled -- random spawns creates
        // FFA spawnpoints that are the same as used in Hold the Gold
        randomSpawnsEnabled = gamemodeYml.getBoolean("RandomSpawns", false);
        if (randomSpawnsEnabled) {
            allyRadius = gamemodeYml.getInt("AllyRadius", 15);
            enemyRadius = gamemodeYml.getInt("EnemyRadius", 15);
            enemyTargetDistance = gamemodeYml.getInt("DistanceEnemySpawnRadius", 60);
        }
    }

    public boolean isRandomSpawnsEnabled () {
        return randomSpawnsEnabled;
    }

    public List<TDMSpawn> getRandomSpawns (TDMGame game) {
        List<TDMSpawn> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new TDMSpawn(game, getWorld(), spawnVector, allyRadius,
                    enemyRadius, enemyTargetDistance, getMapCentre(), isIgnoreYInSpawnCalculations()));
        }
        return spawns;
    }

    public HashMap<String, List<Location>> getTeamSpawns () {
        HashMap<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamSpawns.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : teamSpawns.get(teamName)) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }
}
