package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.core.*;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class LobbyGameSelector {

    private final MapRepository mapRepository;
    private final Audience audience;

    private CBCGamemode gamemodeSelected = null;
    private GamemodeMapData mapSelected = null;
    private GameSettings settings = GameSettings.blank();

    public LobbyGameSelector (MapRepository repository, Audience audience) {
        this.mapRepository = repository;
        this.audience = audience;
    }

    public void setGameSelection (CBCGamemode gamemode, GamemodeMapData map) {

        // Check if gamemode has changed
        if (gamemodeSelected != gamemode) {
            settings = gamemode.defaultGameSettings();
        }

        gamemodeSelected = gamemode;
        mapSelected = map;

        // Send message to say what map has been selected
        audience.sendMessage(Component.text()
                .append(Component.text()
                        .content(gamemodeSelected.getGamemodeName() + " - " + mapSelected.mapData().name())
                        .decorate(TextDecoration.BOLD))
                .append(Component.text()
                        .content(" has been selected!"))
                .color(NamedTextColor.GREEN)
                .build());

    }

    public GameContext getGameContext (List<LobbyTeam> teams, List<LobbyPlayer> players) {

        if (gamemodeSelected == null) return null;
        if (gamemodeSelected.isTeamGamemode()) {
            return new TeamGameContext((TeamMapData) mapSelected, teams, settings);
        } else {
            return new FFAGameContext(mapSelected, players, settings);
        }

    }

    public void validateGameStart (List<LobbyTeam> teams, List<LobbyPlayer> players) {

        if (gamemodeSelected == null) throw new IllegalGameConditionsException("Gamemode has not been selected");
        if (mapSelected == null) throw new IllegalGameConditionsException("Map has not been selected");

        if (gamemodeSelected.isTeamGamemode()) {

            TeamMapData teamMap = (TeamMapData) mapSelected;

            // Check team amount is within boundaries
            if (teams.size() < teamMap.minTeams()) {
                throw new IllegalGameConditionsException("Not enough teams, at least " + teamMap.minTeams() + " required");
            } else if (teams.size() > teamMap.maxTeams()) {
                throw new IllegalGameConditionsException("Too many teams, limit of " + teamMap.maxTeams() + " required");
            }

            // Check all team colors are allowed
            List<TeamColor> colorsAllowed = teamMap.validTeamColors();
            teams.forEach(t -> {
                if (!colorsAllowed.contains(t.teamColor()))
                    throw new IllegalGameConditionsException("Team color " + t.teamColor() + " is not allowed on the selected map");
            });

        } else {

            // TODO: Check player count is within boundaries

        }

    }

    public CBCGamemode gamemodeSelected () {
        return gamemodeSelected;
    }


}
