package neonique.cbcplugin_new.playerclasses;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.PracticeManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.tasks.TempImmunityTask;
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

        PlayerRegistry registry = getGameManager().getPlayerRegistry();
        spawns.sort(Comparator.<FFASpawnpoint>comparingDouble(s -> s.findDistanceOfNearestPlayer(registry, 50.0, this)).reversed());
        teleportPlayerToSpawn(spawns.get(0).location(), practiceManager.getMap().getMapCentre());

    }
}
