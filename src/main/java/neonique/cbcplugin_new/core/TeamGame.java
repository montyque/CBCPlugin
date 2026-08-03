package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes.TeamGameContext;
import neonique.cbcplugin_new.mapmechanics.VoidMechanic;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;

public abstract class TeamGame<P extends CBCPlayer, T extends CBCTeam<P>> extends Game<P> {

    // If teams should be restored after this game
    private boolean restoreTeamsAfterGame = true;
    private final Map<String, T> teams = new LinkedHashMap<>();

    private T winningTeam = null;

    public TeamGame (Plugin plugin, CBCScoreboardManager scoreboardManager, World world) {
        super(plugin, scoreboardManager, world);
    }

    /**
     * Creates a CBCTeam object for the gamemode.
     * @param team The LobbyTeam to create the team from.
     * @param teamNum The number that is assigned to the team.
     */
    public abstract T createGamemodeTeam (TeamLike team, int teamNum);

    public void setupGame (GameContext context) {
        setupGame((TeamGameContext) context);
    }

    public abstract void setupGame (TeamGameContext context);

    public T getTypedTeam (CBCTeam<?> team) {
        if (team == null) return null;
        return teams.get(team.id());
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

    public void createTeams (List<? extends TeamLike> originalTeams) {

        int teamNum = 1;

        // Go through each lobby team and add its team information to a team
        for (TeamLike originalTeam : originalTeams) {

            // Only create the team if the team has players
            Collection<? extends PlayerLike> teamOnlinePlayers = originalTeam.onlinePlayers();
            if (teamOnlinePlayers.isEmpty()) continue;

            // Create team and register on scoreboard manager
            T team = createGamemodeTeam(originalTeam, teamNum);
            team.registerTeam(scoreboardManager());

            for (PlayerLike onlinePlayer : teamOnlinePlayers) {
                P player = createPlayer(onlinePlayer.getPlayer());
                addPlayer(player);
                team.addPlayer(player);
            }

            // Add team to team list
            teams.put(originalTeam.id(), team);
            teamNum++;

        }
    }

    @Override
    public void resetGame () {
        super.resetGame();
        teams.values().forEach(CBCTeam::removeTeam);
    }

    public void gameWon (T team) {

        winningTeam = team;

        // Display title of game win
        Component titleToDisplay = Component.text(team.name().toUpperCase() + " WINS!")
                .decorate(TextDecoration.BOLD).color(team.textColor());

        showTitle(Title.title(titleToDisplay, Component.space(),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

        // Send message of game win
        sendMessage(
                Component.newline()
                        .append(Component.text("GAME WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(team.name()).decorate(TextDecoration.BOLD).color(team.textColor()))
                        .append(Component.text(" has won the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Set all alive players to immune and remove void death
        for (P plr : getPlayers()) {
            if (plr.isAlive()) plr.setImmune(true);
        }
        combatSession().mapMechanicsManager().getMechanicsOfType(VoidMechanic.class).forEach(v -> v.setKillOnVoid(false));

        // Play fireworks
        playVictoryFireworks(team);

    }

    public T getWinner () {
        return winningTeam;
    }

    public HashMap<UUID, String> getPlayersTeamIds () {
        HashMap<UUID, String> playersTeamIds = new HashMap<>();
        for (T team : teams.values()) {
            String teamId = team.id();
            for (CBCPlayer player : team.players()) {
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
        if (player.team() == null) return null;
        return getTypedTeam(player.team());
    }

    public Map<String, T> getGeneralTeamList () {
        return teams;
    }

    public boolean isRestoreTeamsAfterGame() {
        return restoreTeamsAfterGame;
    }
}
