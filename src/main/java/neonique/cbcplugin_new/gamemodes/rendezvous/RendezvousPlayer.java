package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
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
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class RendezvousPlayer extends CBCPlayer {

    private final RendezvousGame game;

    // Player game stats
    private int checkpointsCleared = 0;
    private int enemyRunnersKilled = 0;

    // Constants for game points
    private static int KILL_PTS = 1; // Points you gain for kills
    private static int MBOOST_KILL_PTS = 30; // Extra points for giving a morale boost to a teammate runner
    private static int RUNNER_KILL_PTS = 30; // Extra points for killing a runner
    private static int AS_RUNNER_KILL_PTS = 15; // Extra points for killing a runner
    private static int CHECKPOINT_CAPTURE = 70; // Points for capturing a checkpoint
    private static int FINAL_CHECKPOINT_CAPTURE = 50; // Extra points for capturing the final checkpoint

    public RendezvousPlayer(RendezvousGame game, GameManager gameManager, CombatManager combatManager,
                            Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void checkpointCapturingTitle (float progress) {

        if (!isAlive()) return;
        if (getTeam() == null) return;

        final String title = "CAPTURING CHECKPOINT";

        final int length = title.length();
        final int lengthOfColor = (int) (progress * (float) length);

        final StringBuilder coloredTitle = new StringBuilder();
        final StringBuilder whiteTitle = new StringBuilder();

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

        getPlayer().showTitle(Title.title(blankComponent(), progressComponent, Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(200))));

    }

    public void checkpointCleared () {

        if (!isOnline()) return;

        RendezvousTeam team = getRendezvousTeam();

        // Send message scored
        getGameManager().sendGlobalMessage(
                        Component.text("CHECKPOINT > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(getNameComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                        .append(Component.text(" has cleared a checkpoint!").color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
        );

        // Add to checkpoints cleared
        checkpointsCleared++;

        // Add to points
        addGamePoints(CHECKPOINT_CAPTURE);

        // Update leaderboard
        game.updateTopCheckpointsList();

        // Show title to player
        NamedTextColor color = team.getColor();
        Title title =  Title.title(
                Component.text("Checkpoint cleared!").color(color).decorate(TextDecoration.BOLD),
                Component.text("+1 Checkpoints").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500))
        );
        getPlayer().showTitle(title);

        // Heal player back to full health
        getPlayer().setHealth(20);

        // Play sound to all team members
        for (CBCPlayer player : team.getPlayers()) {
            if (player.isOnline()) {
                Player playerEntity = player.getPlayer();
                playerEntity.playSound(playerEntity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 300, 1);
            }
        }

        // Decrement score
        team.checkpointCleared(this);

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

            // Update leaderboard
            game.updateTopRunnerKillsList();
        }

        // Check if player killed was morale boost
        if (!isPlayerRunner()) {
            RendezvousPlayer teamRunner = getRendezvousTeam().getRunner();
            RendezvousCheckpoint currentCheckpoint = getRendezvousTeam().getTargetCheckpoint();
            if (teamRunner != null) {
                // Check if runner is alive
                if (teamRunner.isAlive()) {
                    boolean grantMoraleBoost = teamRunner.damagingPlayersInLastTime(120).contains(playerKilled);

                    // Check if player has been damaged recently
                    if (playerKilled.isOnline() && !grantMoraleBoost) {
                        Location playerKilledLoc = playerKilled.getPlayer().getLocation();
                        // Check if player killed is nearby the checkpoint
                        if (currentCheckpoint != null) {
                            if (currentCheckpoint.distanceSquared(playerKilledLoc) <= 15 * 15) {
                                grantMoraleBoost = true;
                            }
                        }
                        if (!grantMoraleBoost) {
                            if (teamRunner.getPlayer().getLocation().distanceSquared(playerKilledLoc) <= 15 * 15) {
                                grantMoraleBoost = true;
                            }
                        }
                    }

                    // Give morale boost
                    if (grantMoraleBoost) {
                        teamRunner.addHealing(4);

                        if (isOnline()) {
                            getPlayer().sendMessage(
                                    Component.text("Morale Boost given to ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                                            .append(Component.text(teamRunner.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                                            .append(Component.text("! (Teammate receives + 2.0 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                            );
                        }
                        teamRunner.getPlayer().sendMessage(
                                Component.text("Morale Boost received from ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                                        .append(Component.text(getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                                        .append(Component.text("! ( + 2.0 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                        );

                        // Give points for morale boost
                        killPts += MBOOST_KILL_PTS;
                    }
                }
            } else {
                killPts += AS_RUNNER_KILL_PTS;
            }
        }

        // Update kill leaderboard
        game.updateTopKillsList();

        // Add points
        addGamePoints(killPts);

        if (!isOnline()) return;

        // Update user's client board
        game.updateServerSidebar();
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

        if (game.getWinner() == null) {
            new TempImmunityTask(getGameManager(), getWeaponManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        }
        else {
            setImmune(true);
        }

        resetPlayer();

        teleportPlayerToSpawn(selectSpawn());

        setAlive(true);
        setRespawning(false);
        loadout();

        if (isPlayerRunner()) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000,
                    0, false, false, false));
        }

        setReloadsBySecond(3);

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
                    if (spawn.distanceSquared(checkpoint) < 25 * 25) {
                        nearbyCheckpoint = true;
                        break;
                    }
                }
                if (!nearbyCheckpoint) {
                    boolean nearbyRunner = false;
                    for (RendezvousPlayer runner : game.getCurrentAliveRunners()) {
                        if (spawn.distanceSquared(runner.getPlayer().getLocation()) < 20 * 20) {
                            nearbyRunner = true;
                            break;
                        }
                    }
                    if (!nearbyRunner) {
                        return spawn;
                    }
                }
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
        double smallestDif = 2000000000;

        Set<RendezvousCheckpoint> inPlayCheckpoints = game.getInPlayCheckpoints();

        for (RendezvousSpawn spawn : spawns) {

            double distance = checkpoint.distance(spawn);
            double difference = Math.abs(targetDistance - distance);

            if (difference < smallestDif) {
                closestSpawn = spawn;
                smallestDif = difference;
            }

            if (!spawn.isEnemyNearby(this, 20)) {
                if (difference < 10) {
                    boolean nearbyCheckpoint = false;
                    for (RendezvousCheckpoint inPlayCheckpoint : inPlayCheckpoints) {
                        if (spawn.distanceSquared(inPlayCheckpoint) < 15 * 15) {
                            nearbyCheckpoint = true;
                            break;
                        }
                    }
                    if (!nearbyCheckpoint) {
                        break;
                    }
                }
            }
        }

        if (closestSpawn == null) {
            return spawns.get(0);
        }
        else {
            return closestSpawn;
        }


    }

    @Override
    public void loadout() {
        super.loadout();

        if (!isOnline()) return;
        PlayerInventory inventory = getPlayer().getInventory();

        // Create compass for runner
        if (isPlayerRunner() && getTeamCheckpoint() != null) {
            if (!inventory.contains(Material.COMPASS)) {
                setOffhandCompass(inventory);
            }
        }
        else {
            if (inventory.contains(Material.COMPASS)) {
                inventory.setItemInOffHand(null);
            }
        }
    }

    public void setOffhandCompass (PlayerInventory inventory) {
        RendezvousTeam team = getRendezvousTeam();
        inventory.setItem(8, team.getRunnerCompassItem());
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
                RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getWeaponManager(), this, timeToRespawn + 1);
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

    public void teleportPlayerToSpawn(Location spawn) {

        getPlayer().teleport(spawn);
        Vector dir = game.getMap().getMapCentre().clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = getPlayer().getLocation().setDirection(dir);
        getPlayer().teleport(loc);

    }

    public void finalCheckpointTeleport () {

        if (!isOnline()) return;

        RendezvousSpawn spawn = selectSpawnForRunner();
        teleportPlayerToSpawn(spawn);

        // Send message to player
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
        game.updateTopGameScoreList();
        if (game.getSidebarManager().isShowGamePoints()) {
            if (isOnline()) {
                game.getSidebarManager().updateClientBoard(getPlayer());
            }
        }
    }
}
