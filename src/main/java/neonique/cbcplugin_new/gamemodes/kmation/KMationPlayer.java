package neonique.cbcplugin_new.gamemodes.kmation;

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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KMationPlayer extends CBCPlayer {

    private final KMationGame game;

    // Currently eliminated?
    private boolean eliminated = false;

    // Game variables
    private int playerCycleKills = 0;
    private int cyclePlacement = 1;
    private boolean tied = false;
    private boolean inDanger = false;

    // Game stats
    private int cyclesSurvived = 0;

    public KMationPlayer(KMationGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void teleportToSpawn(Location location) {
        getPlayer().teleport(location);

        Vector dir = game.getMap().getMapCentre().clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = getPlayer().getLocation().setDirection(dir);
        getPlayer().teleport(loc);
    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {
        playerCycleKills++;
        game.updatePlayersInLast();
        game.updatePlacements();
        game.getSidebarManager().updateServerBoard();

        if (game.isOvertime() && playerCycleKills >= game.getOvertimeThreshold() && game.getWinner() == null) {
            game.endCycle();
        }
    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // The player will respawn, so we are overriding the old method
        if (isOnline()) {

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            // Check if player is eliminated
            if (!eliminated) {
                // Respawn player
                setRespawning(true);

                // Find the amount of time that it takes for the players to respawn
                int timeToRespawn = 3;
                // Set up respawn timer
                RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getWeaponManager(), this, timeToRespawn + 1);
                respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);
            } else {
                Component titleComponent = Component.text("ELIMINATED").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                Title diedTitle = Title.title(titleComponent, Component.text("Eliminated in Cycle #" + (cyclesSurvived + 1))
                        .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(1000)));
                getPlayer().showTitle(diedTitle);
            }
        }

        game.getBossbarManager().update();
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

        // Set player gamemode
        resetPlayer();
        for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
        }

        // Teleport player to spawn point
        teleportToSpawn(selectSpawn());

        setAlive(true);
        setRespawning(false);
        loadout();

        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));

        setReloadsBySecond(3);
    }

    // Runs when the game is being set up
    public void playerSetupGame () {

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        eliminated = false;
        // Set gamemode of player to adventure and reset their stats
        getPlayer().setLevel(0);
        getPlayer().setExp(0);
        setAlive(false); // Set player's alive state to false
        playerCycleKills = 0;
        playerEntity.setHealth(20);
        playerEntity.setGameMode(GameMode.ADVENTURE);
        playerEntity.getInventory().clear();
        playerEntity.updateInventory();
        // Manage player effects
        playerEntity.removePotionEffect(PotionEffectType.GLOWING);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 800000, 0, false, false, false));
        setReloadsBySecond(3); // Reset player's reloading timers
    }

    // Runs when the game countdown timer stops
    public void playerStartGame () {

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        // Set gamemode of player to adventure and reset their stats
        playerEntity.setHealth(20);
        playerEntity.getInventory().clear();
        playerEntity.updateInventory();
        setReloadsBySecond(3); // Reset player's reloading timers
        setAlive(true); // Set player's state to alive
        setImmune(true); // Make player immune
        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));
        new TempImmunityTask(getGameManager(), getWeaponManager(), this, 6).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);
    }

    // Eliminate player
    public void eliminatePlayer() {
        this.eliminated = true;
        // Kill player if still alive
        if (isAlive()) {
            // Set player unalive
            setAlive(false);
        }

        if (isOnline()) getPlayer().setGameMode(GameMode.SPECTATOR);
        setRespawning(false);
        playerAfterDeath(null);
    }

    // Runs for every player when a round starts
    public void playerResetCycle () {
        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        playerCycleKills = 0;
        cyclesSurvived++;
    }

    // Selecting spawn
    public KMationSpawn selectSpawn () {

        List<KMationSpawn> spawns = new ArrayList<>(game.getSpawns());
        List<KMationSpawn> noEnemyNearbySpawns = new ArrayList<>();

        for (KMationSpawn spawn : spawns) {
            spawn.setNearestEnemyDistanceMinusTarget();
            if (!spawn.isEnemyNearbySpawn(this)) {
                noEnemyNearbySpawns.add(spawn);
            }
        }

        if (noEnemyNearbySpawns.size() > 0) {
            noEnemyNearbySpawns.sort(Comparator.comparingDouble(KMationSpawn::getNearestEnemyDistanceMinusTarget));
            return noEnemyNearbySpawns.get(0);
        } else {
            spawns.sort(Comparator.comparingDouble(KMationSpawn::getNearestEnemyDistanceMinusTarget));
            return spawns.get(0);
        }
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public int getCycleKills() {
        return playerCycleKills;
    }

    public void setPlacement(int currentPlacement, boolean tied) {
        cyclePlacement = currentPlacement;
        this.tied = tied;
    }

    public int getPlacement() {
        return cyclePlacement;
    }

    public int getCyclesSurvived() {
        return cyclesSurvived;
    }

    public void setInDanger(boolean inDanger) {
        this.inDanger = inDanger;
    }

    public boolean isInDanger() {
        return inDanger;
    }

    public boolean isTied() {
        return tied;
    }
}
