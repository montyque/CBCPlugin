package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapmechanics.MapMechanicSpec;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CBCMapData (String id,
                          String name,
                          Material blockSymbol,
                          Vector centerCoords,
                          Vector lowerBound,
                          Vector upperBound,
                          List<Vector> defaultSpawnCoords,
                          Map<TeamColor, List<Vector>> defaultTeamSpawns,
                          MapOptions options,
                          List<MapMechanicSpec> mechanicSpecs) {

    public static CBCMapData fromConfig (Configuration config, MapMechanicLoader mechanicLoader) {

        String id = ConfigUtil.requireString(config, "id");

        // Parse map metadata
        String name = ConfigUtil.requireString(config, "name");
        Material blockSymbol = ConfigUtil.requireEnum(config, "name", Material.class);

        // Parse center coordinates
        Vector centerCoords = ConfigUtil.requireVector(config, "center");

        // Parse bounding box
        List<Vector> bounds = ConfigUtil.requireVectorList(config, "bounding_box");
        if (bounds.size() != 2) throw new ConfigUtil.InvalidConfigValueException(config, "bounding_box",
                "Bounding box must be a List<Vector> of size 2");

        Vector lowerBound = new Vector(
                Math.min(bounds.get(0).getX(), bounds.get(1).getX()),
                Math.min(bounds.get(0).getY(), bounds.get(1).getY()),
                Math.min(bounds.get(0).getZ(), bounds.get(1).getZ())
        );
        Vector upperBound = new Vector(
                Math.max(bounds.get(0).getX(), bounds.get(1).getX()),
                Math.max(bounds.get(0).getY(), bounds.get(1).getY()),
                Math.max(bounds.get(0).getZ(), bounds.get(1).getZ())
        );

        // Parse individual and team spawns
        List<Vector> defaultSpawnCoords = ConfigUtil.requireVectorList(config, "default_player_spawns");
        Map<TeamColor, List<Vector>> defaultTeamSpawns = loadTeamVectorList(ConfigUtil.requireConfigurationSection(config, "default_team_spawns"));

        // Parse map options
        MapOptions options = ConfigUtil.getConfigurationSection(config, "map_options")
                .map(MapOptions::fromConfig)
                .orElse(MapOptions.DEFAULTS);

        // Parse map mechanics
        List<MapMechanicSpec> mechanicSpecs = mechanicsFromConfig(config, mechanicLoader);

        return new CBCMapData(
                id,
                name,
                blockSymbol,
                centerCoords,
                lowerBound,
                upperBound,
                defaultSpawnCoords,
                defaultTeamSpawns,
                options,
                mechanicSpecs
        );

    }

    private static List<MapMechanicSpec> mechanicsFromConfig (ConfigurationSection config,
                                                              MapMechanicLoader mechanicLoader) {
        return ConfigUtil.getList(config, "map_mechanics").orElse(List.of())
                .stream()
                .map(o -> {
                    if (!(o instanceof ConfigurationSection c)) throw new ConfigUtil.InvalidConfigValueException(config,
                            "map_mechanics", "All values in map_mechanics must be of type ConfigurationSection");
                    return mechanicLoader.fromConfig(c);})
                .toList();
    }

    public static Map<TeamColor, List<Vector>> loadTeamVectorList(ConfigurationSection section) {
        return Arrays.stream(TeamColor.values())
                .collect(Collectors.toMap(
                        t -> t,
                        t -> ConfigUtil.requireVectorList(section, t.name().toLowerCase())
                ));
    }

    /*
    public Map<DeathCause, DeathMessageGenerator> loadDeathMessageOverrides (ConfigurationSection section) {

        Map<DeathCause, DeathMessageGenerator> overrides = new HashMap<>();

        for (String key : section.getKeys(false)) {

            ConfigurationSection deathCauseSection = section.getConfigurationSection(key);
            if (deathCauseSection == null) continue;

            // Check if key matches with a value in the DeathCause enum
            DeathCause deathCause;
            try {
                deathCause = DeathCause.valueOf(key.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigUtil.InvalidConfigValueException(section, key, e);
            }

            // Get all death messages for this death cause in string form
            // TODO: type check
            List<String> directStrings = deathCauseSection.getStringList("DIRECT");
            List<String> indirectStrings = deathCauseSection.getStringList("INDIRECT");
            List<String> indirectNoKillerStrings = deathCauseSection.getStringList("INDIRECT_NO_KILLER");

            // Create death message generator and link it to this DeathCause enum
            DeathMessageGenerator dmGen = new DeathMessageGenerator(directStrings, indirectStrings, indirectNoKillerStrings);
            overrides.put(deathCause, dmGen);

        }

        return overrides;

    }*/

}
