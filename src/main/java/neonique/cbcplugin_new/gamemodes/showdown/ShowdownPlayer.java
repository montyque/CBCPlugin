package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ShowdownPlayer extends CBCPlayer {

    // Stats
    private int playerRoundKills = 0;
    private int secondsAlive = 0;

    // Constants for game points
    private final static int KILL_PTS = 30; // Points you gain for kills
    private final static int CLUTCH_KILL_PTS = 10; // Extra points you gain for killing a player when less than half of players are alive
    private final static int WINNING_KILL = 15; // Extra points you gain for getting a kill that wins your team a round
    private final static int ROUND_SURVIVAL_PTS = 15; // Points you gain for surviving a round
    private final static int TIME_ALIVE_PTS = 5; // Points you gain every 30 seconds you are alive

    public ShowdownPlayer (Player player, CombatContext combatContext) {
        super(player, combatContext);
    }

    // Runs for every player when a round is being setup
    public void playerSetupRound () {

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        setAlive(false);
        setPermanentlyImmune(false);
        resetPlayer();

        // Reset statistics
        playerRoundKills = 0;

        // Manage player effects
        playerEntity.removePotionEffect(PotionEffectType.GLOWING);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 800000, 0, false, false, false));

    }

    public void playerStartRound () {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        // Set gamemode of player to adventure and reset their stats
        playerSetup(2);
        setTempImmune(60);

        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));

    }

    public void playerAfterKill (CBCPlayer playerKilled) {
        playerRoundKills++;
    }

    public void incrementPlayerSecondsAlive () {
        secondsAlive++;
        if (secondsAlive % 30 == 0) {
            addGamePoints(TIME_ALIVE_PTS);
        }
    }

    public int getPlayerSecondsAlive () {
        return secondsAlive;
    }

    public int getPlayerRoundKills () {
        return playerRoundKills;
    }

    @Override
    public void addGamePoints (int points) {
        super.addGamePoints(points);
    }
}
