package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BoxSpawn implements MapStartSpawn {

    private final Location location;
    private final int boxSizeX;
    private final int boxSizeY;
    private final int boxSizeZ;

    private final Map<Location, BlockData> oldBlocks;

    public BoxSpawn (World world, Vector vector, int boxSizeX, int boxSizeY, int boxSizeZ) {
        this.location = VectorUtil.vecToLocation(vector, world);
        this.boxSizeX = boxSizeX;
        this.boxSizeY = boxSizeY;
        this.boxSizeZ = boxSizeZ;
        oldBlocks = new HashMap<>();
    }

    public Location location () {
        return location.clone();
    }

    @Override
    public void onSetup () {
        setBox(Material.BARRIER);
    }

    @Override
    public void reset () {
        resetBox();
    }

    @Override
    public boolean canMove () {
        return true;
    }

    private void applyToBox (Consumer<Location> blockFunction) {
        int xStart = -((boxSizeX - 1) / 2);
        int yStart = -1;
        int zStart = -((boxSizeZ - 1) / 2);

        int xEnd = xStart + boxSizeX - 1;
        int yEnd = yStart + boxSizeY - 1;
        int zEnd = zStart + boxSizeZ - 1;

        for (int x = xStart; x <= xEnd; x++) {
            for (int y = yStart; y <= yEnd; y++) {
                for (int z = zStart; z <= zEnd; z++) {
                    if (x == xStart || x == xEnd || z == zStart || z == zEnd || y == yStart || y == yEnd) {
                        blockFunction.accept(location().add(x, y, z));
                    }
                }
            }
        }
    }

    private void setBlock (Location location, Material mat) {
        Block block = location.getBlock();
        oldBlocks.put(block.getLocation(), block.getBlockData());
        block.setType(mat);
    }

    private void setBox (Material mat) {
        applyToBox((l) -> setBlock(l, mat));
    }

    private void resetBlock (Location location) {
        Block block = location.getBlock();
        if (oldBlocks.containsKey(block.getLocation())) {
            block.setBlockData(oldBlocks.get(block.getLocation()));
        } else {
            block.setType(Material.AIR);
        }
    }

    private void resetBox () {
        applyToBox(this::resetBlock);
    }

}
