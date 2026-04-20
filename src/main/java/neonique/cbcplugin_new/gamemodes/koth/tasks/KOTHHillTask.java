package neonique.cbcplugin_new.gamemodes.koth.tasks;

import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import neonique.cbcplugin_new.gamemodes.koth.KOTHHill;
import neonique.cbcplugin_new.gamemodes.koth.KOTHPlayer;
import neonique.cbcplugin_new.gamemodes.koth.KOTHTeam;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class KOTHHillTask extends BukkitRunnable {

    private final KOTHGame game;
    private final KOTHHill hill;

    private final float detectionPeriod;

    public KOTHHillTask (KOTHGame game, KOTHHill hill, float detectionFrequency) {
        this.game = game;
        this.hill = hill;

        detectionPeriod = (float) 1 / detectionFrequency;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // If hill is disabled do not run this task
        if (game.isHillEnabled()) {

            // Find amount of players in each team in hill
            int peopleInHill = 0;
            HashMap<KOTHTeam, Set<KOTHPlayer>> teamPlayersInHill = new HashMap<>();
            for (KOTHTeam team : game.getTeams()) {
                Set<KOTHPlayer> playersInHill = new HashSet<>();
                for (KOTHPlayer player : team.getPlayers()) {
                    if (this.hill.isPlayerInHill(player)) {
                        playersInHill.add(player);
                        peopleInHill++;
                        player.setInHill(true);

                        // Add time in point
                        player.addTimeInHill(detectionPeriod);
                    }
                    else {
                        player.setInHill(false);
                    }
                }
                teamPlayersInHill.put(team, playersInHill);
            }

            // Check if a team currently holds the hill
            KOTHTeam pointControlTeam = game.getPointControlTeam();
            if (pointControlTeam != null) {

                // Increment time in hill for team
                pointControlTeam.increaseHeldTime(detectionPeriod);

                // Check if any players are in the hill
                if (peopleInHill > 0) {
                    // Check if any player on the point controlling team is still in the hill
                    Set<KOTHPlayer> playersInHill = teamPlayersInHill.get(pointControlTeam);
                    if (playersInHill.isEmpty()) {
                        // Reduce capture status of hill
                        game.uncapturingPoint();
                    } else if (playersInHill.size() == peopleInHill) {
                        game.capturingByTeam(pointControlTeam);
                    }
                }
            } else {
                if (peopleInHill > 0) {
                    // Check if any team has majority in the hill
                    KOTHTeam majorityTeam = null;
                    int majorityTeamPlayers = 0;

                    for (KOTHTeam team : teamPlayersInHill.keySet()) {
                        Set<KOTHPlayer> playersInHill = teamPlayersInHill.get(team);
                        if ((float) playersInHill.size() > ((float) peopleInHill * game.getCapturingPlayerPercentage())) {
                            if (majorityTeam == null) {
                                majorityTeam = team;
                                majorityTeamPlayers = playersInHill.size();
                            } else if (majorityTeamPlayers == playersInHill.size()) {
                                // If two teams are tied in players, do not capture
                                majorityTeam = null;
                                break;
                            } else if (majorityTeamPlayers < playersInHill.size()) {
                                majorityTeam = team;
                                majorityTeamPlayers = playersInHill.size();
                            }
                        }
                    }

                    // If no majority team exists, uncapture point
                    if (majorityTeam == null) {
                        game.uncapturingPoint();
                    }
                    // If a majority team exists, capture the point
                    else {
                        if (game.getPointCaptureTeam() == majorityTeam || game.getPointCaptureProgress() == 0) {
                            game.capturingByTeam(majorityTeam);
                        } else {
                            game.uncapturingPoint();
                        }
                    }
                } else {
                    game.uncapturingPoint();
                }
            }
        }

        game.updateBossbarManager();

    }
}
