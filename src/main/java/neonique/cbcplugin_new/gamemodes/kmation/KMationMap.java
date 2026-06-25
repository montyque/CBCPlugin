package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.core.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class KMationMap extends CBCMap {
    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "SpawnTargetRadius"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Ally radius and enemy radius
    private final int enemyRadius;
    private final int enemyTargetDistance;
    private final double averageY;

    public KMationMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                  GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);

        enemyRadius = gamemodeYml.getInt("EnemyRadius", 15);
        enemyTargetDistance = gamemodeYml.getInt("EnemyTargetDistance", 60);

        averageY = gamemodeYml.getDouble("AverageY", getMapCentre().getY());
    }

    public List<KMationSpawn> getKMationSpawns () {
        List<KMationSpawn> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new KMationSpawn(this.getGameManager().getWorld(), spawnVector, getGameManager(), enemyRadius,
                    enemyTargetDistance, isIgnoreYInSpawnCalculations(), getMapCentre(), averageY));
        }
        return spawns;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }
}
