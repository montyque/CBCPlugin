package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class TagPlayer extends CBCPlayer {

    private final TagGame game;

    private boolean alreadyEliminated = false;
    private boolean canMove = true;

    private final Set<Integer> roundsEvaderIn = new HashSet<>();

    // Statistics
    private int evadersKilled = 0;
    private int secondsSurvived = 0;
    private float pointsScored = 0;
    private int roundsSurvived = 0;
    private int bonusGameScore = 0;

    // Denotes whether they are counted for points
    private boolean inGame = true;

    public TagPlayer(TagGame game, GameManager gameManager, CombatManager combatManager,
                     Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    // Runs for every player when a round is being setup
    public void playerSetupRound() {

        alreadyEliminated = false;
        if (!isTagger()) {
            roundsEvaderIn.add(game.getRoundNumber());
        }

        // Do not start the round for this player if the player is offline
        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        setRespawning(false);
        setAlive(false); // Set player's alive state to false
        // Set gamemode of player to adventure and reset their stats
        resetPlayer();

        // Manage player effects
        playerEntity.removePotionEffect(PotionEffectType.GLOWING);

        // Give player blindness if tagger
        if (isTagger()) {
            playerEntity.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 9000, 1, true, false));
        }

    }

    public void playerStartRound () {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        // Set gamemode of player to adventure and reset their stats
        playerSetup();
        setReloadsBySecond(2);
        setTempImmune(60);

        playerEntity.removePotionEffect(PotionEffectType.BLINDNESS);

        if (isTagger()) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0, false, false, false));
        }

    }

    @Override
    public void playerSpawn () {

        if (!isOnline()) return;

        playerSetup();
        setReloadsBySecond(2);
        setTempImmune(60);

        // Teleport player back to tagger spawn
        TagTeam tagTeam = (TagTeam) getTeam();
        teleportPlayerToSpawn(tagTeam.getRandomTaggerSpawn(), game.getMap().getMapCentre());

        if (isTagger()) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0, false, false, false));
        }

    }

    @Override
    public Component modifyDeathMessage (CBCPlayer playerKiller, Component deathMessage) {

        // Died as evader, so give taggers points
        if (!game.isRoundInPlay()) {
            return deathMessage;
        }

        // Already eliminated, don't give extra points
        if (alreadyEliminated) {
            return deathMessage;
        }

        if (game.getTaggers() == null) {
            return deathMessage;
        }

        NamedTextColor color = game.getTaggers().getColor();
        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        // Modify the death message if the player was their target
        if (!isTagger()) {
            // Add text to death message
            deathMessage = deathMessage.append(smallText( " +" + game.getCurrentEvaderKillValue() + " POINTS").color(color).decorate(TextDecoration.BOLD));
        }
        return deathMessage;
    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        if (!isTagger()) {

            // The player will not respawn, so we are overriding the old method
            if (isOnline()) {
                Component titleComponent = Component.text("YOU DIED!").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                Title diedTitle = Title.title(titleComponent, Component.text("You've been eliminated!")
                        .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(1000)));
                getPlayer().showTitle(diedTitle);
            }

            // Died as evader, so give taggers points IF not already eliminated
            if (game.isRoundInPlay() && !alreadyEliminated) {
                TagTeam taggerTeam = game.getTaggers();
                if (taggerTeam != null) {
                    taggerTeam.evaderKill();
                    if (playerKiller != null) {
                        // Give player credit for points
                        TagPlayer tagPlayer = (TagPlayer) playerKiller;
                        tagPlayer.addEvaderKill();
                    }
                }

                // Update player counts and check if the round should end
                game.checkPlayerCounts();
            }

            alreadyEliminated = true;
        }
        else {

            // Respawn player as player is a tagger
            setRespawning(true);

            // Find the amount of time that it takes for the players to respawn
            int timeToRespawn = game.getTaggerRespawnTimer();
            // Set up respawn timer
            RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getCombatManager(), this, timeToRespawn + 1);
            respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);

        }

        game.getSidebarManager().updateServerBoard();
        game.getBossbarManager().update();

    }

    private void addEvaderKill() {
        // Add points
        pointsScored += game.getCurrentEvaderKillValue();
        evadersKilled++;
        setGamePoints(getIntPointsScored());
    }

    public boolean isTagger () {
        // Check if player is a tagger or not
        return game.getTaggers() == getTeam();
    }

    public void playerSurvivedSecond () {

        // Increase stats
        secondsSurvived++;

    }

    public void playerSurviveScore () {

        // Give team score increase of one
        if (getTeam() == null) return;

        TagTeam tagTeam = (TagTeam) getTeam();
        float multiplier = (float) game.getMaxScorePerSecond() / tagTeam.getInGamePlayers().size();

        tagTeam.playerSurvivalScore(1 * multiplier);

        pointsScored += 1 * multiplier;
        setGamePoints(getIntPointsScored());

    }

    public void giveSurvivalBonus(int survivalBonus) {

        roundsSurvived++;

        // Give team score increase of one
        if (getTeam() == null) return;
        TagTeam tagTeam = (TagTeam) getTeam();

        tagTeam.playerSurvivalScore(survivalBonus);
        pointsScored += survivalBonus;
        setGamePoints(getIntPointsScored());

    }

    public int getSecondsSurvived() {
        return secondsSurvived;
    }

    public int getIntPointsScored() {
        return Math.round(pointsScored);
    }

    public int getEvadersKilled() {
        return evadersKilled;
    }

    public int getRoundsAsEvaderPlayed() {
        return roundsEvaderIn.size();
    }

    public int getRoundsSurvived() {
        return roundsSurvived;
    }

    public void setEliminated (boolean b) {
        alreadyEliminated = b;
    }

    public void setCanMove(boolean b) {
        canMove = b;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public void automaticElimination () {
        TagTeam taggerTeam = game.getTaggers();
        if (taggerTeam != null) {
            taggerTeam.evaderKill();
            // Send message
            getGameManager().sendGlobalMessage(
                    Component.text(getName() + " has been eliminated by default, granting taggers " +
                            taggerTeam.getTeamName() + " " + game.getCurrentEvaderKillValue() + " points.").color(taggerTeam.getColor())
            );
        }
        alreadyEliminated = true;
    }

    public boolean isInGame() {
        return inGame;
    }

    public void setInGame(boolean b) {
        inGame = b;
    }

    @Override
    public void setGamePoints (int points) {
        super.setGamePoints(points + bonusGameScore);
        game.updateServerSidebar();
    }

    public void setBonusGameScore (int points) {
        bonusGameScore = points;
        setGamePoints(getIntPointsScored());
    }
}
