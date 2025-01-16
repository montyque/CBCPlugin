package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class KOTHBossbarManager extends GameBossBarManager {

    private final KOTHGame game;

    public KOTHBossbarManager(KOTHGame game) {

        // Create rows
        super(4);

        this.game = game;

    }

    @Override
    public void update () {

        // Update player display
        setServerText(1, createPlayerDisplayComponent());

        // Show the current point status
        Component middle;
        if (game.getPointControlTeam() == null) {
            middle = smallText(" POINT VACANT ").color(NamedTextColor.WHITE);
        }
        else {
            middle = smallText(" " + game.getPointControlTeam().getTeamName() + " CONTROL ").color(game.getPointControlTeam().getColor());
        }

        NamedTextColor barColor = NamedTextColor.GRAY;
        if (game.getPointCaptureTeam() != null) {
            barColor = game.getPointCaptureTeam().getColor();
        }
        // Calculate percentage of bar that should be filled
        float barPercentage = (float) game.getPointCaptureProgress() / (float) game.getPointCaptureMax();

        final int barLength = 12;
        int barColored = Math.round(barPercentage * (float) barLength);

        Component leftSide = blankComponent();
        Component rightSide = blankComponent();

        // Create components for bar on left and right side
        for (int i = 0; i < barLength; i++) {

            // Left side
            if (i < barColored) {
                leftSide = leftSide.append(smallText("\uE000").color(barColor));
            }
            else {
                leftSide = leftSide.append(smallText("\uE000").color(NamedTextColor.GRAY));
            }

            // Right side
            if ((barLength - (i + 1)) < barColored) {
                rightSide = rightSide.append(smallText("\uE001").color(barColor));
            }
            else {
                rightSide = rightSide.append(smallText("\uE001").color(NamedTextColor.GRAY));
            }
        }

        setServerText(2, leftSide.append(middle).append(rightSide));

        // Get score component
        Component currentScore = smallText("").color(NamedTextColor.WHITE);
        List<KOTHTeam> teams = new ArrayList<>(game.getTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (KOTHTeam team : teams) {
            currentScore = currentScore.append(smallRaisedText(String.valueOf(team.getScore())).color(team.getColor()));
            i++;
            if (i != teams.size()) {
                currentScore = currentScore.append(smallRaisedText("-").color(NamedTextColor.WHITE));
            }
        }
        setServerText(3, currentScore);

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<KOTHTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (KOTHTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<CBCPlayer> players = team.getPlayers();

            for (CBCPlayer player : players) {

                j++;

                // Get player health
                double healthDec = 0;
                if (player.isOnline() && player.isAlive()) {
                    Player playerEntity = player.getPlayer();
                    AttributeInstance maxHealth = playerEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                    if (maxHealth == null) {
                        healthDec = playerEntity.getHealth() / 20d;
                    }
                    else {
                        healthDec = playerEntity.getHealth() / maxHealth.getValue();
                    }
                }

                // Create component
                Component playerComponent;
                if (game.getGameLength() == 0) {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isOnline(), team.getColor());
                }
                else {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), team.getColor());
                }

                // Check if player is in hill
                if (((KOTHPlayer) player).isInHill()) {
                    // Add outline around player
                    playerComponent = playerComponent.append(Component.text("\uF808\uF802\uE470\uF801").color(team.getColor()));
                }

                teamComponent = teamComponent.append(playerComponent);
                // Add a space in between each player
                if (j != players.size()) {
                    teamComponent = teamComponent.append(Component.text(playerSeparator).color(NamedTextColor.WHITE));
                }
            }

            playerDisplayComponent = playerDisplayComponent.append(teamComponent);
            // Add a space in between each team
            if (i != teams.size()) {
                playerDisplayComponent = playerDisplayComponent.append(Component.text(teamSeparator).color(NamedTextColor.WHITE));
            }
        }
        return playerDisplayComponent;
    }

}