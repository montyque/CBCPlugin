package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.StringUtil.checkPlural;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public class CTFBossbarManager extends GameBossBarManager {

    private final CTFGame game;

    public CTFBossbarManager(CTFGame game) {

        // Create rows
        super(4);

        this.game = game;

    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Add flag information
        // Go through each team and each player in every team
        if (game.getWinner() == null) {
            for (CTFTeam team : game.getTeams()) {

                int alive = team.alivePlayers().size();
                Component flagComponent = getTeamStatus(team);

                for (CTFPlayer player : team.players()) {
                    if (!player.isOnline()) continue;
                    Player playerEntity = player.getPlayer();
                    if (player.isEliminated()) {
                        if (team.isTeamEliminated()) {
                            setClientText(playerEntity, 2, smallText("TEAM ELIMINATED").color(NamedTextColor.RED));
                        } else {
                            setClientText(playerEntity, 2, smallText("ELIMINATED - ").color(NamedTextColor.RED).append(
                                    smallText(checkPlural("%s PLAYER%s LEFT", alive).toUpperCase()).color(team.textColor())));
                        }
                    } else {
                        setClientText(playerEntity, 2, flagComponent);
                    }
                }
            }

        } else {
            CTFTeam winner = game.getWinner();
            for (CTFPlayer player : game.getPlayers()) {
                if (player.isOnline()) {
                    setClientText(player.getPlayer(), 2, smallText(winner.name() + " WINS!").color(winner.textColor()));
                }
            }
        }

        if (game.anyFlagsLeft() && game.getWinner() == null) {

            int flagsRemovedTimer = game.getFlagsRemovedTimer();

            // Show timer
            String timerText = timerToText(flagsRemovedTimer);
            NamedTextColor textColor = NamedTextColor.AQUA;

            if (flagsRemovedTimer < 60) {
                textColor = NamedTextColor.RED;
            } else if (flagsRemovedTimer < 180) {
                textColor = NamedTextColor.GOLD;
            } else if (flagsRemovedTimer < 300) {
                textColor = NamedTextColor.YELLOW;
            }

            setServerText(3, smallRaisedText(timerText).color(textColor).append(smallRaisedText(" UNTIL ALL TEAMS LOSE 1 FLAG").color(NamedTextColor.WHITE)));

        } else {
            if (game.isSuddenDeath()) {
                Component suddenDeathText = smallRaisedText("SUDDEN DEATH - BORDER SIZE ").color(NamedTextColor.RED)
                        .append(smallRaisedText(game.getBorderDiameter() + "x" + game.getBorderDiameter()).color(NamedTextColor.YELLOW));
                setServerText(3, suddenDeathText);
            } else {
                setServerText(3, blankComponent());
            }
        }

        updateClientBars();

    }

    public Component getTeamStatus (CTFTeam team) {

        Component flagComponent = Component.text("");
        TextColor teamColor = team.textColor();
        int flagsLeft = team.getFlagsLeft();

        if (flagsLeft > 0)
            if (!team.isFlagAtBase()) {
                CTFPlayer flagHolder = team.getFlagHolder();
                if (flagHolder != null) {
                    flagComponent = smallText("!! ").color(NamedTextColor.RED).append(
                                    smallText("FLAG STOLEN BY ").color(teamColor)
                            .append(flagHolder.getNameComponent(ResourcePackFont.SMALL_5X5))
                            .append(smallText(checkPlural(" - %s FLAG%s REMAINING", flagsLeft)
                                    .toUpperCase()).color(teamColor)));
                }
            } else {
                flagComponent = smallText(checkPlural("FLAG SAFE AT BASE - %s FLAG%s REMAINING", flagsLeft))
                        .color(team.textColor());
            }
        else {
            int alive = team.alivePlayers().size();
            if (alive > 1) {
                flagComponent = smallText("FINAL LIFE - NO FLAGS REMAINING - ").color(NamedTextColor.GOLD).append(
                        smallText(alive + " PLAYERS LEFT").color(team.textColor())
                );
            }
            else {
                flagComponent = smallText("FINAL LIFE - NO FLAGS REMAINING - ").color(NamedTextColor.GOLD).append(
                        smallText("LAST PLAYER ALIVE").color(team.textColor())
                );
            }
        }

        return flagComponent;

    }

    public Component createPlayerDisplayComponent () {
        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<CTFTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (CTFTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<CTFPlayer> players = team.players();

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
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isOnline(), team.textColor());
                }
                else {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), team.textColor());
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
