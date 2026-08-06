package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.mapconfig.spawns.MapStartSpawn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;

import java.util.List;

public class ShowdownTeam extends CBCTeam<ShowdownPlayer> {

    // Set variables relating to showdown game
    private int roundsWon;
    private int playersLeftAlive;
    private boolean teamAlive;

    public ShowdownTeam (TeamLike originalTeam, String teamIdNum) {
        super(originalTeam, teamIdNum);
        roundsWon = 0;
        teamAlive = false;
    }

    public int getRoundsWon () {
        return roundsWon;
    }

    public void teleportPlayers (List<MapStartSpawn> spawns, Location lookLocation) {
        int spawnIndex = 0;
        for (ShowdownPlayer player : onlinePlayers()) {
            MapStartSpawn spawn = spawns.get(spawnIndex++ % spawns.size());
            player.teleportPlayerToSpawn(spawn.location(), lookLocation);
        }
    }

    public void setupRound () {
        playersLeftAlive = updatePlayersLeftAlive(false);
        teamAlive = playersLeftAlive != 0;
        for (ShowdownPlayer player : onlinePlayers()) {
            player.playerSetupRound();
        }
    }

    public int updatePlayersLeftAlive (boolean checkAlive) {
        if (checkAlive) {
            playersLeftAlive = alivePlayers().size();
        } else {
            playersLeftAlive = onlinePlayers().size();
        }
        return playersLeftAlive;
    }

    public void teamWonRound () {
        roundsWon++;
    }

    public boolean isTeamAlive () {
        return teamAlive;
    }

    public int getPlayersLeftAlive () {
        return playersLeftAlive;
    }

    public void reviveTeam () {
        teamAlive = true;
    }

    public void eliminateTeam () {
        teamAlive = false;
    }

}
