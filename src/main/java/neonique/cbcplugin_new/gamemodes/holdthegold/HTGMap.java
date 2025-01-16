package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class HTGMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "GoldSpawn", "TeamSpawns"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    private Vector goldSpawn;
    private HashMap<String, Vector> teamSpawns;

    // Ally radius and enemy radius
    private final int allyRadius;
    private final int enemyRadius;
    private final int spawnGoldRadius;

    public HTGMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                  GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);

        Set<String> keys = gamemodeYml.getKeys(false);

        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        assert gamemodeYml.getConfigurationSection("TeamSpawns") != null;
        teamSpawns = getVectorHashMapFromStrings(Objects.requireNonNull(gamemodeYml.getConfigurationSection("TeamSpawns")).getValues(false));

        goldSpawn = convertStringToVector(Objects.requireNonNull(gamemodeYml.getString("GoldSpawn")));

        allyRadius = gamemodeYml.getInt("AllyRadius", 15);
        enemyRadius = gamemodeYml.getInt("EnemyRadius", 15);
        spawnGoldRadius = gamemodeYml.getInt("SpawnGoldRadius", 60);
    }

    public List<HTGSpawn> getHTGSpawns () {

        List<HTGSpawn> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new HTGSpawn(this.getGameManager().getWorld(), spawnVector, getGameManager(), allyRadius, enemyRadius, spawnGoldRadius, isIgnoreYInSpawnCalculations()));
        }
        return spawns;
    }

    public HashMap<String, Location> getTeamSpawns () {

        HashMap<String, Location> teamSpawnHashMap = new HashMap<>();
        for (String teamId : teamSpawns.keySet()) {
            Vector vector = teamSpawns.get(teamId);
            teamSpawnHashMap.put(teamId, new Location(this.getGameManager().getWorld(),
                    vector.getX(), vector.getY(), vector.getZ()));
        }
        return teamSpawnHashMap;

    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

    public Location getGoldSpawn() {
        return new Location(getWorld(), goldSpawn.getX() + 0.5, goldSpawn.getY(), goldSpawn.getZ() + 0.5);
    }
}
