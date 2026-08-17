package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
/*
public class JumpPad {

    private static final Random rd = new Random();
    private final Location location;

    public JumpPad (Location location) {
        this.location = location;
    }

    public JumpPad(GameManager gameManager, CombatManager combatManager, Vector coordinates) {
        this(new Location(gameManager.getWorld(), coordinates.getX(), coordinates.getY(), coordinates.getZ()));
    }

    public void playParticles () {
        for (int i = 0; i < 2; i++) {
            Random random = new Random();
            location.getWorld().spawnParticle(Particle.SNEEZE, location.clone().add(
                    random.nextDouble() * 2 - 1,
                    0.5,
                    random.nextDouble() * 2 - 1
                ),
                0, 0d, 0.5d, 0d, 0.5
            );
        }
    }

    public Collection<CBCPlayer> getPlayersOnPad (PlayerRegistry registry) {
        return location.getNearbyEntitiesByType(Player.class, 5).stream()
                .filter(registry::hasPlayer)
                .map(registry::getPlayer)
                .filter(CBCPlayer::isAlive)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Location location () {
        return location;
    }

}
*/