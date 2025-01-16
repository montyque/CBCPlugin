package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public abstract class TeamGame extends Game {

    // If teams should be restored after this game
    private boolean restoreTeamsAfterGame = true;

    protected LinkedHashMap<String, CBCTeam> generalTeamList = new LinkedHashMap<>();

    private CBCTeam winningTeam = null;

    public TeamGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupDefaultGameVars (HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {
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
            CBCTeam team = createGamemodeTeam(lobbyTeam, teamNum);

            for (LobbyPlayer onlinePlayer : teamOnlinePlayers) {
                // Create player and add them to the created team
                CBCPlayer player = createPlayer(onlinePlayer.getPlayer());
                team.addPlayer(player);
            }

            // Add team to team list
            generalTeamList.put(teamId, team);
            teamNum++;
        }
    }

    /**
     * Creates a CBCTeam object for the gamemode.
     * @param team The LobbyTeam to create the team from.
     * @param teamNum The number that is assigned to the team.
     */
    public abstract CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum);

    @Override
    public void resetGame () {
        super.resetGame();

        // Remove all teams
        for (CBCTeam team : generalTeamList.values()) {
            team.removeTeam();
        }
    }

    public void gameWon (CBCTeam team) {

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
        for (CBCPlayer player : getPlayers().values()) {
            if (player.isAlive()) {
                player.setImmune(true);
            }
        }

        // Play fireworks
        playVictoryFireworks(team);
        updateServerSidebar();
        updateBossbarManager();

    }

    public CBCTeam getWinner () {
        return winningTeam;
    }

    public HashMap<UUID, String> getPlayersTeamIds () {

        HashMap<UUID, String> playersTeamIds = new HashMap<>();

        for (CBCTeam team : generalTeamList.values()) {
            String teamId = team.getTeamId();
            for (CBCPlayer player : team.getPlayers()) {
                playersTeamIds.put(player.getOfflinePlayer().getUniqueId(), teamId);
            }
        }

        return playersTeamIds;

    }

    public LinkedHashMap<String, CBCTeam> getGeneralTeamList() {
        return generalTeamList;
    }

    public boolean isRestoreTeamsAfterGame() {
        return restoreTeamsAfterGame;
    }
}
