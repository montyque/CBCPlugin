package neonique.cbcplugin_new.misc;

import neonique.cbcplugin_new.enums.EasingType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class StraightLineCutscene extends Cutscene {

    private final Location startingPosition;
    private final Location endingPosition;

    // Pause at start and pause at end of cutscene
    private final int startPause;
    private final int endPause;

    private final EasingType easingType;

    public StraightLineCutscene(World world, Location startingPosition, Location endingPosition, int length,
                         Collection<Player> players, EasingType easingType, int startPause, int endPause) {
        super(world, startingPosition, length, players);

        this.startingPosition = startingPosition;
        this.endingPosition = endingPosition;

        this.easingType = easingType;

        this.startPause = startPause;
        this.endPause = endPause;
    }

    @Override
    public void tick(boolean init) {

        // Retrieve camera entity
        Entity entity = getSpectateEntity();

        // If entity no longer exists for some reason
        if (entity == null) {
            endCutscene();
            return;
        }

        if (getTickNum() >= startPause && getLength() - getTickNum() > endPause) {
            // Set location of camera
            setCameraLocation();
        }

        if (!init) {
            incrementTick();
        }
    }

    public void setCameraLocation () {

        double travelTimeLength = getLength() - startPause - endPause;
        double progressLinear = (getTickNum() - startPause) / travelTimeLength;

        double progress = easingType.getProgress(progressLinear);

        // Difference between starting position and ending position
        Vector difference = endingPosition.toVector().subtract(startingPosition.toVector());
        difference.multiply(progress);

        Location newLocation = startingPosition.clone().add(difference);

        // Retrieve camera entity
        Entity entity = getSpectateEntity();
        entity.teleport(newLocation);
    }

}
