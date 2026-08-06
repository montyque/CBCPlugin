package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapmechanics.MapMechanicSpec;
import neonique.cbcplugin_new.util.VectorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CBCMap {

    private final World world;
    private final CBCMapData mapData;

    public CBCMap (World world, CBCMapData data) {
        this.world = world;
        this.mapData = data;
    }

    public Location getMapCentre () {
        return VectorUtil.vecToLocation(mapData.centerCoords(), world);
    }

    public List<Location> defaultSpawns () {
        return mapData.defaultSpawnCoords().stream()
                .map(v -> VectorUtil.vecToLocation(v, world))
                .toList();
    }

    public Map<TeamColor, List<Location>> defaultTeamSpawns () {
        return mapData.defaultTeamSpawns().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(v -> VectorUtil.vecToLocation(v, world))
                                .toList()
                ));
    }

    public void loadMapChunks (boolean sendMessage) {

        int startChunkX = mapData.upperBound().getBlockX() >> 4;
        int startChunkZ = mapData.lowerBound().getBlockZ() >> 4;
        int endChunkX = mapData.upperBound().getBlockX() >> 4;
        int endChunkZ = mapData.lowerBound().getBlockZ() >> 4;

        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load();
            }
        }

        if (sendMessage) {
            world.sendMessage(Component.text("Chunks for map " + name() + " successfully loaded!").color(NamedTextColor.YELLOW));
        }

    }

    public String name () {
        return mapData.name();
    }

    public String id () {
        return mapData.id();
    }

    public Material blockSymbol () {
        return mapData.blockSymbol();
    }

    public List<MapMechanicSpec> mechanicSpecs () {
        return mapData.mechanicSpecs();
    }

    public MapOptions mapOptions () {
        return mapData.options();
    }



}
