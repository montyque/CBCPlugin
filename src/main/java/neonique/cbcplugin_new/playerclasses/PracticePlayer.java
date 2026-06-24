package neonique.cbcplugin_new.playerclasses;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.PracticeManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PracticePlayer extends CBCPlayer {

    PracticeManager practiceManager;

    public PracticePlayer(GameManager gameManager, CombatManager combatManager, PracticeManager practiceManager, Player player) {
        super(gameManager, combatManager, player);
        this.practiceManager = practiceManager;
    }

    @Override
    public void playerSpawn() {

        if (!isOnline()) return;

        setImmune(true);
        new TempImmunityTask(getGameManager(), getCombatManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);

        // Teleport player to spawn point
        teleportPlayer();

        getGameManager().getWorld().spawnParticle(Particle.INSTANT_EFFECT, getPlayer().getLocation(), 80, 0.25, 0.25, 0.25, 1, null, true);
        playerSetup(2);

    }

    public void teleportPlayer() {

        // Find spawnpoint that is the farthest away from other players
        List<FFASpawnpoint> spawns = practiceManager.getSpawns();
        Collections.shuffle(spawns);

        for (FFASpawnpoint spawn : spawns) {
            spawn.findDistanceOfNearestPlayer(50.0, this);
        }

        spawns.sort(Comparator.comparingDouble(FFASpawnpoint::getNearestPlayerRange));
        Collections.reverse(spawns);

        teleportPlayerToSpawn(spawns.get(0), practiceManager.getMap().getMapCentre());

    }
}
