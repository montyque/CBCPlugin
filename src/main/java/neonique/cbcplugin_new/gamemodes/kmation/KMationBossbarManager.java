package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class KMationBossbarManager extends GameBossBarManager {

    private final KMationGame game;

    public KMationBossbarManager (KMationGame game) {
        super(4);
        this.game = game;
    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Show cycle information
        Component cycleInfo;
        // If there's a winner then set bossbar
        if (game.getWinner() != null) {
            CBCPlayer winningPlayer = game.getWinner();
            cycleInfo = smallText(winningPlayer.getName().toUpperCase() + " WINS!")
                    .color(NamedTextColor.GREEN);
        }
        // Display timer
        else {
            // Get cycle number and display it
            cycleInfo = smallText("CYCLE " + game.getCycleNumber()).color(NamedTextColor.AQUA);
            cycleInfo = cycleInfo.append(smallText(" - ").color(NamedTextColor.AQUA));
            // Check if this is final cycle
            if (game.isFinalCycle()) {
                cycleInfo = cycleInfo.append(smallText("FINAL CYCLE").color(NamedTextColor.GREEN));
                cycleInfo = cycleInfo.append(smallText(" - ").color(NamedTextColor.WHITE));
            }
            // Get timer and display it
            // This also changes the color of the bossbar
            if (game.isOvertime()) {
                cycleInfo = cycleInfo.append(
                        Component.text("OVERTIME - FIRST TO " + game.getOvertimeThreshold()).color(NamedTextColor.RED));
            } else {
                if (game.getCycleTimer() <= 15) {
                    cycleInfo = cycleInfo.append(
                            Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                    Component.text(game.timerToText()).color(NamedTextColor.RED)));
                } else if (game.getCycleTimer() <= 30) {
                    cycleInfo = cycleInfo.append(
                            Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                    Component.text(game.timerToText()).color(NamedTextColor.YELLOW)));
                } else {
                    cycleInfo = cycleInfo.append(
                            Component.text("Time left: ").color(NamedTextColor.WHITE).append(
                                    Component.text(game.timerToText()).color(NamedTextColor.AQUA)));
                }
            }
        }

        setServerText(2, cycleInfo);

        // Go through each team and each player in every team
        for (KMationPlayer player : game.getPlayers()) {

            if (!player.isOnline()) continue;
            Component placeComponent;

            NamedTextColor accentColor = NamedTextColor.GREEN;

            if (player.isEliminated()) {
                accentColor = NamedTextColor.RED;
                placeComponent = smallRaisedText("ELIMINATED - ").color(accentColor);
            }
            else if (player.isInDanger()) {
                accentColor = NamedTextColor.YELLOW;
                placeComponent = smallRaisedText("CURRENTLY IN DANGER - ").color(accentColor);
            }
            else {
                placeComponent = smallRaisedText("CURRENTLY SAFE - ").color(accentColor);
            }

            if (player.isTied()) {
                placeComponent = placeComponent.append(smallRaisedText("OVERALL TIED FOR ").color(NamedTextColor.WHITE))
                        .append(smallRaisedText(StringUtil.getPlacementString(player.getPlacement())).color(accentColor));
            }
            else {
                placeComponent = placeComponent.append(smallRaisedText("OVERALL PLACEMENT ").color(NamedTextColor.WHITE))
                        .append(smallRaisedText(StringUtil.getPlacementString(player.getPlacement())).color(accentColor));
            }

            Player playerEntity = player.getPlayer();
            setClientText(playerEntity, 3, placeComponent);
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";

        // Teams
        Collection<KMationPlayer> players = game.getKMationPlayersInGame();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");

        for (CBCPlayer player : players) {

            i++;

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

            playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), null);

            playerDisplayComponent = playerDisplayComponent.append(playerComponent);
            // Add a space in between each player
            if (i != players.size()) {
                playerDisplayComponent = playerDisplayComponent.append(Component.text(playerSeparator).color(NamedTextColor.WHITE));
            }
        }

        return playerDisplayComponent;
    }

}
