package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;

import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class ThrowdownPlayer extends CBCPlayer {

    private final ThrowdownGame game;

    // Score
    private int roundsWon = 0;

    // Currently eliminated?
    private boolean eliminated = false;

    // Stats
    private int playerRoundKills = 0;
    private int secondsAlive = 0;

    public ThrowdownPlayer(ThrowdownGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public void teleportToSpawn(Location location) {
        getPlayer().teleport(location);
        Vector dir = game.getMap().getMapCentre().clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = getPlayer().getLocation().setDirection(dir);
        getPlayer().teleport(loc);
    }

    // Runs for every player when a round is being setup
    public void playerSetupRound () {

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        setAlive(false); // Set player's alive state to false
        // Set gamemode of player to adventure and reset their stats
        resetPlayer();

        playerRoundKills = 0;
        eliminated = false;

        // Manage player effects
        playerEntity.removePotionEffect(PotionEffectType.GLOWING);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 800000, 0, false, false, false));

    }

    public void playerStartRound () {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        // Set gamemode of player to adventure and reset their stats
        resetPlayer();
        setAlive(true); // Set player's state to alive
        setImmune(true); // Make player immune

        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);
        playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000, 0, false, false, false));
        new TempImmunityTask(getGameManager(), getWeaponManager(), this, 6).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        game.checkPlayerCounts();

        eliminated = true;
        // The player will not respawn, so we are overriding the old method
        if (isOnline()) {

            // Put player on eliminated team
            game.getGameManager().getCbcScoreboardManager().addTeamEntry(getName(), game.getElimTeam());

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

        // Check player counts
        game.checkPlayerCounts();

    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {
        playerRoundKills++;
        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    public void incrementPlayerSecondsAlive () {
        secondsAlive++;
        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    public int getPlayerSecondsAlive () {
        return secondsAlive;
    }

    public int getPlayerRoundKills () {
        return playerRoundKills;
    }

    public void playerWonRound() {
        roundsWon++;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public int getRoundsWon() {
        return roundsWon;
    }
}
