package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.util.*;

public class TagMap extends CBCMap {

    static final String[] GM_YAML_SET_VALUES = new String[] {
            "MapId", "MinTeams", "MaxTeams", "ValidTeams"
    };
    static final Set<String> GM_YAML_REQUIRED_KEYS = new HashSet<>(Arrays.asList(GM_YAML_SET_VALUES));

    // Map spawns
    private final boolean canEvadersMoveAtRoundStart;

    // Evader spawns
    private final boolean randomEvaderSpawns;
    private List<List<Vector>> evaderSpawns;
    private Map<String, List<Vector>> teamEvaderSpawns;

    // Tagger spawns
    private final boolean equalTaggerSpawns;
    private List<Vector> taggerSpawns;
    private Map<String, List<Vector>> teamTaggerSpawns;

    public TagMap(YamlConfiguration ymlConfig, YamlConfiguration gamemodeYml,
                         GameManager gameManager, CombatManager combatManager) {

        super(ymlConfig, gameManager, combatManager);
        int minTeams = gamemodeYml.getInt("MinTeams");
        int maxTeams = gamemodeYml.getInt("MaxTeams");
        setMinAndMaxTeams(minTeams, maxTeams);

        List<String> allowedTeams = gamemodeYml.getStringList("ValidTeams");
        setTeamsAllowed(allowedTeams);

        canEvadersMoveAtRoundStart = gamemodeYml.getBoolean("CanEvadersMoveAtRoundStart", false);

        // Team evader spawns - where teams will first spawn on non tagger rounds
        randomEvaderSpawns = gamemodeYml.getBoolean("RandomEvaderSpawns", false);
        if (randomEvaderSpawns) {

            // Evader spawns are not attached to teams
            ConfigurationSection evaderSpawnSection = gamemodeYml.getConfigurationSection("EvaderSpawns");
            evaderSpawns = new ArrayList<>();

            assert evaderSpawnSection != null;
            for (String key : evaderSpawnSection.getValues(false).keySet()) {
                evaderSpawns.add(VectorUtil.blockStrListToVecList(evaderSpawnSection.getStringList(key)));
            }

        }
        else {

            // Evader spawns are attached to teams
            assert gamemodeYml.getConfigurationSection("TeamEvaderSpawns") != null;
            teamEvaderSpawns = new HashMap<>();
            ConfigurationSection baseSpawnSection = gamemodeYml.getConfigurationSection("TeamEvaderSpawns");
            assert baseSpawnSection != null;
            for (String teamName : baseSpawnSection.getValues(false).keySet()) {
                teamEvaderSpawns.put(teamName, VectorUtil.blockStrListToVecList(baseSpawnSection.getStringList(teamName)));
            }

        }

        // Team tagger spawns - where the teams will first spawn when they're a tagger
        equalTaggerSpawns = gamemodeYml.getBoolean("EqualTaggerSpawns", false);
        if (equalTaggerSpawns) {

            // Tagger spawns are the same for every team
            taggerSpawns = VectorUtil.blockStrListToVecList(gamemodeYml.getStringList("TaggerSpawns"));

        }
        else {

            // Tagger spawns are different for every team
            assert gamemodeYml.getConfigurationSection("TeamTaggerSpawns") != null;
            teamTaggerSpawns = new HashMap<>();
            ConfigurationSection baseTaggerSpawnSection = gamemodeYml.getConfigurationSection("TeamTaggerSpawns");
            assert baseTaggerSpawnSection != null;
            for (String teamName : baseTaggerSpawnSection.getValues(false).keySet()) {
                teamTaggerSpawns.put(teamName, VectorUtil.blockStrListToVecList(baseTaggerSpawnSection.getStringList(teamName)));
            }

        }
    }

    public Map<String, List<Location>> getTeamEvaderSpawns () {
        Map<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamEvaderSpawns.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : teamEvaderSpawns.get(teamName)) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public List<List<Location>> getEvaderSpawns () {
        List<List<Location>> allSpawnLocations = new ArrayList<>();
        for (List<Vector> spawns : evaderSpawns) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : spawns) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            allSpawnLocations.add(spawnLocations);
        }
        return allSpawnLocations;
    }

    public Map<String, List<Location>> getTeamTaggerSpawns () {
        Map<String, List<Location>> spawnLocationMap = new HashMap<>();
        for (String teamName : teamTaggerSpawns.keySet()) {
            List<Location> spawnLocations = new ArrayList<>();
            for (Vector spawn : teamTaggerSpawns.get(teamName)) {
                spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
            }
            spawnLocationMap.put(teamName, spawnLocations);
        }
        return spawnLocationMap;
    }

    public List<Location> getTaggerSpawns () {

        List<Location> spawnLocations = new ArrayList<>();
        if (taggerSpawns == null) return spawnLocations;

        for (Vector spawn : taggerSpawns) {
            spawnLocations.add(new Location(getWorld(), spawn.getX(), spawn.getY(), spawn.getZ()));
        }

        return spawnLocations;

    }

    public boolean isEvaderSpawnsRandom () {
        return randomEvaderSpawns;
    }

    public boolean isTaggerSpawnsEqual () {
        return equalTaggerSpawns;
    }

    public boolean canEvadersMoveAtRoundStart() {
        return canEvadersMoveAtRoundStart;
    }

    public static Set<String> getGmYamlRequiredKeys() {
        return GM_YAML_REQUIRED_KEYS;
    }

}
