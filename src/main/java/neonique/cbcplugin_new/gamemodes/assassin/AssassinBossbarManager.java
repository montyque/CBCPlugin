package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.*;

public class AssassinBossbarManager extends GameBossBarManager {

    private final AssassinGame game;

    public AssassinBossbarManager (AssassinGame game) {
        super(4);
        this.game = game;
    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Go through each team and each player in every team
        for (AssassinPlayer player : game.getPlayers()) {

            if (!player.isOnline()) continue;
            Player playerEntity = player.getPlayer();

            Component topRow = smallText("TARGET: ");

            if (game.getWinner() != null) {
                CBCPlayer winningPlayer = game.getWinner();
                topRow = smallText(winningPlayer.name().toUpperCase() + " WINS!")
                        .color(NamedTextColor.GREEN);
            }
            else {
                // Show player's current target
                AssassinPlayer target = player.getCurrentTarget();

                if (target == null) {
                    topRow = topRow.append(smallText("NONE").color(NamedTextColor.YELLOW));
                } else {
                    topRow = topRow.append(smallText(target.name()).color(NamedTextColor.AQUA));

                    // Add change target timer as display
                    int playerChangeTargetTimer = player.getTargetChangeTimer();

                    // Change color of display depending on amount of time left
                    NamedTextColor timerColor = NamedTextColor.GREEN;
                    if (playerChangeTargetTimer <= 10) timerColor = NamedTextColor.RED;
                    else if (playerChangeTargetTimer <= 20) timerColor = NamedTextColor.GOLD;
                    else if (playerChangeTargetTimer <= 30) timerColor = NamedTextColor.YELLOW;

                    topRow = topRow.append(smallText(" \uE000 TARGET CHANGING IN " + playerChangeTargetTimer).color(timerColor));
                }
            }

            setClientText(playerEntity, 2, topRow);

            // Show player's placement
            Component placeComponent;
            NamedTextColor accentColor = NamedTextColor.AQUA;

            if (player.isTied()) {
                placeComponent = smallRaisedText("TIED FOR ").color(NamedTextColor.WHITE)
                .append(smallRaisedText(StringUtil.getPlacementString(player.getPlacement())).color(accentColor));
            }
            else {
                placeComponent = smallRaisedText("CURRENTLY IN ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(player.getPlacement())).color(accentColor));
            }

            setClientText(playerEntity, 3, placeComponent);
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";

        // Teams
        Collection<AssassinPlayer> players = game.getPlayers();

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
