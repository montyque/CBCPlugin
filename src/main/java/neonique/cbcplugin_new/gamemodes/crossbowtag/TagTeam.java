package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class TagTeam extends CBCTeam<TagPlayer> {

    private final TagGame game;

    // Team start spawns
    private Set<Location> evaderSpawns;
    private Set<Location> taggerSpawns;

    // Scores
    private float score = 0;

    // Placement
    int placement = 1;
    boolean tied = true;

    // Statistics
    private float evaderPoints = 0;
    private float taggerPoints = 0;

    public TagTeam (TagGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                          String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);

        this.game = game;

    }

    public void setupRound () {

        // Teleport players
        List<Location> teamSpawnList;

        // Change the team's spawns depending on if they are the tagger or not
        if (isTeamTaggers()) {
            teamSpawnList = new ArrayList<>(taggerSpawns);
        } else {
            teamSpawnList = new ArrayList<>(evaderSpawns);
        }

        Collections.shuffle(teamSpawnList);

        int playerinc = 0; // Increments every time we teleport a player
        for (TagPlayer player : getPlayers()) {

            player.setEliminated(false);

            if (!player.isOnline()) continue;

            player.playerSetupRound();
            // Spawns players in different spawnpoints - reason playerinc is used
            player.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()), game.getMap().getMapCentre());
            playerinc++;
        }
    }

    public boolean isTeamTaggers () {
        return game.getTaggers() == this;
    }

    public void setEvaderSpawns(Set<Location> evaderSpawns) {
        this.evaderSpawns = evaderSpawns;
    }

    public void setTaggerSpawns(Set<Location> taggerSpawns) {
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
        for (TagPlayer player : getPlayers()) {
            player.setEliminated(false);
            if (player.isInGame()) {
                inGamePlayers.add(player);
            }
        }
        return inGamePlayers;
    }

}
