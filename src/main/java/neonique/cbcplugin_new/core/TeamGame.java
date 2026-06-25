package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.*;

public abstract class TeamGame<P extends CBCPlayer, M extends CBCMap, T extends CBCTeam<P>> extends Game<P, M> {

    // If teams should be restored after this game
    private boolean restoreTeamsAfterGame = true;
    private final LinkedHashMap<String, T> teams = new LinkedHashMap<>();

    private T winningTeam = null;

    public TeamGame(GameManager gameManager) {
        super(gameManager);
    }

    /**
     * Creates a CBCTeam object for the gamemode.
     * @param team The LobbyTeam to create the team from.
     * @param teamNum The number that is assigned to the team.
     */
    public abstract T createGamemodeTeam (LobbyTeam team, int teamNum);

    public T getTypedTeam (CBCTeam<?> team) {
        if (team == null) return null;
        return teams.get(team.getTeamId());
    }

    public void addPlayerToTeam (CBCPlayer player, CBCTeam<?> team) {
        P gamemodePlayer = getTypedPlayer(player);
        T gamemodeTeam = getTypedTeam(team);
        gamemodeTeam.addPlayer(gamemodePlayer);
    }

    public void removePlayerFromTeam (CBCPlayer player, CBCTeam<?> team) {
        P gamemodePlayer = getTypedPlayer(player);
        T gamemodeTeam = getTypedTeam(team);
        gamemodeTeam.removePlayer(gamemodePlayer);
    }

    @Override
    public void setupDefaultGameVars (Map<String, Boolean> boolVars, Map<String, Integer> intVars, Map<String, String> stringVars) {
        super.setupDefaultGameVars(boolVars, intVars, stringVars);
        restoreTeamsAfterGame = boolVars.getOrDefault("restoreTeamsAfterGame", true);
    }

    /**
     * Creates the game's teams.
     * @param lobbyTeams A list of all the teams registered in the lobby
     */
    public void createTeams (LinkedHashMap<String, LobbyTeam> lobbyTeams) {

        int teamNum = 1;

        // Go through each lobby team and add its team information to a team
        for (String teamId : lobbyTeams.keySet()) {
            LobbyTeam lobbyTeam = lobbyTeams.get(teamId);

            // Only create the team if the team has players
            if (lobbyTeam.getOnlinePlayers().isEmpty()) continue;
            Set<LobbyPlayer> teamOnlinePlayers = lobbyTeam.getOnlinePlayers();

            // Create team
            T team = createGamemodeTeam(lobbyTeam, teamNum);

            for (LobbyPlayer onlinePlayer : teamOnlinePlayers) {
                // Create player and add them to the created team
                P player = createPlayer(onlinePlayer.getPlayer());
                addPlayer(player);
                team.addPlayer(player);
            }

            // Add team to team list
            teams.put(teamId, team);
            teamNum++;
        }
    }

    @Override
    public void resetGame () {

        super.resetGame();

        // Remove all teams
        for (T team : teams.values()) {
            team.removeTeam();
        }

    }

    public void gameWon (T team) {

        final GameManager gameManager = getGameManager();
        winningTeam = team;

        // Display title of game win
        Component titleToDisplay = Component.text(team.getTeamName().toUpperCase() + " WINS!")
                .decorate(TextDecoration.BOLD).color(team.getColor());

        gameManager.sendGlobalTitle(Title.title(titleToDisplay, Component.space(),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

        // Send message of game win
        gameManager.sendGlobalMessage(
                Component.newline()
                        .append(Component.text("GAME WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(team.getTeamName()).decorate(TextDecoration.BOLD).color(team.getColor()))
                        .append(Component.text(" has won the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Play sound to all players
        gameManager.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 100, 1);

        // Set all alive players to immune
        getCombatManager().setAllPlayersImmune(true);
        getCombatManager().setVoidKill(false);

        // Play fireworks
        playVictoryFireworks(team);
        updateServerSidebar();
        updateBossbarManager();

    }

    public T getWinner () {
        return winningTeam;
    }

    public HashMap<UUID, String> getPlayersTeamIds () {
        HashMap<UUID, String> playersTeamIds = new HashMap<>();
        for (T team : teams.values()) {
            String teamId = team.getTeamId();
            for (CBCPlayer player : team.getPlayers()) {
                playersTeamIds.put(player.getOfflinePlayer().getUniqueId(), teamId);
            }
        }
        return playersTeamIds;
    }

    public List<T> getTeams () {
        return new ArrayList<>(teams.values());
    }

    public T getPlayerTeam (P player) {
        if (player == null) return null;
        if (player.getTeam() == null) return null;
        return getTypedTeam(player.getTeam());
    }

    public LinkedHashMap<String, T> getGeneralTeamList () {
        return teams;
    }

    public boolean isRestoreTeamsAfterGame() {
        return restoreTeamsAfterGame;
    }
}
