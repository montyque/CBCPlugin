package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    public KMationPlayer(KMationGame game, GameManager gameManager, CombatManager combatManager, Player player) {
        super(gameManager, combatManager, player);
        this.game = game;
    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {
        playerCycleKills++;
        game.updatePlayersInLast();
        game.updatePlacements();
        game.updateServerSidebar();

        if (game.isOvertime() && playerCycleKills >= game.getOvertimeThreshold() && game.getWinner() == null) {
            game.endCycle();
        }
    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // The player will respawn, so we are overriding the old method
        if (isOnline()) {

            clearEffects();

            // Check if player is eliminated
            if (!eliminated) {
                setRespawnTicks(80);
            }
        }

        game.updateBossbarManager();

    }

    @Override
    public Title getDeathTitle() {
        if (eliminated) {
            return Title.title(
                    Component.text("ELIMINATED").color(NamedTextColor.RED),
                    Component.text("Eliminated in Cycle " + (cyclesSurvived + 1))
                    .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(1000)));
        } else {
            return getRespawnTitle();
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
        teleportPlayerToSpawn(selectSpawn(), game.getMap().getMapCentre());

        playerSetup(2);

        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));

    }

    // Runs when the game is being set up
    public void playerSetupGame () {

        if (!isOnline()) return;
        resetPlayer();
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 800000, 0, false, false, false));

    }

    // Runs when the game countdown timer stops
    public void playerStartGame () {

        Player playerEntity = getPlayer();

        // Set gamemode of player to adventure and reset their stats
        playerSetup(2);
        setTempImmune(60);

        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));
        new TempImmunityTask(getGameManager(), getCombatManager(), this, 6).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

    }

    // Eliminate player
    public void eliminatePlayer() {
        this.eliminated = true;

        if (isAlive()) {
            setAlive(false);
        }

        if (isOnline()) getPlayer().setGameMode(GameMode.SPECTATOR);
        setRespawnTicks(0);
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

        if (!noEnemyNearbySpawns.isEmpty()) {
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
