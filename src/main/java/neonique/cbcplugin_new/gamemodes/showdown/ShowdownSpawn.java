package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.gamemodes._base.BoxSpawn;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ShowdownSpawn extends BoxSpawn {

    private final boolean createBox; // If a barrier box should be created upon spawning

    public ShowdownSpawn(World world, Vector vector, boolean createBox) {
        super(world, vector);
        this.createBox = createBox;
    }

    public void createBarrierBox () {
        if (createBox) createBox();
    }

    public void setupSpawn() {
        createBarrierBox();
    }

    public void roundStart() {
        if (createBox) removeBox();
    }

}
