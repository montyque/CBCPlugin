package neonique.cbcplugin_new.gamemodes._base;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.packets.WrapperPlayServerEntityMetadata;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class GlowManager {

    protected World world;

    protected ProtocolManager protocolManager;
    protected PacketAdapter packetListener;
    protected PacketAdapter packetSpawnListener;

    private HashMap<Player, Set<Player>> glowMap;

    boolean active = false;

    public GlowManager (World world) {
        this.world = world;
        protocolManager = ProtocolLibrary.getProtocolManager();

        glowMap = new HashMap<>();
    }

    public void togglePlayer (Player client, Player playerGlowing) {
        if (!glowMap.containsKey(client)) {
            glowMap.put(client, new HashSet<>());
        }

        Set<Player> playerGlowingSet = glowMap.get(client);

        if (playerGlowingSet.contains(playerGlowing)) {
            playerGlowingSet.remove(playerGlowing);
            playerImmediateGlow(client, playerGlowing, false);
        } else {
            playerGlowingSet.add(playerGlowing);
            playerImmediateGlow(client, playerGlowing, true);
        }
    }

    public void playerImmediateGlow(Player client, Player playerGlowing, boolean glowing) {

        WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata();
        WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(playerGlowing);

        // Collect if the entity is already glowing
        if (glowing) {
            byte data = watcher.getByte(0);
            data |= 1 << 6;
            wrapper.addToDataValueCollection(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), data));
            CBCPlugin.getPlugin().getLogger().info("Sending glowing packet to " + client.getName() + " for player " + playerGlowing.getName());
        }
        else {
            wrapper.addToDataValueCollection(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));
        }

        wrapper.setEntityID(playerGlowing.getEntityId());
        wrapper.sendPacket(client);

    }

    public void activate() {

        if (active) {
            return;
        }

        active = true;

        packetListener = new PacketAdapter(CBCPlugin.getPlugin(), PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                run(event);
            }
        };
        protocolManager.addPacketListener(packetListener);

        packetSpawnListener = new PacketAdapter(CBCPlugin.getPlugin(), PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                run(event);
            }
        };
        protocolManager.addPacketListener(packetSpawnListener);
    }

    public void deactivate() {

        if (!active) {
            return;
        }

        active = false;
        protocolManager.removePacketListener(packetListener);
        protocolManager.removePacketListener(packetSpawnListener);

    }

    public abstract boolean isGlowing (Player client, Player player);

    public void run (PacketEvent packetEvent) {

        Player client = packetEvent.getPlayer();
        Entity player = packetEvent.getPacket().getEntityModifier(world).read(0);

        if (player.getType() != EntityType.PLAYER) return;

        if (packetEvent.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {

            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(packetEvent.getPacket().deepClone());

            if (isGlowing(client, (Player) player)) {
                // Already glowing
                if (Objects.requireNonNull(wrapper.getDataValueCollection()).stream()
                        .map(WrappedDataValue::getValue)
                        .filter(Byte.class::isInstance)
                        .map(Byte.class::cast)
                        .anyMatch(b -> b == (byte) 0x40))
                    return;
                wrapper.addToDataValueCollection(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x40));
            }

            packetEvent.setPacket(wrapper.getHandle());
        }
        else if (packetEvent.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            playerImmediateGlow(client, (Player) player, isGlowing(client, (Player) player));
        }
    }
}
