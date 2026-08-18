package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.mapmechanics.*;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MapMechanicLoader {

    private final Map<String, Function<ConfigurationSection, MapMechanicSpec>> mechanicConfigFactories = new HashMap<>();

    public MapMechanicLoader () {
        mechanicConfigFactories.put("health_pad", HealthPadMechanicSpec::fromConfig);
        mechanicConfigFactories.put("void", VoidMechanicSpec::fromConfig);
    }

    public MapMechanicSpec fromConfig (ConfigurationSection config) {

        String type = ConfigUtil.requireString(config, "type");
        if (!mechanicConfigFactories.containsKey(type))
            throw new InvalidMapMechanicConfigException(type);

        try {
            return mechanicConfigFactories.get(type).apply(config);
        } catch (Exception e) {
            throw new InvalidMapMechanicConfigException(type, e);
        }

    }

}