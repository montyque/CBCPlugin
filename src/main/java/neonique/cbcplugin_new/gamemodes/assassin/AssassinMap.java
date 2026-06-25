package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.core.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class AssassinMap extends CBCMap {
    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Ally radius and enemy radius
    private final int enemyRadius;
    private final int targetDistance;

    public AssassinMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                      GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);

        enemyRadius = gamemodeYml.getInt("EnemyRadius", 15);
        targetDistance = gamemodeYml.getInt("TargetSpawnDistance", 40);
    }

    public List<AssassinSpawn> getAssassinSpawns () {
        List<AssassinSpawn> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new AssassinSpawn(this.getGameManager().getWorld(), spawnVector, getGameManager(), enemyRadius,
                    isIgnoreYInSpawnCalculations()));
        }
        return spawns;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

    public int getTargetDistance() {
        return targetDistance;
    }
}
