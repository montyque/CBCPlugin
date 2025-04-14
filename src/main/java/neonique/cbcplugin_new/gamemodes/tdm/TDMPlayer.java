package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TDMPlayer extends CBCPlayer {

    private final TDMGame game;
    private int withinTeamPlacement = 1;
    private boolean tied = false;

    // Constants for game points
    private static int KILL_PTS = 20; // Points you gain for kills
    private static int CLUTCHKILL_PTS = 15; // Extra points you gain for getting a kill in the clutch

    public TDMPlayer(TDMGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void teleportPlayerToSpawn(Location spawn) {

        getPlayer().teleport(spawn);
        Vector dir = game.getMap().getMapCentre().clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = getPlayer().getLocation().setDirection(dir);
        getPlayer().teleport(loc);

    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {

        // Add points up for this kill
        int killPts = KILL_PTS;

        TDMTeam team = (TDMTeam) this.getTeam();

        // Check if team is in clutch
        if (game.getTimer() < 120) {
            if (team.isTeamInClutch()) {
                killPts += CLUTCHKILL_PTS;
            }
        }

        // Add game points
        addGamePoints(killPts);

        team.onPlayerKill();

        // Update game score and kill leaderboards
        game.updateTopGameScoreList();
        game.updateTopKillsList();

        game.updateServerSidebar();

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // The player will respawn, so we are overriding the old method
        if (isOnline() && getTeam() != null) {
            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            // Respawn player
            setRespawning(true);

            // Find the amount of time that it takes for the players to respawn
            int timeToRespawn = 4;
            // Set up respawn timer
            RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getCombatManager(), this, timeToRespawn + 1);
            respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);
        }
    }

    public void playerRefresh () {

        playerSetup();
        setReloadsBySecond(2);
        setTempImmune(60);

        // Only add glowing if glowing is enabled
        if (game.isPlayerGlowingEnabled()) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));
        }
    }

    @Override
    public void playerSpawn () {

        if (!isOnline()) return;

        if (game.getWinner() == null) {
            new TempImmunityTask(getGameManager(), getCombatManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        }
        else {
            setImmune(true);
        }

        // Teleport player to spawn point
        if (game.isRandomSpawns()) {
            teleportPlayerToSpawn(selectSpawn());
        } else {
            teleportPlayerToSpawn(((TDMTeam) getTeam()).getPlayerSpawn());
        }

        playerRefresh();
    }

    public TDMSpawn selectSpawn () {

        List<TDMSpawn> spawns = new ArrayList<>(game.getRandomSpawns());

        List<TDMSpawn> teamNearbySpawns = new ArrayList<>();
        List<TDMSpawn> noEnemyNearbySpawns = new ArrayList<>();

        for (TDMSpawn spawn : spawns) {
            spawn.setNearestEnemyDistanceMinusTarget(this);
            if (!spawn.isEnemyNearbySpawn(this)) {
                noEnemyNearbySpawns.add(spawn);
                if (spawn.isOutOfCombatAllyNearSpawn(this)) {
                    teamNearbySpawns.add(spawn);
                }
            }
        }

        if (!teamNearbySpawns.isEmpty()) {
            teamNearbySpawns.sort(Comparator.comparingDouble(TDMSpawn::getNearestEnemyDistanceMinusTarget));
            return teamNearbySpawns.get(0);
        }
        else if (!noEnemyNearbySpawns.isEmpty()) {
            noEnemyNearbySpawns.sort(Comparator.comparingDouble(TDMSpawn::getNearestEnemyDistanceMinusTarget));
            return noEnemyNearbySpawns.get(0);
        } else {
            spawns.sort(Comparator.comparingDouble(TDMSpawn::getNearestEnemyDistanceMinusTarget));
            return spawns.get(0);
        }
    }

    public void setWithinTeamPlacement (int placement, boolean tied) {
        this.withinTeamPlacement = placement;
        this.tied = tied;
    }

    public int getWithinTeamPlacement () {
        return withinTeamPlacement;
    }

    @Override
    public void addGamePoints (int points) {
        super.addGamePoints(points);
        if (game.getSidebarManager().isShowGamePoints()) {
            if (isOnline()) {
                game.getSidebarManager().updateClientBoard(getPlayer());
            }
        }
    }
}
