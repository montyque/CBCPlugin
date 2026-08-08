package neonique.cbcplugin_new.practice;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.mapconfig.CBCMap;
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

    private AreaEffectCloud hologram;

    private PracticeInstance currentInstance = null;

    private final Location portalLocation;
    private final Location hologramLocation;
    private final Location teleportLocation;

    // Tasks and listeners
    private PracticePortalListener portalListener;

    public PracticeManager (Plugin plugin,
                            World world,
                            Location portalLocation,
                            Location hologramLocation,
                            Location teleportLocation) {

        this.plugin = plugin;
        this.worldUUID = world.getUID();
        this.portalLocation = portalLocation;
        this.hologramLocation = hologramLocation;
        this.teleportLocation = teleportLocation;

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

    public void newInstance (CBCMap map) {

        currentInstance = new PracticeInstance(plugin, getWorld());
        currentInstance.activate();
        currentInstance.setMap(map);

        updateHologram();

        // Start new player teleport task
        portalListener = new PracticePortalListener(portalLocation, this::playerJoin);
        plugin.getServer().getPluginManager().registerEvents(portalListener, CBCPlugin.getPlugin());

    }

    public void endInstance () {

        for (PracticePlayer player : currentInstance.players()) {
            currentInstance.playerLeave(player.getPlayer());
            resetPlayer(player.getPlayer());
        }

        currentInstance.deactivate();
        HandlerList.unregisterAll(portalListener);

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

}
