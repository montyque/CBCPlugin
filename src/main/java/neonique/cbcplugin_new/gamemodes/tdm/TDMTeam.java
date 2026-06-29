package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamLike;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class TDMTeam extends CBCTeam<TDMPlayer> {

    // Set variables relating to game
    private final TDMGame game;
    private int kills = 0;

    // Spawns
    private List<Location> spawns;

    // Placement
    int placement = 1;
    boolean tied = true;

    public TDMTeam (TDMGame game, TeamLike originalTeam, String teamIdNum) {
        super(originalTeam, teamIdNum);
        this.game = game;
    }

    public void setSpawns(List<Location> spawns) {
        this.spawns = spawns;
    }

    public void onPlayerKill () {
        kills++;
        if (game.isOvertime()) {
            game.killDuringOvertime(this);
        }
        game.updatePlacements();
        updateWithinTeamPlacements();
        // Update bossbar
        game.updateBossbarManager();
    }

    public int getKills() {
        return kills;
    }

    public List<Location> getSpawns() {
        return spawns;
    }

    public Location getPlayerSpawn() {

        List<Location> validSpawns = new ArrayList<>();
        for (Location spawn : spawns) {
            boolean validSpawn = true;
            for (Player player : spawn.getNearbyEntitiesByType(Player.class, 0.3)) {
                if (game.getPlayer(player) != null) {
                    validSpawn = false;
                    break;
                }
            }
            if (validSpawn) {
                validSpawns.add(spawn);
            }
        }

        if (validSpawns.isEmpty()) {
            validSpawns = new ArrayList<>(spawns);
        }

        return validSpawns.get(new Random().nextInt(validSpawns.size()));
    }

    public List<TDMPlayer> sortPlayersByKills () {
        List<TDMPlayer> playerList = new ArrayList<>(players());
        playerList.sort(Comparator.comparingInt(TDMPlayer::getKills).reversed());
        return playerList;
    }

    public void setPlacement (int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }

    public void updateWithinTeamPlacements () {

        List<TDMPlayer> playersByKills = sortPlayersByKills();

        int placement = 0;
        int currentkills = 100000;
        int i = 0;

        for (TDMPlayer player : playersByKills) {

            boolean tied = false;
            if (player.getKills() < currentkills) {
                placement = i + 1;
                currentkills = player.getKills();
                if (playersByKills.size() - 1 != i) {
                    if (playersByKills.get(i + 1).getKills() == currentkills) {
                        tied = true;
                    }
                }
            }
            else if (currentkills == player.getKills()) {
                tied = true;
            }

            player.setWithinTeamPlacement(placement, tied);

            i++;

        }

    }

    public int getPlacement () {
        return placement;
    }

    public boolean isTied () {
        return tied;
    }

    public boolean isTeamInClutch () {

        // Calculate threshold
        int gameTimer = game.getTimer();
        int clutchThreshold;
        if (game.isOvertime() || gameTimer <= 60) {
            clutchThreshold = 5;
        }
        else if (gameTimer <= 120) {
            clutchThreshold = 7;
        }
        else {
            return false;
        }

        List<TDMTeam> teams = game.getTeamsByKills();

        // Check if team is in first place
        if (placement == 1) {
            // Find team in second
            if (teams.size() > 1) {
                // Second place team points
                int secondTeamScore = teams.get(1).getKills();
                // Check if team is within clutch threshold
                return Math.abs(kills - secondTeamScore) <= clutchThreshold;
            }
            else {
                // There is only one team in this game (somehow?)
                return false;
            }
        }
        else {
            // Team is not in first place, check if leading team is within clutch threshold
            int firstKills = teams.get(0).getKills();
            return Math.abs(kills - firstKills) <= clutchThreshold;
        }
    }
}
