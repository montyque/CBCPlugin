package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.enums.DeathBorderShape;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gameobjects.FFASpawnpoint;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class ThrowdownMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "SuddenDeath"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Sudden death options
    private final boolean suddenDeathEnabled;
    private int suddenDeathTimer;
    private boolean suddenDeathBorderEnabled;
    private DeathBorderShape suddenDeathBorderShape;
    private int suddenDeathBorderStartRadius;
    private int suddenDeathBorderShrinkRate;
    private int suddenDeathBorderUpwardsLimit;
    private int suddenDeathBorderDownwardsLimit;
    private int suddenDeathBorderRadiusLimit;

    private final Set<Vector> spawnOverrideSet;

    public ThrowdownMap(YamlConfiguration baseYml, YamlConfiguration gamemodeYml,
                       GameManager gameManager, CombatManager combatManager) {

        super(baseYml, gameManager, combatManager);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        suddenDeathEnabled = gamemodeYml.getBoolean("SuddenDeath");
        if (suddenDeathEnabled) {
            // Setup sudden death variables if sudden death is enabled
            suddenDeathTimer = gamemodeYml.getInt("SuddenDeathTimer");
            suddenDeathBorderEnabled = gamemodeYml.getBoolean("SuddenDeathBorder");
            if (suddenDeathBorderEnabled) {
                suddenDeathBorderShape = DeathBorderShape.valueOf(gamemodeYml.getString("SuddenDeathBorderShape", "CIRCLE").toUpperCase());
                suddenDeathBorderStartRadius = gamemodeYml.getInt("SuddenDeathBorderStartRadius");
                suddenDeathBorderShrinkRate = gamemodeYml.getInt("SuddenDeathBorderShrinkRate");
                suddenDeathBorderUpwardsLimit = gamemodeYml.getInt("SuddenDeathBorderUpwardsLimit");
                suddenDeathBorderDownwardsLimit = gamemodeYml.getInt("SuddenDeathBorderDownwardsLimit");
                suddenDeathBorderRadiusLimit = gamemodeYml.getInt("SuddenDeathBorderRadiusLimit", 16);
            }
        }

        // Get FFA spawn point coordinates
        List<String> spawnOverrides = gamemodeYml.getStringList("SpawnOverrides");
        spawnOverrideSet = getVectorSetFromStrings(spawnOverrides);

    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

    public boolean isSuddenDeathEnabled() {
        return suddenDeathEnabled;
    }

    public boolean isSuddenDeathBorderEnabled() {
        return suddenDeathBorderEnabled;
    }

    public Integer getSuddenDeathTimer() {
        if (suddenDeathEnabled) return suddenDeathTimer; else return null;
    }

    public Integer getSuddenDeathBorderStartRadius() {
        if (suddenDeathBorderEnabled) return suddenDeathBorderStartRadius; else return null;
    }

    public Integer getSuddenDeathBorderShrinkRate () {
        if (suddenDeathBorderEnabled) return suddenDeathBorderShrinkRate; else return null;
    }

    public DeathBorderShape getSuddenDeathBorderShape () {
        if (suddenDeathBorderEnabled) return suddenDeathBorderShape; else return null;
    }

    public int getSuddenDeathBorderUpwardsLimit() {
        return suddenDeathBorderUpwardsLimit;
    }

    public int getSuddenDeathBorderDownwardsLimit() {
        return suddenDeathBorderDownwardsLimit;
    }

    public int getSuddenDeathBorderRadiusLimit() {
        return suddenDeathBorderRadiusLimit;
    }

    public List<FFASpawnpoint> getOverrideSpawns() {
        List<FFASpawnpoint> spawnpoints = new ArrayList<>();
        for (Vector spawnpoint : spawnOverrideSet) {
            spawnpoints.add(new FFASpawnpoint(getGameManager(), spawnpoint));
        }
        return spawnpoints;
    }
}
