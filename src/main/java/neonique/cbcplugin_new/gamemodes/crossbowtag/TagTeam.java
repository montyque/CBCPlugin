package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TagTeam extends CBCTeam {

    private final TagGame game;
    private final GameManager gameManager;

    // Team start spawns
    private Set<Location> evaderSpawns;
    private Set<Location> taggerSpawns;

    // Scores
    private float score = 0;
    private float survivalMultiplier = 0;

    // Placement
    int placement = 1;
    boolean tied = true;

    // Statistics
    private int eliminationBonusPoints = 0;
    private float evaderPoints = 0;
    private float taggerPoints = 0;

    public TagTeam (TagGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                          String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);

        this.game = game;
        this.gameManager = game.getGameManager();
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
        for (CBCPlayer player : getPlayers()) {

            TagPlayer tagPlayer = (TagPlayer) player;
            tagPlayer.setEliminated(false);

            if (!player.isOnline()) continue;

            tagPlayer.playerSetupRound();
            // Spawns players in different spawnpoints - reason playerinc is used
            tagPlayer.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));
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
        eliminationBonusPoints += pointsEarned;
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

    public int getEliminationBonusPoints () {
        return eliminationBonusPoints;
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
        for (CBCPlayer player : getPlayers()) {
            // Convert CBCPlayer instance to TagPlayer object
            TagPlayer tagPlayer = (TagPlayer) player;
            tagPlayer.setEliminated(false);
            // Only add him to list if in game
            if (tagPlayer.isInGame()) {
                inGamePlayers.add(tagPlayer);
            }
        }
        return inGamePlayers;
    }
}
