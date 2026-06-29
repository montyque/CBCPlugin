package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.mechanics.DeathBorderShape;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.mechanics.GamemodeOptions;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class ShowdownMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "RandomTeamSpawns", "TeamSpawns", "SuddenDeath"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Team spawns
    private final boolean randomTeamSpawns;
    private boolean movingAllowedAtRoundStart = true;
    private boolean createBoxAtRoundStart = false;
    private Map<String, Vector> teamSpawnsWithKeys;
    private List<Vector> teamSpawns;

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

    public ShowdownMap(YamlConfiguration baseYml, YamlConfiguration gamemodeYml,
            GameManager gameManager, CombatManager combatManager) {

        super(baseYml, gameManager, combatManager);

        Set<String> keys = gamemodeYml.getKeys(false);

        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        if (keys.contains("MovingAllowedAtRoundStart")) {
            movingAllowedAtRoundStart = gamemodeYml.getBoolean("MovingAllowedAtRoundStart");
        }
        if (keys.contains("CreateBoxAtRoundStart")) {
            createBoxAtRoundStart = gamemodeYml.getBoolean("CreateBoxAtRoundStart");
        }

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

        // Setup spawnpoint vectors
        randomTeamSpawns = gamemodeYml.getBoolean("RandomTeamSpawns");
        if (randomTeamSpawns) {
            // Setup random team spawns using teamSpawns list
            teamSpawns = VectorUtil.blockStrListToVecList(gamemodeYml.getStringList("TeamSpawns"));
        } else {
            assert gamemodeYml.getConfigurationSection("TeamSpawns") != null;
            teamSpawnsWithKeys = VectorUtil.blockStrMapToVecMap(Objects.requireNonNull(gamemodeYml.getConfigurationSection("TeamSpawns")).getValues(false));
        }
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

    public boolean isRandomTeamSpawns() {
        return randomTeamSpawns;
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

    public DeathBorder getSuddenDeathBorder (GameManager gameManager) {
        return new DeathBorder(
                gameManager, getMapCentre(), suddenDeathBorderShape, suddenDeathBorderStartRadius,
                suddenDeathBorderRadiusLimit, suddenDeathBorderUpwardsLimit,
                suddenDeathBorderDownwardsLimit, suddenDeathBorderShrinkRate
        );
    }

    public List<ShowdownSpawn> getTeamSpawns() {
        List<ShowdownSpawn> spawnpoints = new ArrayList<>();
        for (Vector spawnpoint : teamSpawns) {
            spawnpoints.add(new ShowdownSpawn(getWorld(), spawnpoint, createBoxAtRoundStart));
        }
        return spawnpoints;
    }

    public HashMap<String, ShowdownSpawn> getTeamSpawnsWithKeys() {
        HashMap<String, ShowdownSpawn> spawnpoints = new HashMap<>();
        for (String teamAssignedSpawnpoint : teamSpawnsWithKeys.keySet()) {
            spawnpoints.put(teamAssignedSpawnpoint, new ShowdownSpawn(
                    getWorld(), teamSpawnsWithKeys.get(teamAssignedSpawnpoint), createBoxAtRoundStart));
        }
        return spawnpoints;
    }
}
