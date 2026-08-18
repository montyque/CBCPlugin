package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

public record VoidMechanicSpec (double planeHeight, Vector teleport) implements MapMechanicSpec {

    public static VoidMechanicSpec fromConfig (ConfigurationSection config) {

        return new VoidMechanicSpec(
                ConfigUtil.requireDouble(config, "plane_height"),
                ConfigUtil.requireVector(config, "teleport_location")
        );

    }

    @Override
    public MapMechanic createMechanic (World world) {
        return new VoidMechanic(VectorUtil.vecToLocation(teleport, world), planeHeight);
    }

}
