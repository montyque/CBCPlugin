package neonique.cbcplugin_new.gamemodes.showdown;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ShowdownSpawn extends Location {

    // Start of round related options
    private final boolean createBox;

    private final Set<Block> Box = new HashSet<>();

    public ShowdownSpawn(World world, Vector vector, boolean createBox) {
        super(world, vector.getX(), vector.getY(), vector.getZ());

        this.createBox = createBox;

        // If create box is set to true, get the blocks around the box
        if (createBox) {
            // Iterate through each block
            for (double boxX = -2; boxX < 3; boxX++) {
                for (double boxY = -1; boxY < 4; boxY++) {
                    for (double boxZ = -2; boxZ < 3; boxZ++) {
                        // The floors of the box
                        if (boxY == -1 || boxY == 3) {
                            Box.add(this.clone().add(boxX, boxY, boxZ).getBlock());
                        } else if (Math.abs(boxX) == 2 || Math.abs(boxZ) == 2) {
                            Box.add(this.clone().add(boxX, boxY, boxZ).getBlock());
                        }
                    }
                }
            }
        }
    }

    public void setupSpawn() {
        if (createBox) {
            // Create box with barrier blocks
            for (Block block : Box) {
                block.setType(Material.BARRIER);
            }
        }
    }

    public void roundStart() {
        if (createBox) {
            // Remove box
            for (Block block : Box) {
                block.setType(Material.AIR);
            }
        }

    }
}
