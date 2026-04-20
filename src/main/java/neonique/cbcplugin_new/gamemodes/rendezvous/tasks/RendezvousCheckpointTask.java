package neonique.cbcplugin_new.gamemodes.rendezvous.tasks;

import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousCheckpoint;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousGame;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousPlayer;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousTeam;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class RendezvousCheckpointTask extends BukkitRunnable {

    private final RendezvousGame game;

    public RendezvousCheckpointTask (RendezvousGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        if (game.getWinner() != null) {
            return;
        }

        Set<RendezvousCheckpoint> checkpointsParticles = new HashSet<>();

        // Go through each team and check if team's checkpoint
        for (RendezvousTeam team : game.getTeams()) {

            // Check if team has checkpoint
            if (team.getTargetCheckpoint() == null) {
                team.setRunnerInCheckpoint(false);
                team.updateCheckpointStatus();
                continue;
            }
            RendezvousCheckpoint targetCheckpoint = team.getTargetCheckpoint();

            // Play particles for checkpoint
            if (!checkpointsParticles.contains(targetCheckpoint)) {
                targetCheckpoint.playParticles(team.getColor(), true);
                checkpointsParticles.add(targetCheckpoint);
            }
            else {
                targetCheckpoint.playParticles(team.getColor(), false);
            }

            // Check if team has a runner
            if (team.getRunner() == null) continue;

            // Check if player's runner is within checkpoint
            if (!team.getRunner().isAlive()) {
                team.setRunnerInCheckpoint(false);
                team.updateCheckpointStatus();
                continue;
            }

            RendezvousPlayer teamRunner = team.getRunner();
            Player teamRunnerEntity = teamRunner.getPlayer();

            team.setRunnerInCheckpoint(targetCheckpoint.checkIfPlayerInCheckpoint(teamRunnerEntity));

            team.updateCheckpointStatus();

        }
    }
}
