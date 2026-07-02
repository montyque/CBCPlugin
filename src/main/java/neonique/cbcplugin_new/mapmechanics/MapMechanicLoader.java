package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.mapconfig.InvalidMapConfigException;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class MapMechanicLoader {

    private final Map<String, Consumer<ConfigurationSection>> mechanicVerifiers = new HashMap<>();
    private final Map<String, LoadMapMechanicFunction> mechanicFactories = new HashMap<>();

    public MapMechanicLoader () {

        mechanicVerifiers.put("health_pad", HealthPadMechanic::verifyConfig);
        mechanicFactories.put("health_pad", HealthPadMechanic::fromConfig);

    }

    public void verifyMechanic (ConfigurationSection config) {
        String type = ConfigUtil.requireString(config, "type");
        if (!mechanicVerifiers.containsKey(type))
            throw new InvalidMapMechanicConfigException(type);

        try {
            mechanicVerifiers.get(type).accept(config);
        } catch (Exception e) {
            throw new InvalidMapMechanicConfigException(type, e);
        }
    }

    public MapMechanic fromConfig (ConfigurationSection config, PlayerRegistry registry,
                                   CombatManager combatManager, World world) {

        String type = ConfigUtil.requireString(config, "type");
        if (!mechanicFactories.containsKey(type))
            throw new InvalidMapMechanicConfigException(type);

        try {
            return mechanicFactories.get(type).fromConfig(config, registry, combatManager, world);
        } catch (Exception e) {
            throw new InvalidMapMechanicConfigException(type, e);
        }

    }

    @FunctionalInterface
    interface LoadMapMechanicFunction {

        MapMechanic fromConfig (ConfigurationSection config, PlayerRegistry registry,
                                CombatManager combatManager, World world);


    }

}