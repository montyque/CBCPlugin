package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class RendezvousPlayer extends CBCPlayer {

    private final RendezvousGame game;

    // Player game stats
    private int checkpointsCleared = 0;
    private int enemyRunnersKilled = 0;
    private int moraleBoostsGiven = 0;

    // Constants for giving players game score
    private static final int KILL_PTS = 5; // Points for killing a player
    private static final int MORALE_BOOST_KILL_PTS = 10; // Extra points for giving a morale boost to a teammate runner
    private static final int RUNNER_KILL_PTS = 10; // Extra points for killing a runner
    private static final int AS_RUNNER_KILL_PTS = 5; // Extra points for killing someone as a runner
    private static final int CHECKPOINT_CAPTURE = 60; // Points for capturing a checkpoint
    private static final int FINAL_CHECKPOINT_CAPTURE = 20; // Extra points for capturing the final checkpoint

    // When selecting a runner spawn, prioritise spawns with no enemy inside this radius
    private static final int RUNNER_SPAWN_ENEMY_RADIUS = 30;
    // When selecting a runner spawn, prioritise spawns with no checkpoint inside this radius
    private static final int RUNNER_SPAWN_CHECKPOINT_RADIUS = 30;

    public RendezvousPlayer(RendezvousGame game, GameManager gameManager, CombatManager combatManager,
                            Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void checkpointCapturingTitle (float progress) {

        if (!isAlive()) return;
        if (getTeam() == null) return;

        String title = "CAPTURING CHECKPOINT";

        int length = title.length();
        int lengthOfColor = (int) (progress * (float) length);

        StringBuilder coloredTitle = new StringBuilder();
        StringBuilder whiteTitle = new StringBuilder();

        for (int i = 0; i < length; i++) {
            if (i < lengthOfColor) {
                coloredTitle.append(title.charAt(i));
            }
            else {
                whiteTitle.append(title.charAt(i));
            }
        }

        Component progressComponent = smallText(coloredTitle.toString()).color(getTeam().getColor())
                .append(smallText(whiteTitle.toString()).color(NamedTextColor.WHITE));

        getPlayer().showTitle(Title.title(blankComponent(), progressComponent,
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200)))
        );

    }

    /**
    Runs when a player fully captures their checkpoint.
     */
    public void checkpointCleared () {

        if (!isOnline()) return;
        RendezvousTeam team = getRendezvousTeam();

        getGameManager().sendGlobalMessage(
                        Component.text("CHECKPOINT > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(getNameComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                        .append(Component.text(" has cleared a checkpoint!").color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
        );
        team.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 300, 1);

        // Update statistics
        checkpointsCleared++;
        addGamePoints(CHECKPOINT_CAPTURE);

        // Show title to player
        NamedTextColor color = team.getColor();
        Title title =  Title.title(
                Component.text("Checkpoint cleared!").color(color).decorate(TextDecoration.BOLD),
                Component.text("+1 Checkpoints").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC),
                TextUtil.titleTimes(0, 500, 250)
        );
        getPlayer().showTitle(title);

        healToFull();
        team.checkpointCleared();

        if (team.getScore() == 0) {
            addGamePoints(FINAL_CHECKPOINT_CAPTURE);
        }

    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {

        int killPts = KILL_PTS;

        // Check if player killed was a runner
        RendezvousPlayer rdvPlayerKilled = (RendezvousPlayer) playerKilled;
        if (rdvPlayerKilled.isPlayerRunner()) {
            enemyRunnersKilled++;
            if (isOnline()) {
                getPlayer().sendMessage(
                        Component.text("+1 Runners Killed").color(NamedTextColor.YELLOW)
                                .decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC)
                );
            }

            // Give points for killing runner
            killPts += RUNNER_KILL_PTS;

        }

        if (isPlayerRunner()) {
            killPts += AS_RUNNER_KILL_PTS;
        }

        // Give morale boost
        if (checkMoraleBoost(playerKilled)) {

            RendezvousPlayer teamRunner = getRendezvousTeam().getRunner();

            teamRunner.addHealing(6);
            if (isOnline()) {
                getPlayer().sendMessage(
                        Component.text("Morale Boost given to ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                                .append(Component.text(teamRunner.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                                .append(Component.text("! (Teammate receives + 3.0 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                );
            }

            teamRunner.getPlayer().sendMessage(
                    Component.text("Morale Boost received from ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                            .append(Component.text(getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                            .append(Component.text("! ( + 3.0 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
            );

            // Give points for morale boost
            killPts += MORALE_BOOST_KILL_PTS;
            moraleBoostsGiven++;

        }

        addGamePoints(killPts);

        if (!isOnline()) return;

        // Update user's client board
        game.updateServerSidebar();

    }

    /**
    Checks if the player meets the requirements to give a Morale Boost to their runner.
     @param playerKilled: The player who was killed.
     */
    public boolean checkMoraleBoost (CBCPlayer playerKilled) {

        if (isPlayerRunner()) return false;

        RendezvousPlayer teamRunner = getRendezvousTeam().getRunner();
        if (teamRunner == null) return false;
        if (!teamRunner.isAlive()) return false;

        RendezvousCheckpoint currentCheckpoint = getRendezvousTeam().getTargetCheckpoint();
        if (currentCheckpoint == null) return false;

        // Check if the runner has been damaged in the last 8 seconds by the player who was killed
        if (teamRunner.damagingPlayersInLastTime(160).contains(playerKilled)) {
            return true;
        }

        if (!playerKilled.isOnline()) return false;
        Location playerKilledLocation = playerKilled.getPlayer().getLocation();
        Location teamRunnerLocation = teamRunner.getPlayer().getLocation();

        // Check if player killed is near the checkpoint
        double maxDistanceSquared = 30 * 30;
        if (currentCheckpoint.distanceSquared(playerKilledLocation) <= maxDistanceSquared) {
            return true;
        }

        // Check if the player credited with the kill is nearby the checkpoint
        if (isAlive()) {
            if (currentCheckpoint.distanceSquared(getPlayer().getLocation()) <= maxDistanceSquared) {
                return true;
            }
        }

        // Check if the runner is 30 blocks away from the player who was killed
        return teamRunnerLocation.distanceSquared(playerKilledLocation) <= maxDistanceSquared;

    }

    public boolean isPlayerRunner () {
        RendezvousTeam team = getRendezvousTeam();
        return (team.getRunner() == this);
    }

    public RendezvousCheckpoint getTeamCheckpoint () {
        RendezvousTeam team = getRendezvousTeam();
        return team.getTargetCheckpoint();
    }

    @Override
    public void playerSpawn () {

        if (!isOnline()) return;

        playerSetup(2);
        setTempImmune(60);

        RendezvousSpawn selectedSpawn = selectSpawn();
        Location lookLocation = game.getMap().getMapCentre();

        if (isPlayerRunner()) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000,
                    0, false, false, false));
            if (getTeamCheckpoint() != null) {
                lookLocation = getTeamCheckpoint();
            }
        }

        teleportPlayerToSpawn(selectedSpawn, lookLocation);

        if (getRendezvousTeam() != null) {
            getRendezvousTeam().setPlayerListFooterForPlayer(getPlayer());
        }

    }

    public RendezvousSpawn selectSpawn () {


        if (isPlayerRunner() && getTeamCheckpoint() != null) {
            return selectSpawnForRunner();
        }
        else {
            List<RendezvousSpawn> spawns = new ArrayList<>(game.getSpawns());
            Collections.shuffle(spawns);

            Set<RendezvousCheckpoint> inPlayCheckpoints = game.getInPlayCheckpoints();

            for (RendezvousSpawn spawn : spawns) {

                boolean nearbyCheckpoint = false;
                for (RendezvousCheckpoint checkpoint : inPlayCheckpoints) {
                    if (spawn.distanceSquared(checkpoint) < 30 * 30) {
                        nearbyCheckpoint = true;
                        break;
                    }
                }

                if (nearbyCheckpoint) continue;
                boolean nearbyRunner = false;
                for (RendezvousPlayer runner : game.getCurrentAliveRunners()) {
                    if (spawn.distanceSquared(runner.getPlayer().getLocation()) < 30 * 30) {
                        nearbyRunner = true;
                        break;
                    }
                }

                if (nearbyRunner) continue;
                return spawn;
            }
            return spawns.get(0);
        }
    }

    public RendezvousSpawn selectSpawnForRunner () {

        RendezvousCheckpoint checkpoint = getTeamCheckpoint();
        double targetDistance = getRendezvousTeam().getCurrentCheckpointTargetDistance();

        List<RendezvousSpawn> spawns = new ArrayList<>(game.getSpawns());
        Collections.shuffle(spawns);

        RendezvousSpawn closestSpawn = null;
        double smallestDif = Double.POSITIVE_INFINITY;

        Set<RendezvousCheckpoint> inPlayCheckpoints = game.getInPlayCheckpoints();

        for (RendezvousSpawn spawn : spawns) {

            double distance = checkpoint.distance(spawn);
            double difference = Math.abs(targetDistance - distance);

            if (difference < smallestDif) {
                closestSpawn = spawn;
                smallestDif = difference;
            }

            // Check the error in distance is less than 20
            if (difference > 20) continue;

            // Check if there is an enemy nearby the spawn
            if (spawn.isEnemyNearby(this, RUNNER_SPAWN_ENEMY_RADIUS)) continue;

            // Check if there is an enemy checkpoint nearby the spawn
            boolean nearbyCheckpoint = false;
            for (RendezvousCheckpoint inPlayCheckpoint : inPlayCheckpoints) {
                if (spawn.distanceSquared(inPlayCheckpoint) < RUNNER_SPAWN_CHECKPOINT_RADIUS * RUNNER_SPAWN_CHECKPOINT_RADIUS) {
                    nearbyCheckpoint = true;
                    break;
                }
            }

            if (nearbyCheckpoint) continue;

            // If all prior conditions are satisfied, select this as the spawn
            return spawn;

        }

        if (closestSpawn == null) {
            return spawns.get(0);
        } else {
            return closestSpawn;
        }

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        clearPlayerListSuffixes();

        // The player will respawn, so we are overriding the old method
        if (isOnline() && getTeam() != null) {

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            if (!getRendezvousTeam().isOutOfGame()) {
                // Make sure team is still able to respawn
                setRespawning(true);

                // Find the amount of time that it takes for the players to respawn
                int timeToRespawn = 4;
                // Set up respawn timer
                RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getCombatManager(), this, timeToRespawn + 1);
                respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);
            }
        }

        if (getRendezvousTeam() != null) {
            if (isPlayerRunner()) {
                getRendezvousTeam().incrementRunnerDeaths();
            }
        }

        // Update bossbar
        game.updateBossbarManager();

    }

    public void finalCheckpointTeleport () {

        if (!isOnline()) return;

        RendezvousSpawn spawn = selectSpawnForRunner();
        teleportPlayerToSpawn(spawn, getTeamCheckpoint());

        getPlayer().sendMessage(Component.text("You have been teleported away from your checkpoint.").color(NamedTextColor.YELLOW));

    }

    public RendezvousTeam getRendezvousTeam () {
        try {
            return (RendezvousTeam) getTeam();
        } catch (ClassCastException e) {
            return null;
        }
    }

    public int getCheckpointsCleared() {
        return checkpointsCleared;
    }

    public int getEnemyRunnersKilled() {
        return enemyRunnersKilled;
    }

    @Override
    public void addGamePoints (int points) {
        super.addGamePoints(points);
        game.getSidebarManager().updateServerBoard();
    }

    public int getMoraleBoostsGiven() {
        return moraleBoostsGiven;
    }
}
