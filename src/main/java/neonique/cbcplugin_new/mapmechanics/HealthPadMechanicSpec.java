package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.List;

public record HealthPadMechanicSpec(int cooldownTicks,
                                    int healing,
                                    List<Vector> locations) implements MapMechanicSpec {

    public static final double DEFAULT_COOLDOWN = 20.0;
    public static final int DEFAULT_HEALING = 6;

    public static HealthPadMechanicSpec fromConfig (ConfigurationSection config) {

        // Load the heal pad cooldown
        double cooldownSecs = ConfigUtil.getDouble(config, "cooldown").orElse(DEFAULT_COOLDOWN);
        int cooldownTicks = (int) Math.round(cooldownSecs * 20);

        // Load the healing amount
        int healing = ConfigUtil.getInt(config, "healing").orElse(DEFAULT_HEALING);

        // Load the healing pad locations
        List<Vector> healPadLocations = ConfigUtil.requireVectorList(config, "locations");

        return new HealthPadMechanicSpec(cooldownTicks, healing, healPadLocations);

    }

    @Override
    public MapMechanic createMechanic (World world) {

        List<HealthPad> healthPads = locations.stream()
                .map(v -> new Location(world, v.getX(), v.getY(), v.getZ()))
                .map(l -> new HealthPad(l, cooldownTicks, healing))
                .toList();

        return new HealthPadMechanic(healthPads);

    }
}
