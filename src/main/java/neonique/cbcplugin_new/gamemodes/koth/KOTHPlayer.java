package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.tasks.TempImmunityTask;
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

    // Constants for game points
    private static final int KILL_PTS = 5; // Points you gain for kills
    private static final int HILL_KILL_PTS = 3; // Extra points you gain for kills on players in the hill
    private static final int HOLDER_KILL_PTS = 3; // Extra points you gain for kills on players holding the point
    private static final int DEFEND_HILL_PTS = 10; // Points you gain for defending a point in the hill
    private static final int CAPTURE_HILL_PTS = 10; // Points you gain for capture the hill
    private static final int TIME_IN_HILL_PTS = 0; // Points you get for every second in the hill
    private int timeInHillLast = 0;

    public KOTHPlayer(KOTHGame game, GameManager gameManager, CombatManager combatManager, Player player) {
        super(gameManager, combatManager, player);
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
        int killPts = KILL_PTS;

        KOTHPlayer kothPlayerKilled = (KOTHPlayer) playerKilled;

        if (kothPlayerKilled.isInHill()) {
            killPts += HILL_KILL_PTS;
        }

        if (kothPlayerKilled.team() == game.getPointControlTeam()) {
            killPts += HOLDER_KILL_PTS;
        }

        // Add game points for kill
        addGamePoints(killPts);
        game.updateServerSidebar();

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // If the player is online and has a team let them respawn
        if (isOnline() && team() != null) {
            clearEffects();
            if (!((KOTHTeam) team()).isOutOfGame()) {
                setRespawnTicks(80);
            }
        }

        // Update bossbar manager
        game.updateBossbarManager();
    }

    public void playerRefresh () {

        playerSetup(2);
        setTempImmune(60);

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
        teleportPlayerToSpawn(((KOTHTeam) team()).getPlayerSpawn());

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
            game.updateClientSidebar(getPlayer());
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

        if (getSecondsInHill() > timeInHillLast) {
            addGamePoints(TIME_IN_HILL_PTS * (getSecondsInHill() - timeInHillLast));
            timeInHillLast = getSecondsInHill();
            game.updateServerSidebar();
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
        addGamePoints(DEFEND_HILL_PTS);
    }

    public void addHillCapture() {
        hillCaptures++;
        addGamePoints(CAPTURE_HILL_PTS);
    }
}
