package neonique.cbcplugin_new.gamemodes.koth;

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

public class KOTHPlayer extends CBCPlayer {

    private final KOTHGame game;
    private boolean inHill = false;

    // Statistics
    private float timeInHill = 0f;
    private int hillCaptures = 0;
    private int pointsDefended = 0;

    public KOTHPlayer(KOTHGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void teleportPlayerToSpawn(Location spawn) {
        // Teleport player to spawn location
        getPlayer().teleport(spawn);
        // Make player face map center
        faceToLocation(game.getMap().getMapCentre(), false);
    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {

        // Set game points for kill
        // int killPts = 5;

        // Add game points for kill
        // addGamePoints(killPts);

        // Update leaderboards
        // game.updateTopKillsList();
        // game.updateTopGameScoreList();

        // Update sidebar manager
        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // If the player is online and has a team let them respawn
        if (isOnline() && getTeam() != null) {

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            if (!((KOTHTeam) getTeam()).isOutOfGame()) {
                // Respawn player
                setRespawning(true);

                // Find the amount of time that it takes for the players to respawn
                int timeToRespawn = 4;
                // Set up respawn timer
                RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getWeaponManager(), this, timeToRespawn + 1);
                respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);
            }

        }

        // Update bossbar manager
        game.getBossbarManager().update();
    }

    public void playerRefresh () {
        resetPlayer();
        setAlive(true);
        setRespawning(false);
        setReloadsBySecond(1);
        loadout();
        setTempImmune(60);
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

        // Teleport player to spawn point
        teleportPlayerToSpawn(((KOTHTeam) getTeam()).getPlayerSpawn());

        playerRefresh();
    }

    public void setInHill (boolean inHill) {

        boolean toggle = false;

        // Toggle player's glowing
        if (inHill && !this.inHill) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000,
                    0, false, false, false));
            toggle = true;
        }
        else if (!inHill && this.inHill) {
            getPlayer().removePotionEffect(PotionEffectType.GLOWING);
            toggle = true;
        }

        this.inHill = inHill;

        // Update client board
        if (toggle && isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    public boolean isInHill() {
        return inHill;
    }

    // Statistics handling
    public int getSecondsInHill() {
        return Math.round(timeInHill);
    }

    public void addTimeInHill (float time) {
        timeInHill += time;
        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    public int getHillCaptures () {
        return hillCaptures;
    }

    public int getPointsDefended () {
        return pointsDefended;
    }

    public void addPointDefended () {
        pointsDefended++;
        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    public void addHillCapture() {
        hillCaptures++;
    }
}
