package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamLike;
import org.bukkit.Location;

import java.util.*;

public class TagTeam extends CBCTeam<TagPlayer> {

    private final TagGame game;

    // Team start spawns
    private Collection<Location> evaderSpawns;
    private Collection<Location> taggerSpawns;

    // Scores
    private float score = 0;

    // Placement
    int placement = 1;
    boolean tied = true;

    // Statistics
    private float evaderPoints = 0;
    private float taggerPoints = 0;

    public TagTeam (TagGame game, TeamLike originalTeam, String teamIdNum) {
        super(originalTeam, teamIdNum);
        this.game = game;
    }

    public void setupRound (List<Location> spawns) {

        List<Location> teamSpawnList = new ArrayList<>(spawns);
        Collections.shuffle(teamSpawnList);

        int playerinc = 0; // Increments every time we teleport a player
        for (TagPlayer player : players()) {

            player.setEliminated(false);

            if (!player.isOnline()) continue;

            player.playerSetupRound();
            player.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()), game.getMap().getMapCentre());
            playerinc++;

        }
    }

    public boolean isTeamTaggers () {
        return game.getTaggers() == this;
    }

    public void setEvaderSpawns(Collection<Location> evaderSpawns) {
        this.evaderSpawns = evaderSpawns;
    }

    public void setTaggerSpawns(Collection<Location> taggerSpawns) {
        this.taggerSpawns = taggerSpawns;
    }

    public Location getRandomTaggerSpawn () {
        List<Location> randomTaggerSpawns = new ArrayList<>(taggerSpawns);
        Collections.shuffle(randomTaggerSpawns);
        return randomTaggerSpawns.get(0);
    }

    public void evaderKill() {

        // Add points to team
        score += game.getCurrentEvaderKillValue();
        taggerPoints += game.getCurrentEvaderKillValue();
        game.updatePlacements();

    }

    public void taggerRoundCompletedPoints (int pointsEarned) {

        // Add points to team
        score += pointsEarned;
        taggerPoints += pointsEarned;
        game.updatePlacements();

    }

    public void playerSurvivalScore (float points) {

        // Add one to score
        score += points;
        evaderPoints += points;

    }

    public int getIntScore() {
        return Math.round(score);
    }

    public int getPlacement () {
        return placement;
    }

    public boolean isTied () {
        return tied;
    }

    public void setPlacement(int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }

    public int getIntTaggerPoints () {
        return Math.round(taggerPoints);
    }

    public int getIntEvaderPoints () {
        return Math.round(evaderPoints);
    }

    public Location getRandomEvaderSpawn() {
        List<Location> randomSpawns = new ArrayList<>(evaderSpawns);
        Collections.shuffle(randomSpawns);
        return randomSpawns.get(0);
    }

    public Set<TagPlayer> getInGamePlayers() {
        Set<TagPlayer> inGamePlayers = new HashSet<>();
        for (TagPlayer player : players()) {
            player.setEliminated(false);
            if (player.isInGame()) {
                inGamePlayers.add(player);
            }
        }
        return inGamePlayers;
    }

}
