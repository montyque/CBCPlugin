package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
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

public class MapRushTDMBossbarManager extends TDMBossbarManager {

    private final MapRushTDMGame game;

    public MapRushTDMBossbarManager (MapRushTDMGame game) {
        super(game);
        this.game = game;
    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Get score component
        Component finalScore = smallText("").color(NamedTextColor.WHITE);
        List<TDMTeam> teamsByScore = new ArrayList<>(game.getTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TDMTeam team : teamsByScore) {
            finalScore = finalScore.append(smallText(String.valueOf(team.getKills())).color(team.getColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(smallText("-").color(NamedTextColor.WHITE));
            }
        }

        // Add map information
        Component mapInfo = finalScore.append(smallText(" \uE001 ").color(NamedTextColor.WHITE));

        mapInfo = mapInfo.append(smallText(game.getMap().getMapName() + ": MAP "));
        mapInfo = mapInfo.append(smallText(game.getMapNumber() + "/" + game.getMapsMaximum()));

        Component gameInfo = mapInfo.append(smallText(" \uE001 ").color(NamedTextColor.WHITE));

        // Show timer
        if (game.getWinner() != null) {
            CBCTeam winningTeam = game.getWinner();
            gameInfo = gameInfo.append(smallText(winningTeam.getTeamName() + " WINS!").color(winningTeam.getColor()));
        }
        else {
            if (game.isOvertime()) {
                gameInfo = gameInfo.append(smallText("OVERTIME: FIRST TO " + game.getOvertimeKillsToWin() + " WINS").color(NamedTextColor.RED));
            }
            else {
                if (game.getMapTimer() < 10) {
                    gameInfo = gameInfo.append(smallText(game.mapTimerToText()).color(NamedTextColor.RED));
                }
                else if (game.getMapTimer() < 30) {
                    gameInfo = gameInfo.append(smallText(game.mapTimerToText()).color(NamedTextColor.YELLOW));
                }
                else {
                    gameInfo = gameInfo.append(smallText(game.mapTimerToText()).color(NamedTextColor.AQUA));
                }
            }
        }

        setServerText(2, gameInfo);

        // Go through each team and each player in every team
        for (TDMTeam team : teamsByScore) {

            Component placeComponent;
            if (team.isTied()) {
                placeComponent = smallRaisedText("TIED FOR ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.getColor()));
            }
            else {
                placeComponent = smallRaisedText("CURRENTLY IN ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.getColor()));
            }

            for (CBCPlayer player : team.getPlayers()) {
                if (player.isOnline()) {
                    Player playerEntity = player.getPlayer();
                    setClientText(playerEntity, 3, placeComponent);
                }
            }
        }

        updateClientBars();

    }
}
