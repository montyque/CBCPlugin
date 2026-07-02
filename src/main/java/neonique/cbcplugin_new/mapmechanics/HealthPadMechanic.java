package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public class HealthPadMechanic implements MapMechanic {

    private static final double DEFAULT_COOLDOWN = 20.0;
    private static final int DEFAULT_HEALING = 6;

    private final Collection<HealthPad> healthPads;

    private PlayerRegistry registry;
    private CombatManager combatManager;
    private BukkitRunnable updateTask;

    public HealthPadMechanic (Collection<HealthPad> healthPads) {
        this.healthPads = healthPads;
    }

    @Override
    public void activate (PlayerRegistry registry, CombatManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;
        enableAll();

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 1);
    }

    @Override
    public void deactivate() {
        disableAll();
        updateTask.cancel();
    }

    public void update () {
        for (HealthPad healPad : healthPads) {
            if (!healPad.isEnabled()) continue;
            if (healPad.isOnline()) {
                healPad.playParticles();
                healPad.playerCheck(registry);
            } else {
                healPad.decrementTimer();
            }
        }
    }

    public void enableAll () {
        healthPads.forEach(h -> h.enable(true));
    }

    public void disableAll () {
        healthPads.forEach(HealthPad::disable);
    }

    public static void verifyConfig (ConfigurationSection config) {
        ConfigUtil.requireVectorList(config, "locations"); // locations
    }


    public static HealthPadMechanic fromConfig (ConfigurationSection config, PlayerRegistry registry,
                                                CombatManager combatManager, World world) {

        // Load the heal pad cooldown
        double cooldownSecs = ConfigUtil.getDouble(config, "cooldown").orElse(DEFAULT_COOLDOWN);
        int cooldownTicks = (int) Math.round(cooldownSecs * 20);

        // Load the healing amount
        int healing = ConfigUtil.getInt(config, "healing").orElse(DEFAULT_HEALING);

        // Load the healing pad locations
        List<Vector> healPadLocations = ConfigUtil.requireVectorList(config, "locations");
        List<HealthPad> healthPads = healPadLocations.stream()
                .map(v -> v.add(VectorUtil.BLOCK_CENTER_OFFSET))
                .map(v -> new Location(world, v.getX(), v.getY(), v.getZ()))
                .map(l -> new HealthPad(l, cooldownTicks, healing))
                .toList();

        return new HealthPadMechanic(healthPads);

    }

}
