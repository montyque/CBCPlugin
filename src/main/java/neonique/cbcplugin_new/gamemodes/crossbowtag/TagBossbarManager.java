package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public class TagBossbarManager extends GameBossBarManager {

    private final TagGame game;

    public TagBossbarManager(TagGame game) {

        // Create rows
        super(4);

        this.game = game;

    }

    @Override
    public void update () {

        // Update player display
        setServerText(1, createPlayerDisplayComponent());

        // Show the round number and the amount of time remaining in the round
        int roundNumber = game.getRoundNumber();
        int roundTimer = game.getRoundTimer();
        int evaderReleaseTimer = game.getTaggerReleaseTimer();

        Component row3 = smallText("ROUND " + roundNumber).color(NamedTextColor.AQUA).append(smallText(" \uE000 ").color(NamedTextColor.WHITE));

        TagTeam taggers = game.getTaggers();
        if (taggers != null) {

            NamedTextColor taggersColor = game.getTaggers().getColor();

            // If evader release timer is greater than 0, show release timer
            if (evaderReleaseTimer > 0) {
                row3 = row3.append(smallText("TAGGERS RELEASED IN " + evaderReleaseTimer).color(taggersColor));
            }
            // Otherwise, show round timer
            else {

                // Get color of timer
                NamedTextColor timerColor = NamedTextColor.AQUA;
                if (roundTimer < 10) {
                    timerColor = NamedTextColor.RED;
                } else if (roundTimer < 30) {
                    timerColor = NamedTextColor.GOLD;
                } else if (roundTimer < 60) {
                    timerColor = NamedTextColor.YELLOW;
                }

                String roundTimerText = timerToText(roundTimer);
                row3 = row3.append(smallText(roundTimerText).color(timerColor));
            }

        }

        // Set the third row to timer
        setServerText(2, row3);

        // Show the amount of evaders remaining
        int evadersRemaining = game.getEvadersAlive();
        int evadersTotal = game.getEvaders().size();
        if (game.isStartRoundTimer()) {
            evadersRemaining = evadersTotal;
        }

        if (game.getWinner() == null) {
            if (evadersTotal > 0) {
                // Change the color of the text depending on the percentage of evaders remaining
                NamedTextColor evadersRemainingColor = NamedTextColor.GREEN;
                float evadersRemainingPercentage = ((float) evadersRemaining / (float) evadersTotal) * 100;

                if (evadersRemainingPercentage == 0) evadersRemainingColor = NamedTextColor.RED;
                else if (evadersRemainingPercentage <= 25) evadersRemainingColor = NamedTextColor.GOLD;
                else if (evadersRemainingPercentage <= 50) evadersRemainingColor = NamedTextColor.YELLOW;

                Component row4 = smallRaisedText(evadersRemaining + "/" + evadersTotal).color(evadersRemainingColor)
                        .append(smallRaisedText(" EVADERS REMAINING - ").color(NamedTextColor.WHITE));

                // Add text showing what eliminations are worth
                row4 = row4.append(smallRaisedText("ELIMINATIONS WORTH ").color(NamedTextColor.WHITE));

                if (taggers != null) {
                    NamedTextColor taggersColor = game.getTaggers().getColor();
                    row4 = row4.append(smallRaisedText(game.getCurrentEvaderKillValue() + " POINTS").color(taggersColor));
                }

                // Set the fourth row to the evaders remaining display
                setServerText(3, row4);
            }
        }
        else {
            CBCTeam winningTeam = game.getWinner();
            setServerText(3, smallRaisedText(winningTeam.getTeamName() + " WINS!").color(winningTeam.getColor()));
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<TagTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (TagTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");
            Collection<TagPlayer> players = team.getInGamePlayers();

            boolean isTagger = team.isTeamTaggers();

            for (TagPlayer player : players) {

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

                // Check if player is a tagger
                if (isTagger) {
                    // Add icon to show they are a tagger
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
