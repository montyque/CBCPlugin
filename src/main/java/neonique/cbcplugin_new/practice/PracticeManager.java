package neonique.cbcplugin_new.practice;

import dev.jorel.commandapi.CommandAPI;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PracticeManager {

    private final Plugin plugin;
    private final UUID worldUUID;
    private final CBCScoreboardManager scoreboardManager;
    private final MapRepository maps;

    private AreaEffectCloud hologram;

    private PracticeInstance currentInstance = null;

    private final Location portalLocation;
    private final Location hologramLocation;
    private final Location teleportLocation;

    // Tasks and listeners
    private PracticePortalListener portalListener;

    public PracticeManager (Plugin plugin,
                            World world,
                            CBCScoreboardManager scoreboardManager,
                            MapRepository maps,
                            Location portalLocation,
                            Location hologramLocation,
                            Location teleportLocation) {

        this.plugin = plugin;
        this.worldUUID = world.getUID();
        this.scoreboardManager = scoreboardManager;
        this.maps = maps;
        this.portalLocation = portalLocation;
        this.hologramLocation = hologramLocation;
        this.teleportLocation = teleportLocation;

        createHologram();
        updateHologram();

    }

    public void createHologram () {

        Collection<AreaEffectCloud> entities = hologramLocation.getNearbyEntitiesByType(AreaEffectCloud.class, 1);
        entities.forEach(Entity::remove);

        hologram = (AreaEffectCloud) getWorld().spawnEntity(hologramLocation,
                EntityType.AREA_EFFECT_CLOUD,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
                e -> {
                    AreaEffectCloud h = (AreaEffectCloud) e;
                    h.setCustomNameVisible(true);
                    h.clearCustomEffects();
                    h.setRadius(0);
                    h.setDuration(300000000);
                });

        updateHologram();

    }

    public void updateHologram () {

        if (hologram == null) return;

        if (currentInstance == null) {
            hologram.customName(Component.text("Practice Arena Closed"));
        } else {
            hologram.customName(Component.text("Practice Arena Open"));
        }

    }

    public void newInstance (CBCMapData map) {

        currentInstance = new PracticeInstance(plugin, getWorld(), scoreboardManager);
        currentInstance.activate();
        currentInstance.setMap(new CBCMap(getWorld(), map));

        updateHologram();

        // Start new player teleport task
        portalListener = new PracticePortalListener(portalLocation, this::playerJoin);
        plugin.getServer().getPluginManager().registerEvents(portalListener, plugin);

        for (Player p : Bukkit.getOnlinePlayers()) {
            CommandAPI.updateRequirements(p);
        }

    }

    public void endInstance () {

        for (PracticePlayer player : currentInstance.players()) {
            if (player.isOnline()) {
                currentInstance.playerLeave(player.getPlayer());
                resetPlayer(player.getPlayer());
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            CommandAPI.updateRequirements(p);
        }

        currentInstance.deactivate();
        HandlerList.unregisterAll(portalListener);

        currentInstance = null;

    }

    public void playerJoin (Player player) {
        if (instanceHasPlayer(player)) return;
        currentInstance.playerJoin(player);
    }

    public void playerLeave (Player player) {
        currentInstance.playerLeave(player);
    }

    public boolean instanceHasPlayer (Player player) {
        return instanceActive() && currentInstance.hasPlayer(player);
    }

    public boolean instanceActive () {
        return currentInstance != null;
    }

    public void resetPlayer (Player player) {

        player.clearTitle();
        player.setHealth(20);
        player.getInventory().clear();
        player.updateInventory();
        player.removeScoreboardTag("NVDisable");
        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(teleportLocation);

    }

    public void changeMap (CBCMap map) {
        currentInstance.setMap(map);
    }

    public World getWorld () {
        return Bukkit.getWorld(worldUUID);
    }

    public Map<String, CBCMapData> getPracticeMaps () {
        return maps.allMaps();
    }

}
