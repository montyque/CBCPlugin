package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.tasks.TempImmunityTask;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HTGPlayer extends CBCPlayer {

    private final HTGGame game;

    // Game fields
    private boolean isHoldingGold = false;
    private Location lastValidPosition = null;

    // Statistics
    private int pointsScored = 0;
    private int timesPickedUp = 0;
    private int goldHoldersKilled = 0;

    // Constants for game points
    private final static int KILL_PTS = 10; // Points you gain for kills
    private final static int GOLD_HOLDER_KILL_PTS = 20; // Extra points you gain for killing the gold holder
    private final static int KILL_WITH_TEAMMATE_GOLD_PTS = 5; // Extra points you gain for killing an enemy while your teammate has the gold
    private final static int KILL_WITH_GOLD_PTS = 5; // Extra points you gain for killing an enemy while you have the gold
    private final static int GOLD_SCORE_PTS = 20; // Points you gain for scoring a point with the gold
    private final static int GOLD_SCORE_WITHIN_7_PTS = 5; // Extra points for scoring a point with the gold while within 7
    private final static int WINNING_GOLD_RUN_PTS = 50; // Points you gain for getting a winning gold run

    public HTGPlayer(HTGGame game, GameManager gameManager, CombatManager combatManager, Player player) {
        super(gameManager, combatManager, player);
        this.game = game;
    }

    public void updateLastValidPosition() {
        lastValidPosition = getPlayer().getLocation();
    }

    public Location getLastValidPosition() {
        return lastValidPosition;
    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {

        int killPts = KILL_PTS;

        // Check if player killed someone while their teammate has the gold
        if (game.getGoldHolder() != null) {
            if (game.getGoldHolder().team() == team()) {
                if (game.getGoldHolder() == this) {
                    killPts += KILL_WITH_GOLD_PTS;
                } else {
                    killPts += KILL_WITH_TEAMMATE_GOLD_PTS;
                }
            }
        }

        addGamePoints(killPts);

        // Update leaderboards
        game.updateServerSidebar();

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        if (isHoldingGold) {
            if (playerKiller != null) {
                game.getTypedPlayer(playerKiller).killedGoldHolder();
            }
            dropGold();
        }

        // The player will respawn, so we are overriding the old method
        if (isOnline() && team() != null) {
            clearEffects();
            if (!((HTGTeam) team()).isOutOfGame()) {
                setRespawnTicks(80);
            }
        }

        game.updateBossbarManager();

    }

    public HTGSpawn selectSpawn () {

        List<HTGSpawn> spawns = new ArrayList<>(game.getSpawns());

        List<HTGSpawn> teamNearbySpawns = new ArrayList<>();
        List<HTGSpawn> noEnemyNearbySpawns = new ArrayList<>();

        for (HTGSpawn spawn : spawns) {
            if (!spawn.isEnemyNearbySpawn(this)) {
                noEnemyNearbySpawns.add(spawn);
                if (spawn.isOutOfCombatAllyNearSpawn(this)) {
                    teamNearbySpawns.add(spawn);
                }
            }
        }

        if (!teamNearbySpawns.isEmpty()) {
            teamNearbySpawns.sort(Comparator.comparingDouble(HTGSpawn::getGoldDistanceMinusGoldRadius));
            return teamNearbySpawns.get(0);
        }
        else if (!noEnemyNearbySpawns.isEmpty()) {
            noEnemyNearbySpawns.sort(Comparator.comparingDouble(HTGSpawn::getGoldDistanceMinusGoldRadius));
            return noEnemyNearbySpawns.get(0);
        } else {
            spawns.sort(Comparator.comparingDouble(HTGSpawn::getGoldDistanceMinusGoldRadius));
            return spawns.get(0);
        }
    }

    @Override
    public void playerSpawn() {

        if (!isOnline()) return;

        if (game.getWinner() == null) {
            new TempImmunityTask(getGameManager(), getCombatManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        }
        else {
            setImmune(true);
        }

        playerSetup(2);
        setTempImmune(60);

        // Teleport player to spawn point
        teleportPlayerToSpawn(selectSpawn(), game.getGoldLocation());

    }

    public void pickupGold() {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        HTGTeam team = (HTGTeam) team();
        this.timesPickedUp++;
        team.incrementTimesPickedUp();

        isHoldingGold = true;

        // Heal player with absorption hearts
        healToFull();
        playerEntity.addPotionEffect(
                new PotionEffect(PotionEffectType.ABSORPTION, 30000000, 0, false, false, false)
        );
        playerEntity.addPotionEffect(
                new PotionEffect(PotionEffectType.GLOWING, 30000000, 0, false, false, false)
        );

        // Give player temporary immunity for 0.75 seconds
        setTempImmune(15);

        // Play sound for player
        playerEntity.playSound(getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 300, 2);

        // Set player helmet to gold block
        getInventory().setHelmetOverride(game.getGoldHead());

        // Show gold holder title
        Component titleComponent = Component.text("You have the gold!").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD);
        Component subtitleComponent = Component.text("Survive to score points!");

        Title title = Title.title(titleComponent, subtitleComponent, TextUtil.titleTimes(100, 500, 100));

        playerEntity.showTitle(title);

        // Update position
        updateLastValidPosition();
    }

    public void dropGold() {
        isHoldingGold = false;
        getInventory().setHelmetOverride(null);
        game.playerDropGold();
    }

    public void killedGoldHolder() {
        goldHoldersKilled++;
        // Add points
        addGamePoints(GOLD_HOLDER_KILL_PTS);
    }

    public void addPointsScored() {

        HTGTeam team = game.getPlayerTeam(this);
        pointsScored++;

        int goldPts = GOLD_SCORE_PTS;
        if (team() != null) {
            if (team.getScore() <= 7) {
                goldPts += GOLD_SCORE_WITHIN_7_PTS;
            }
            if (team.getScore() == 1) {
                goldPts += WINNING_GOLD_RUN_PTS;
            }
        }

        addGamePoints(goldPts);
    }

    public int getGoldScore() {
        return pointsScored;
    }

    public int getGoldHoldersKilled() {
        return goldHoldersKilled;
    }

    public int getTimesGoldPickedUp() {
        return timesPickedUp;
    }

}
