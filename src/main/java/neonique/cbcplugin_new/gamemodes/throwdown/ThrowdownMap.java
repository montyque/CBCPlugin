package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.mechanics.DeathBorderShape;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
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
    private DeathBorder.DeathBorderShape suddenDeathBorderShape;
    private int suddenDeathBorderStartRadius;
    private int suddenDeathBorderShrinkRate;
    private int suddenDeathBorderUpwardsLimit;
    private int suddenDeathBorderDownwardsLimit;
    private int suddenDeathBorderRadiusLimit;

    private final List<Vector> spawnOverrides;

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
                suddenDeathBorderShape = DeathBorder.DeathBorderShape.valueOf(gamemodeYml.getString("SuddenDeathBorderShape", "CIRCLE").toUpperCase());
                suddenDeathBorderStartRadius = gamemodeYml.getInt("SuddenDeathBorderStartRadius");
                suddenDeathBorderShrinkRate = gamemodeYml.getInt("SuddenDeathBorderShrinkRate");
                suddenDeathBorderUpwardsLimit = gamemodeYml.getInt("SuddenDeathBorderUpwardsLimit");
                suddenDeathBorderDownwardsLimit = gamemodeYml.getInt("SuddenDeathBorderDownwardsLimit");
                suddenDeathBorderRadiusLimit = gamemodeYml.getInt("SuddenDeathBorderRadiusLimit", 16);
            }
        }

        // Get FFA spawn point coordinates
        List<String> spawnOverridesStr = gamemodeYml.getStringList("SpawnOverrides");
        spawnOverrides = VectorUtil.blockStrListToVecList(spawnOverridesStr);

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

    public List<FFASpawnpoint> getOverrideSpawns() {
        List<FFASpawnpoint> spawnpoints = new ArrayList<>();
        for (Vector v : spawnOverrides) {
            spawnpoints.add(new FFASpawnpoint(new Location(getWorld(), v.getX(), v.getY(), v.getZ())));
        }
        return spawnpoints;
    }

    public DeathBorder getSuddenDeathBorder (GameManager gameManager) {
        return new DeathBorder(
                gameManager, getMapCentre(), suddenDeathBorderShape, suddenDeathBorderStartRadius,
                suddenDeathBorderRadiusLimit, suddenDeathBorderUpwardsLimit,
                suddenDeathBorderDownwardsLimit, suddenDeathBorderShrinkRate
        );
    }
}
