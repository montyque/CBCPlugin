package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerSession;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import neonique.cbcplugin_new.weapons.EquipmentFactory;
import neonique.cbcplugin_new.weapons.WeaponFactory;
import net.kyori.adventure.audience.Audience;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CombatSession {

    private final Plugin plugin;
    private final World world;
    private final PlayerSession<?> players;
    private final Audience audience;

    private final MapMechanicsManager mapMechanicsManager;

    private final List<Listener> listeners = new ArrayList<>();
    private final List<BukkitRunnable> tasks = new ArrayList<>();
    private int timer = 0;

    public CombatSession (Plugin plugin, World world, Audience audience, PlayerSession<?> players) {

        this.plugin = plugin;
        this.world = world;
        this.players = players;
        this.audience = audience;

        this.mapMechanicsManager = new MapMechanicsManager(players);

    }

    public void activate () {

    }

    public void deactivate () {

    }

}
