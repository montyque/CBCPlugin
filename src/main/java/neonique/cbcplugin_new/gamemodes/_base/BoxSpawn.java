package neonique.cbcplugin_new.gamemodes._base;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class BoxSpawn extends Location {

    public BoxSpawn (World world, Vector vector) {

        super(world, vector.getX(), vector.getY(), vector.getZ());

    }

    public void setBox (Material block) {
        // Iterate through each block
        for (double boxX = -2; boxX < 3; boxX++) {
            for (double boxY = -1; boxY < 4; boxY++) {
                for (double boxZ = -2; boxZ < 3; boxZ++) {
                    if (boxY == -1 || boxY == 3) {
                        // Fills in the box
                        this.clone().add(boxX, boxY, boxZ).getBlock().setType(block);
                    } else if (Math.abs(boxX) == 2 || Math.abs(boxZ) == 2) {
                        // Fills in the walls
                        this.clone().add(boxX, boxY, boxZ).getBlock().setType(block);
                    }
                }
            }
        }
    }

    public void createBox () {
        setBox(Material.BARRIER);
    }

    public void removeBox () {
        setBox(Material.AIR);
    }

}
