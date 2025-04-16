package neonique.cbcplugin_new.gamemodes.ctf;

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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class CTFPlayer extends CBCPlayer {

    protected final CTFGame game;

    // Game fields
    protected CTFTeam teamWithFlagPickedUp = null;
    private boolean eliminated = false;

    // Statistics
    protected int defensiveKills = 0;
    protected int flagsPickedUp = 0;
    protected int flagsCaptured = 0;
    protected int mBoostsGiven = 0;

    // Constants for game points
    private static int KILL_PTS = 5; // Points you gain for kills
    private static int DKILL_PTS = 10; // Extra points you gain for defensive kills
    private static int FLAGHOLDER_KILL_PTS = 5; // Extra points you gain for killing a flag holder
    private static int MBOOST_KILL_PTS = 15; // Extra points for giving a morale boost to a teammate
    private static int FINAL_KILL_PTS = 35; // Extra points for getting a final kill
    private static int FLAGCAPTURE_PTS = 150; // Points for capturing a flag

    public CTFPlayer(CTFGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;
    }

    public CTFTeam getFlagHeld () {
        return teamWithFlagPickedUp;
    }

    public void playerPickupFlag (CTFTeam flagTeam) {

        if (!isOnline()) return;

        teamWithFlagPickedUp = flagTeam;
        // Heal player and give player absorption hearts
        getPlayer().setHealth(20);
        getPlayer().addPotionEffect(
                new PotionEffect(PotionEffectType.ABSORPTION, 3000000, 0, false, false, false)
        );

        // Play sound for yourself and allies
        CTFTeam ownTeam = (CTFTeam) getTeam();

        ownTeam.allyPickedUpFlag();

        // Increment stats
        flagsPickedUp++;

        // Set player helmet to banner
        setOverrideGlassHelmet(true);
        getPlayer().getInventory().setHelmet(flagTeam.getBannerItem());
        getPlayer().updateInventory();

        // Add flag
        List<Component> suffixes = new ArrayList<>(Collections.singletonList(Component.text("⚑ ").color(flagTeam.getColor())));
        setPlayerListSuffixes(suffixes);

        // Send message
        getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("FLAG PICKED UP > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(getNameComponent())
                        .append(Component.text(" has picked up the ").color(NamedTextColor.WHITE))
                        .append(Component.text("⚑ " + teamWithFlagPickedUp.getTeamName() + " Flag").color(teamWithFlagPickedUp.getColor()).decorate(TextDecoration.BOLD))
                        .append(Component.text(".").color(NamedTextColor.WHITE)).append(Component.newline())
        );

        // Display title
        Title title = Title.title(
                Component.text("⚑ Picked up " + flagTeam.getTeamName() + " Flag!").color(flagTeam.getColor()).decorate(TextDecoration.BOLD),
                Component.text("Run back to your flag to capture it!").color(NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150))
        );
        getPlayer().showTitle(title);

        try {
            updateToBaseLaser();
        } catch (ReflectiveOperationException ignored) {}

    }

    public void playerCaptureFlag () {

        if (!isOnline()) return;

        clearPlayerListSuffixes();

        CTFTeam teamCaptured = teamWithFlagPickedUp;
        teamWithFlagPickedUp = null;

        getGameManager().playSound(getPlayer().getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 17, 1);

        Component flagCaptureComponent = Component.newline().append(Component.text("FLAG CAPTURED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(getNameComponent())
                .append(Component.text(" has captured the ").color(NamedTextColor.WHITE))
                .append(Component.text("⚑ " + teamCaptured.getTeamName() + " Flag").color(teamCaptured.getColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("!").color(NamedTextColor.WHITE));

        // Display title
        Title title = Title.title(
                Component.text("⚑ Captured " + teamCaptured.getTeamName() + " Flag!").color(teamCaptured.getColor()).decorate(TextDecoration.BOLD),
                Component.text("+1 Flags Captured").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150))
        );
        getPlayer().showTitle(title);

        // Send message
        if (teamCaptured.getFlagsLeft() == 1) {
            getGameManager().sendGlobalMessage(
                    flagCaptureComponent.append(Component.text( " " + teamCaptured.getTeamName() + " Team").color(teamCaptured.getColor()).decorate(TextDecoration.BOLD))
                            .append(Component.text(" will no longer respawn!").color(NamedTextColor.WHITE))
                            .append(Component.newline())
            );
        }
        else {
            getGameManager().sendGlobalMessage(
                    flagCaptureComponent.append(Component.newline())
            );
        }

        // Increment stats
        flagsCaptured++;

        // Add game points
        addGamePoints(FLAGCAPTURE_PTS);

        // Reset player's health and effects
        getPlayer().setHealth(20);
        for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
        }

        // Set player helmet back to normal
        setOverrideGlassHelmet(false);
        loadInventory();

    }


    public void playerDropFlag () {

        clearPlayerListSuffixes();

        CTFTeam flagDropped = teamWithFlagPickedUp;
        teamWithFlagPickedUp = null;

        getGameManager().sendGlobalMessage(
                Component.text("FLAG DROPPED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                        .append(getNameComponent())
                        .append(Component.text(" dropped the ").color(NamedTextColor.WHITE))
                        .append(Component.text("⚑ " + flagDropped.getTeamName() + " Flag").color(flagDropped.getColor()).decorate(TextDecoration.BOLD))
                        .append(Component.text("!").color(NamedTextColor.WHITE))
        );

        // Set player helmet back to normal
        setOverrideGlassHelmet(false);

        if (isAlive()) {
            loadInventory();
        }
    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        clearPlayerListSuffixes();
        CTFPlayer ctfPlayerKiller = null;
        if (playerKiller != null) {
            ctfPlayerKiller = (CTFPlayer) playerKiller;
        }

        boolean defensiveKillGive = false;

        // Check if near a team's base
        if (ctfPlayerKiller != null) {
            if (isOnline()) {
                for (Player playerNearby : ((CTFTeam) playerKiller.getTeam()).getFlagLocation().getNearbyPlayers(game.getDefensiveKillRadius())) {
                    if (playerNearby == getPlayer() && ((CTFTeam) playerKiller.getTeam()).isFlagAtBase()) {
                        defensiveKillGive = true;
                        break;
                    }
                }
            }
        }

        // Drop flag
        if (teamWithFlagPickedUp != null) {
            // Give a defensive kill to the player if they are on the team
            if (ctfPlayerKiller != null) {
                if (ctfPlayerKiller.getTeam() == teamWithFlagPickedUp) {
                    defensiveKillGive = true;
                    // Give extra points for killing flag holder
                    playerKiller.addGamePoints(FLAGHOLDER_KILL_PTS);
                }
            }

            teamWithFlagPickedUp.flagReset();
            playerDropFlag();
        }

        if (defensiveKillGive) ctfPlayerKiller.incrementDefensiveKills();

        CTFTeam team = (CTFTeam) getTeam();

        // The player will respawn, so we are overriding the old method
        if (isOnline() && getTeam() != null) {

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            if (!(getTeam() instanceof CTFTeam)) return;
            // Make sure team is still able to respawn
            if (team.canRespawn()) {

                setRespawning(true);

                // Find the amount of time that it takes for the players to respawn
                int timeToRespawn = game.getRespawnTime(team.getOnlinePlayers().size());
                // Set up respawn timer
                RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getCombatManager(), this, timeToRespawn + 1);
                respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);
            } else {
                // Player can no longer respawn
                Component titleComponent = Component.text("YOU DIED!").color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                Title diedTitle = Title.title(titleComponent, Component.text("You've been eliminated!")
                        .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(1000)));
                getPlayer().showTitle(diedTitle);
            }
        }
        if (!team.canRespawn()) {
            // Player can no longer respawn
            if (!eliminated) {
                eliminatePlayer();
                // Add points to player killer
                if (playerKiller != null) {
                    playerKiller.addGamePoints(FINAL_KILL_PTS);
                }
            }
        }

        // Update sidebar
        game.getSidebarManager().updateServerBoard();
    }

    @Override
    public void playerAfterKill(CBCPlayer playerKilled) {

        int killPoints = KILL_PTS;

        CTFPlayer ctfPlayerKilled = (CTFPlayer) playerKilled;

        // Morale boosts
        // Check if teammate is currently holding a flag
        for (CBCPlayer teammate : getTeam().getAlivePlayers()) {
            CTFPlayer ctfteammate = (CTFPlayer) teammate;

            if (ctfteammate == this) continue;

            if (ctfteammate.getFlagHeld() == ctfPlayerKilled.getTeam()) {

                if (!ctfteammate.isOnline()) continue;

                ctfteammate.addHealing(3);

                if (isOnline()) {
                    getPlayer().sendMessage(
                            Component.text("Morale Boost given to ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                                    .append(Component.text(ctfteammate.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                                    .append(Component.text("! (Teammate receives + 1.5 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                    );
                }

                mBoostsGiven++;
                ctfteammate.getPlayer().sendMessage(
                        Component.text("Morale Boost received from ").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC)
                                .append(Component.text(getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                                .append(Component.text("! ( + 1.5 ❤)").color(NamedTextColor.GREEN).decorate(TextDecoration.ITALIC))
                );

                // Add points for morale boost
                killPoints += MBOOST_KILL_PTS;
            }
        }

        // Add points to player
        addGamePoints(killPoints);
        game.getSidebarManager().updateServerBoard();

    }

    public void eliminatePlayer() {
        eliminated = true;
        // Send message
        getGameManager().sendGlobalMessage(
                Component.text("PLAYER ELIMINATION > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                        .append(getNameComponent())
                        .append(Component.text(" has been eliminated!").color(NamedTextColor.WHITE))
        );
        // Update team player counts
        ((CTFTeam) getTeam()).countNonEliminatedPlayers();
    }

    @Override
    public void playerSpawn() {

        if (!isOnline()) return;

        if (game.getWinner() == null) {
            new TempImmunityTask(getGameManager(), getCombatManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        }
        else {
            setImmune(true);
        }

        teleportPlayerToSpawn();
        playerSetup();
        setReloadsBySecond(2);

        // Update sidebar
        game.getSidebarManager().updateServerBoard();
    }

    public void teleportPlayerToSpawn() {

        if (getTeam() == null) return;
        if (!(getTeam() instanceof CTFTeam team)) return;

        getPlayer().teleport(team.getPlayerSpawn());

        Vector dir = game.getMap().getMapCentre().clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = getPlayer().getLocation().setDirection(dir);
        getPlayer().teleport(loc);

    }

    public void incrementDefensiveKills () {

        if (!isOnline()) return;

        defensiveKills++;
        getPlayer().sendMessage(
                Component.text("+1 Defensive Kills").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC)
        );

        // Give points
        addGamePoints(DKILL_PTS);

    }

    public int getDefensiveKills() {
        return defensiveKills;
    }

    public int getFlagsPickedUp() {
        return flagsPickedUp;
    }

    public int getFlagsCaptured() {
        return flagsCaptured;
    }

    public int getmBoostsGiven() {
        return mBoostsGiven;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public Set<Player> getGlowingPlayers() {

        Set<Player> glowingPlayers = new HashSet<>();
        for (CBCPlayer player : getTeam().getAlivePlayers()) {
            if (player == this) continue;
            if (player.isAlive()) {
                glowingPlayers.add(player.getPlayer());
            }
        }

        // Make player glow if they are holding a flag
        if (((CTFTeam) getTeam()).getFlagHolder() != null) {
            CTFPlayer flagHolder = ((CTFTeam) getTeam()).getFlagHolder();
            if (flagHolder.isAlive() && flagHolder.isOnline()) {
                glowingPlayers.add(flagHolder.getPlayer());
            }
        }

        return glowingPlayers;
    }

    public void updateFlagHolderLaser() throws ReflectiveOperationException {

        /*if (!isOnline()) {
            if (toFlagHolderLaser != null) {
                toFlagHolderLaser = null;
            }
        }

        CTFTeam team = (CTFTeam) getTeam();
        if (team.getFlagHolder() != null) {
            // Player has flag
            CTFPlayer playerWithFlag = team.getFlagHolder();
            // Check if player has laser
            if (toFlagHolderLaser == null) {
                toFlagHolderLaser = new Laser.GuardianLaser(team.getFlagHolder().getPlayer().getLocation(), getPlayer(), -1, 300);
                toFlagHolderLaser.start(CBCPlugin.getPlugin(), getPlayer());
            } else {
                // Update laser
                toFlagHolderLaser.moveStart(team.getFlagHolder().getPlayer().getLocation());
            }
        } else {
            if (toFlagHolderLaser != null) {
                try {
                    toFlagHolderLaser.stop();
                } catch (NullPointerException ignored) {}
                toFlagHolderLaser = null;
            }
        }*/
    }

    public void updateToBaseLaser() throws ReflectiveOperationException {

        /*if (!isOnline()) {
            if (toBaseLaser != null) {
                toBaseLaser = null;
            }
        }

        if (!isAlive()) return;

        CTFTeam team = (CTFTeam) getTeam();
        if (toBaseLaser == null) {
            toBaseLaser = new Laser.GuardianLaser(team.getFlagLocation().clone().add(0, 0.5, 0), getPlayer(), -1, 300);
            toBaseLaser.start(CBCPlugin.getPlugin(), getPlayer());
        }*/
    }
}
