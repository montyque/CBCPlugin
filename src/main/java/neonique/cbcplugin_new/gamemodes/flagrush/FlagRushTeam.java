package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

public class FlagRushTeam extends CTFTeam {

    private final FlagRushGame game;

    // Current score
    private int score;

    // Placement
    int placement = 1;
    boolean tied = true;

    public FlagRushTeam(FlagRushGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor, String prefix, ItemStack item, ItemStack glassHead) {
        super(game, teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);
        score = game.getScoreStart();
        this.game = game;
    }

    @Override
    public void flagCaptured() {

        // Create and show title to players on team
        Component title;
        Component subtitle;

        title = Component.text("Your flag was captured!").color(getColor()).decorate(TextDecoration.BOLD);
        subtitle = Component.text("Captured by ").color(NamedTextColor.WHITE)
                .append(flagHolder.getNameComponent());

        for (CBCPlayer teamPlayer : getOnlinePlayers()) {
            if (teamPlayer.isOnline()) {
                teamPlayer.getPlayer().showTitle(
                        Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2000), Duration.ofMillis(150)))
                );
            }
        }

        // Take points away from team score if not in overtime
        if (!game.isOvertime()) {
            int flagLostScore = game.getFlagLostPoints();
            score -= flagLostScore;
        }

        // Check if it's below 0, and if so set it to 0 - no negative scores
        if (score < 0) score = 0;

        timesFlagCaptured++;
        flagReset();

        // Update placements in the game
        game.updatePlacements();
    }

    public void flagCapturedByTeam (CTFTeam teamCaptured) {

        // Add points to score
        int flagCapturePoints = game.getFlagCapturePoints();
        score += flagCapturePoints;

        // Check if it's above 0, and if so set it to 9999 - no 5 digit scores
        if (score > 9999) score = 9999;

        // Display title to all players in the team
        Title title = Title.title(
                Component.text("⚑ Captured " + teamCaptured.getTeamName() + " Flag!").color(teamCaptured.getColor()).decorate(TextDecoration.BOLD),
                Component.text("+" + flagCapturePoints + " Points").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC),
                Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1000), Duration.ofMillis(150))
        );

        for (CBCPlayer teamPlayer : getOnlinePlayers()) {
            if (teamPlayer.isOnline()) {
                teamPlayer.getPlayer().showTitle(title);
            }
        }

        if (game.isOvertime()) {
            game.checkWinnerOvertime(this);
        }
    }

    public int getScore() {
        return score;
    }

    public int getPlacement () {
        return placement;
    }

    public boolean isTied () {
        return tied;
    }

    public void setPlacement(int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }
}
