package neonique.cbcplugin_new.tasks.gamemodetasks.tdm;

import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import neonique.cbcplugin_new.gamemodes.tdm.TDMTeam;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TDMBossBarTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final TDMGame game;

    public TDMBossBarTask(GameManager gameManager, TDMGame game) {
        this.gameManager = gameManager;
        this.game = game;
    }

    @Override
    public void run() {

        /*if (this.game.isGameOver()) {
            if (game.getGameBossBar() != null) {
                gameManager.hideGlobalBossbar(game.getGameBossBar());
            }
            this.cancel();
            return;
        }

        if (game.getGameBossBar() == null) {
            return;
        }

        BossBar bossBar = game.getGameBossBar();

        // If there's a winner then set bossbar
        if (game.getWinner() != null) {
            TDMTeam winningTeam = game.getWinner();
            bossBar.name(Component.text(winningTeam.getTeamName().toUpperCase() + " WINS!")
                    .color(winningTeam.getColor())
                    .decorate(TextDecoration.BOLD));
            setBossbarColorByTeam(winningTeam, bossBar);
            bossBar.progress(1f);
        } else {

            // Figure out which team is currently leading
            List<TDMTeam> teamsLeading = game.getLeadingTeams();

            // Create text that will be displayed to show who is leading
            // This text is displayed on left side of bar title
            Component leadingText;
            if (teamsLeading.size() == 1) {
                TDMTeam teamLeading = teamsLeading.get(0);
                leadingText = Component.text(teamLeading.getTeamName()).color(teamLeading.getColor()).decorate(TextDecoration.BOLD)
                        .append(Component.text(" leading - ").color(NamedTextColor.WHITE));
            } else if (teamsLeading.size() == 2) {
                TDMTeam teamLeading1 = teamsLeading.get(0);
                TDMTeam teamLeading2 = teamsLeading.get(1);
                leadingText = Component.text(teamLeading1.getTeamName()).color(teamLeading1.getColor()).decorate(TextDecoration.BOLD)
                        .append(Component.text(" and ").color(NamedTextColor.WHITE))
                        .append(Component.text(teamLeading2.getTeamName()).color(teamLeading2.getColor()).decorate(TextDecoration.BOLD))
                        .append(Component.text(" tied for lead - ").color(NamedTextColor.WHITE));
            } else {
                leadingText = Component.text(teamsLeading.size() + " teams").decorate(TextDecoration.BOLD)
                        .append(Component.text(" tied for lead - ")).color(NamedTextColor.WHITE);
            }

            // If game is by timer
            if (game.isGameByTimer()) {

                if (!game.isOvertime()) {
                    // Set color depending on timer
                    // If timer is below 3 minutes, set color to yellow
                    // If timer is below 1 minute, set color to red
                    // If neither, set color to green
                    if (game.getTimer() <= 60) {
                        bossBar.color(BossBar.Color.RED);
                        bossBar.name(leadingText.append(
                                Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                        Component.text(game.timerToText()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))));
                    } else if (game.getTimer() <= 180) {
                        bossBar.color(BossBar.Color.YELLOW);
                        bossBar.name(leadingText.append(
                                Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                        Component.text(game.timerToText()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))));
                    } else {
                        bossBar.color(BossBar.Color.GREEN);
                        bossBar.name(leadingText.append(
                                Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                        Component.text(game.timerToText()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))));
                    }

                    bossBar.progress((float) game.getTimer() / (float) game.getMaxTimer());
                } else {

                    bossBar.name(leadingText.append(
                            Component.text("OVERTIME:").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).append(
                                    Component.text(" First to ").color(NamedTextColor.WHITE)).append(
                                    Component.text(game.getOvertimeKillsToWin()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)).append(
                                    Component.text(" wins").color(NamedTextColor.WHITE))
                            )
                    );

                    bossBar.color(BossBar.Color.RED);
                    bossBar.progress(0f);
                }
            }
        }

        gameManager.showGlobalBossbar(bossBar);*/
    }

    private void setBossbarColorByTeam(TDMTeam team, BossBar bossBar) {

        if (NamedTextColor.RED.equals(team.getColor())) {
            bossBar.color(BossBar.Color.RED);
        } else if (NamedTextColor.BLUE.equals(team.getColor())) {
            bossBar.color(BossBar.Color.BLUE);
        } else if (NamedTextColor.GREEN.equals(team.getColor())) {
            bossBar.color(BossBar.Color.GREEN);
        } else if (NamedTextColor.YELLOW.equals(team.getColor())) {
            bossBar.color(BossBar.Color.YELLOW);
        } else if (NamedTextColor.AQUA.equals(team.getColor())) {
            bossBar.color(BossBar.Color.BLUE);
        } else if (NamedTextColor.GOLD.equals(team.getColor())) {
            bossBar.color(BossBar.Color.YELLOW);
        } else if (NamedTextColor.LIGHT_PURPLE.equals(team.getColor())) {
            bossBar.color(BossBar.Color.PINK);
        } else if (NamedTextColor.DARK_PURPLE.equals(team.getColor())) {
            bossBar.color(BossBar.Color.PURPLE);
        }
    }
}
