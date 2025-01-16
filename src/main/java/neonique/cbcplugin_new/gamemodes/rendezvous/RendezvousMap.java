package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class RendezvousMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams", "TeamStartSpawns", "CheckpointDistanceMin",
            "CheckpointDistanceMax", "CheckpointLocations"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Map spawns
    private final HashMap<String, Set<Vector>> teamStartSpawns;

    // Checkpoint related maps
    private Set<Vector> checkpointLocations;
    private final double checkpointRadius;
    private final double checkpointDistanceMin;
    private final double checkpointDistanceMax;
    private final boolean isFinalCheckpointEnabled;
    private Vector finalCheckpoint;
    private double finalCheckpointRadius;

    public RendezvousMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                         GameManager gameManager, CombatManager combatManager) {
        super(ymlConfig, gameManager, combatManager);
        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        // Team spawns - where the teams will first spawn
        assert gamemodeYml.getConfigurationSection("TeamStartSpawns") != null;
        teamStartSpawns = new HashMap<>();
        ConfigurationSection baseSpawnSection = gamemodeYml.getConfigurationSection("TeamStartSpawns");
        assert baseSpawnSection != null;
        for (String teamName : baseSpawnSection.getValues(false).keySet()) {
            teamStartSpawns.put(teamName, getVectorSetFromStrings(baseSpawnSection.getStringList(teamName)));
        }

        // Checkpoint locations - locations of checkpoints
        List<String> checkpointStringList = gamemodeYml.getStringList("CheckpointLocations");
        checkpointLocations = getVectorSetFromStrings(checkpointStringList);

        // Checkpoint distance min and max -- this is used when selecting a checkpoint
        checkpointDistanceMin = gamemodeYml.getDouble("CheckpointDistanceMin", 100);
        checkpointDistanceMax = gamemodeYml.getDouble("CheckpointDistanceMax", 175);

        // Checkpoint radius
        checkpointRadius = gamemodeYml.getDouble("CheckpointRadius", 2.5);

        // Final checkpoint
        String finalCheckpointLocation = gamemodeYml.getString("FinalCheckpoint", null);
        if (finalCheckpointLocation == null) {
            isFinalCheckpointEnabled = false;
        }
        else {
            isFinalCheckpointEnabled = true;
            finalCheckpoint = convertStringToVector(finalCheckpointLocation).add(new Vector(0.5, 0.0, 0.5));
            finalCheckpointRadius = gamemodeYml.getDouble("FinalCheckpointRadius", checkpointRadius);
        }
    }

    public HashMap<String, Set<Location>> getTeamStartSpawns () {
        HashMap<String, Set<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamStartSpawns.keySet()) {
            Set<Location> spawnLocations = new HashSet<>();
            for (Vector spawn : teamStartSpawns.get(teamName)) {
                spawnLocations.add(new Location(this.getGameManager().getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public List<RendezvousSpawn> getRandomSpawns () {
        List<RendezvousSpawn> spawns = new ArrayList<>();
        for (Vector spawnVector : getSpawnpointCoordinates()) {
            spawns.add(new RendezvousSpawn(this.getGameManager().getWorld(), spawnVector, getGameManager()));
        }
        return spawns;
    }

    public List<RendezvousCheckpoint> getCheckpoints () {

        // Recalculate checkpoints
        RendezvousCheckpoint.recalculateCirclePositions();

        List<RendezvousCheckpoint> checkpoints = new ArrayList<>();
        for (Vector spawnVector : checkpointLocations) {
            checkpoints.add(new RendezvousCheckpoint(this.getGameManager().getWorld(), spawnVector, getGameManager(), checkpointRadius));
        }
        return checkpoints;
    }

    public double getCheckpointDistanceMin() {
        return checkpointDistanceMin;
    }

    public double getCheckpointDistanceMax() {
        return checkpointDistanceMax;
    }

    public boolean isFinalCheckpointEnabled() {
        return isFinalCheckpointEnabled;
    }

    public RendezvousCheckpoint getFinalCheckpoint() {
        return new RendezvousCheckpoint(this.getGameManager().getWorld(), finalCheckpoint, getGameManager(), finalCheckpointRadius);
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }
}
