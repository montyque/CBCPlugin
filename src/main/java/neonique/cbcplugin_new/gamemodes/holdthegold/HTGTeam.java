package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class HTGTeam extends CBCTeam {

    // Set variables relating to game
    private final HTGGame game;
    private int score;

    private Location startSpawn;

    // Statistics
    private int timesPickedUp = 0;
    private int totalPointsScored = 0;
    boolean outOfGame = false;

    // Placement
    int placement = 1;
    boolean tied = true;

    public HTGTeam(HTGGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                   String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);

        this.game = game;
        score = game.getStartScore();
    }

    public void setStartSpawn (Location spawn) {
        startSpawn = spawn;
    }

    public void createSpawnBox () {
        // Creates a 5x5 hollow barrier
        for (double boxX = -2; boxX < 3; boxX++) {
            for (double boxY = -1; boxY < 4; boxY++) {
                for (double boxZ = -2; boxZ < 3; boxZ++) {
                    // The floors of the box
                    if (boxY == -1 || boxY == 3) {
                        startSpawn.clone().add(boxX, boxY, boxZ).getBlock().setType(Material.BARRIER);
                    } else if (Math.abs(boxX) == 2 || Math.abs(boxZ) == 2) {
                        startSpawn.clone().add(boxX, boxY, boxZ).getBlock().setType(Material.BARRIER);
                    }
                }
            }
        }
    }

    public void removeSpawnBox () {
        // Remove barrier box around spawns
        for (double boxX = -2; boxX < 3; boxX++) {
            for (double boxY = -1; boxY < 4; boxY++) {
                for (double boxZ = -2; boxZ < 3; boxZ++) {
                    if (startSpawn.clone().add(boxX, boxY, boxZ).getBlock().getType() == Material.BARRIER) {
                        startSpawn.clone().add(boxX, boxY, boxZ).getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void score() {
        score--;
        totalPointsScored++;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int newScore) {
        score = newScore;
    }

    public void incrementTimesPickedUp() {
        timesPickedUp++;
    }

    public int getGoldScore() {
        return totalPointsScored;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement (int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }

    public void teamOutOfGame (int place) {

        outOfGame = true;

        Component titleComponent = Component.text(StringUtil.getPlacementString(place).toUpperCase() + " PLACE").decorate(TextDecoration.BOLD).color(getColor());
        Component subtitleComponent = Component.text("You've earned your spot!");

        Title title = Title.title(titleComponent, subtitleComponent, TextUtil.titleTimes(0, 3000, 700));

        for (CBCPlayer player : getPlayers()) {
            // Kill player if still alive
            if (player.isAlive()) {
                // Set player unalive
                player.setAlive(false);
                player.playerAfterDeath(null);
            }

            player.setRespawning(false);

            // Show title to players
            if (player.isOnline()) {
                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                player.getPlayer().showTitle(title);
            }
        }
    }

    public boolean isOutOfGame() {
        return outOfGame;
    }
}
