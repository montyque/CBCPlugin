package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;

public class ShowdownPlayer extends CBCPlayer {

    private final ShowdownGame game;

    // Stats
    private int playerRoundKills = 0;
    private int secondsAlive = 0;

    // Constants for game points
    private final static int KILL_PTS = 30; // Points you gain for kills
    private final static int CLUTCH_KILL_PTS = 10; // Extra points you gain for killing a player when less than half of players are alive
    private final static int WINNING_KILL = 15; // Extra points you gain for getting a kill that wins your team a round
    private final static int ROUND_SURVIVAL_PTS = 20; // Points you gain for surviving a round
    private final static int TIME_ALIVE_PTS = 5; // Points you gain every 30 seconds you are alive

    public ShowdownPlayer(ShowdownGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    // Runs for every player when a round is being setup
    public void playerSetupRound () {

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        setAlive(false); // Set player's alive state to false
        // Set gamemode of player to adventure and reset their stats
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
        playerSetup();
        setReloadsBySecond(2);
        setTempImmune(60);

        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (game.isPlayerGlowingEnabled()) {
            playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));
        }

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // Update player counts
        game.checkPlayerCounts();
        // The player will not respawn, so we are overriding the old method
        if (isOnline()) {
            Component titleComponent = Component.text("YOU DIED!").color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);
            Title diedTitle = Title.title(titleComponent, Component.text("You've been eliminated!")
                    .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(1000)));
            getPlayer().showTitle(diedTitle);

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }
        }

        // Update boss bar
        game.updateBossbarManager();
        game.updateServerSidebar();

    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {

        // Calculate points gained from kill
        int killPts = KILL_PTS;

        // Check if less than half of players remain after kill
        if (game.getTotalPlayers() / 2 <= game.getPlayersAlive()) {
            killPts += CLUTCH_KILL_PTS;
        }

        // Check if only player remains is this player
        if (isAlive() && game.getPlayersAlive() == 1) {
            killPts += WINNING_KILL;
        }

        addGamePoints(killPts);

        playerRoundKills++;
        game.updateServerSidebar();
    }

    public void playerSurvivedRound () {
        addGamePoints(ROUND_SURVIVAL_PTS);
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
        game.getSidebarManager().updateServerBoard();
    }
}
