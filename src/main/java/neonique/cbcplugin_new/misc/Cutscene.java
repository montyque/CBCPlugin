package neonique.cbcplugin_new.misc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CutsceneType;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.CutsceneRunTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class Cutscene {

    private UUID worldUUID;

    // Cutscene information
    private boolean active;
    private int tickNum;
    private int length;

    // Task that runs the cutscene
    private CutsceneRunTask cutsceneTask;

    // Cutscene starting position
    private Location startingPosition;

    // Entity that is followed in cutscene
    private UUID spectateEntityUUID;

    // Players in cutscene
    private Set<UUID> playerUUIDs;

    public Cutscene (World world, Location startingPosition, int length, Collection<Player> players) {

        // Set fields
        this.active = false;
        this.length = length;
        this.tickNum = 0;
        this.startingPosition = startingPosition;

        this.worldUUID = world.getUID();

        // Add players to UUIDs
        playerUUIDs = new HashSet<>();
        for (Player player : players) {
            addPlayer(player);
        }

        // Set task
        cutsceneTask = new CutsceneRunTask(this);
    }

    public void addPlayer (Player player) {

        playerUUIDs.add(player.getUniqueId());
        // If cutscene is active make them spectate the entity
        if (active) {
            Entity cameraEntity = getSpectateEntity();
            if (cameraEntity != null) {
                setPlayerSpectate(player, cameraEntity);
            }
        }

    }

    public void startCutscene () {

        World world = getWorld();

        if (world == null) {
            return;
        }

        active = true;

        // Summon armor stand
        ArmorStand entity =  (ArmorStand) world.spawnEntity(startingPosition, EntityType.ARMOR_STAND,
                CreatureSpawnEvent.SpawnReason.COMMAND, armor_stand -> {
            armor_stand.setGravity(false);
            armor_stand.setInvulnerable(true);
            armor_stand.setCustomNameVisible(true);
        });
        entity.setInvisible(true);

        spectateEntityUUID = entity.getUniqueId();

        // Get all players to spectate the armor stand
        for (UUID playerUUID : playerUUIDs) {
            Player client = CBCPlugin.getPlugin().getServer().getPlayer(playerUUID);
            if (client == null) continue;
            if (!client.isOnline()) continue;
            setPlayerSpectate(client, entity);
        }

        // Run the 0th frame of the cutscene
        tick(true);

        // Start running cutscene
        cutsceneTask.runTaskTimer(CBCPlugin.getPlugin(), 1, 1);
    }

    public void endCutscene () {

        if (!active) return;

        // Turn cutscene inactive
        active = false;

        // Cancel cutscene task
        if (cutsceneTask != null) {
            if (!cutsceneTask.isCancelled()) {
                cutsceneTask.cancel();
            }
        }

        // Set all players camera back to themselves
        for (UUID playerUUID : playerUUIDs) {
            Player client = CBCPlugin.getPlugin().getServer().getPlayer(playerUUID);
            if (client == null) continue;
            if (!client.isOnline()) continue;
            setPlayerSpectate(client, client);
        }

        // Remove spectate entity
        Entity spectateEntity = getSpectateEntity();
        if (spectateEntity != null) {
            spectateEntity.remove();
        }
    }

    public void setAllPlayerSpectate() {
        // Get all players to spectate the armor stand
        Entity entity = getWorld().getEntity(spectateEntityUUID);

        if (entity == null) return;

        for (UUID playerUUID : playerUUIDs) {
            Player client = CBCPlugin.getPlugin().getServer().getPlayer(playerUUID);
            if (client == null) continue;
            if (!client.isOnline()) continue;
            setPlayerSpectate(client, entity);
        }
    }

    public void setPlayerSpectate(Player client, Entity spectateEntity) {

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        PacketContainer spectatePacket = protocolManager.createPacket(PacketType.Play.Server.CAMERA);
        spectatePacket.getIntegers().write(0, spectateEntity.getEntityId());

        protocolManager.sendServerPacket(client, spectatePacket);

    }

    public void incrementTick () {

        if (tickNum % 5 == 0) {
            setAllPlayerSpectate();
        }

        tickNum++;
        if (tickNum > length) {
            endCutscene();
        }
    }

    public abstract void tick(boolean init);

    public World getWorld() {
        return Bukkit.getWorld(worldUUID);
    }

    public Entity getSpectateEntity () {
        return getWorld().getEntity(spectateEntityUUID);
    }

    public int getTickNum () {
        return tickNum;
    }

    public int getLength () {return length;}

    public boolean isActive () {
        return active;
    }
}
