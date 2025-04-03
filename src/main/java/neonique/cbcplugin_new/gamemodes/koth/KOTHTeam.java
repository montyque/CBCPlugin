package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KOTHTeam extends CBCTeam {

    private final KOTHGame game;

    // Significant game variables
    private int score = 0;
    int placement = 1;
    boolean tied = true;

    // Map variables
    private Set<Location> teamSpawns; // Where players spawn at the team's base

    boolean outOfGame = false;

    // Statistics
    private int pointsScored = 0;
    private int timesHillCaptured = 0;
    private float totalHeldTime = 0;
    private float longestHeldTime = 0;
    private float currentHeldTime = 0;

    private String materialColorName = "";


    public KOTHTeam(KOTHGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                   String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);
        this.game = game;

        if (teamColor == NamedTextColor.RED) {
            materialColorName = "RED";
        } else if (teamColor == NamedTextColor.BLUE) {
            materialColorName = "BLUE";
        } else if (teamColor == NamedTextColor.GREEN) {
            materialColorName = "LIME";
        } else if (teamColor == NamedTextColor.YELLOW) {
            materialColorName = "YELLOW";
        }

    }

    public void score() {
        // Subtract score
        score--;
        pointsScored++;

        // Give point defended to any player in the point
        for (KOTHPlayer player : getKothPlayers()) {
            if (player.isInHill()) {
                player.addPointDefended();
            }
        }
    }

    public void hillCaptured () {
        timesHillCaptured++;

        // Get all team members and check if they are in hill
        // Give point defended to any player in the point
        for (KOTHPlayer player : getKothPlayers()) {
            if (player.isInHill()) {
                player.addHillCapture();
            }
        }
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



    public int getScore() {
        return score;
    }

    public void setScore(int newScore) {
        score = newScore;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement (int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }

    public Set<KOTHPlayer> getKothPlayers () {
        // Get players at KOTH objects
        Set<KOTHPlayer> kothPlayers = new HashSet<>();
        for (CBCPlayer player : getPlayers()) {
            kothPlayers.add((KOTHPlayer) player);
        }
        return kothPlayers;
    }

    public Location getPlayerSpawn() {

        List<Location> validSpawns = new ArrayList<>();
        for (Location spawn : teamSpawns) {
            boolean validSpawn = true;
            for (Player player : spawn.getNearbyEntitiesByType(Player.class, 0.3)) {
                if (game.getPlayer(player) != null) {
                    validSpawn = false;
                    break;
                }
            }
            if (validSpawn) {
                validSpawns.add(spawn);
            }
        }

        if (validSpawns.size() == 0) {
            validSpawns = new ArrayList<>(teamSpawns);
        }

        return validSpawns.get(new Random().nextInt(validSpawns.size()));
    }

    public void setTeamSpawns(Set<Location> spawns) {
        teamSpawns = spawns;
    }

    public Set<Location> getTeamSpawns() {
        return teamSpawns;
    }

    // Statistics
    public int getTimesHillCaptured() {
        return timesHillCaptured;
    }

    public void increaseHeldTime (float time) {
        totalHeldTime += time;
        currentHeldTime += time;

        // Check if current held time is bigger than longest held time
        if (currentHeldTime > longestHeldTime) {
            longestHeldTime = currentHeldTime;
        }
    }

    public int getSecondsLongestHeldHill () {
        return Math.round(longestHeldTime);
    }

    public int getTotalSecondsHeldHill() {
        return Math.round(totalHeldTime);
    }

    public int getPointsScored() {
        return pointsScored;
    }

    public String getMaterialColorName() {
        return materialColorName;
    }
}
