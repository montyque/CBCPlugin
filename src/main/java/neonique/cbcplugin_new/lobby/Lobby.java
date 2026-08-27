package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.core.*;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.lobby.tasks.StartCountdownTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Lobby implements ForwardingAudience {

    private final Plugin plugin;
    private final World world;
    private final CBCScoreboardManager scoreboardManager;
    private final MapRepository mapRepository;

    private Consumer<GameContext> gameStarter;

    boolean active = false;

    // Lobby variables
    private Location lobbyTeleport;

    // Map selected
    private LobbyGameSelector gameSelector;

    // If the game is starting
    private boolean gameStarting;
    private StartCountdownTask startingCountdown;

    // Players and teams
    private Map<UUID, LobbyPlayer> players = new HashMap<>();
    private Map<TeamColor, LobbyTeam> teams = new LinkedHashMap<>();
    private CBCScoreboardTeam spectatorTeam;
    private CBCScoreboardTeam ffaTeam;

    public Lobby (Plugin plugin,
                  World world,
                  CBCScoreboardManager scoreboardManager,
                  MapRepository repository) {

        this.plugin = plugin;
        this.world = world;
        this.scoreboardManager = scoreboardManager;
        this.mapRepository = repository;

    }

    public void activate (Consumer<GameContext> gameStarter) {

        if (active) return;
        active = true;

        this.gameStarter = gameStarter;
        this.gameSelector = new LobbyGameSelector(mapRepository, this);

        // Reset variables
        gameStarting = false;

        // Set world spawn
        world.setSpawnLocation(lobbyTeleport);

        // Create teams
        teamSetup();
        players.clear();

        // Create lobby players for all online players
        for (Player player : world.getPlayers()) {
            newPlayer(player);
        }

    }

    private void teamSetup () {

        teams.clear();
        teams = createTeams();
        for (LobbyTeam team : teams.values()) {
            team.registerTeam(scoreboardManager);
        }

        // Creating spectator and FFA scoreboard teams
        ffaTeam = scoreboardManager.registerNewTeam("09ffaLobby");
        ffaTeam.setFriendlyFireEnabled(true);
        ffaTeam.setPrefix(Component.text(" ■ ").color(NamedTextColor.WHITE));

        spectatorTeam = scoreboardManager.registerNewTeam("10spectatorLobby");
        spectatorTeam.setFriendlyFireEnabled(true);
        spectatorTeam.setPrefix(Component.text(" □ ").color(NamedTextColor.WHITE));

    }

    private Map<TeamColor, LobbyTeam> createTeams () {
        return Map.of(
                TeamColor.RED, new LobbyTeam("01", "red", "Red", "R", TeamColor.RED),
                TeamColor.BLUE, new LobbyTeam("02", "blue", "Blue", "B", TeamColor.BLUE),
                TeamColor.GREEN, new LobbyTeam( "03", "green", "Green", "G", TeamColor.GREEN),
                TeamColor.YELLOW, new LobbyTeam("04", "yellow", "Yellow", "Y", TeamColor.YELLOW),
                TeamColor.CYAN, new LobbyTeam("05", "cyan", "Cyan", "C", TeamColor.CYAN),
                TeamColor.ORANGE, new LobbyTeam("06", "orange", "Orange", "O", TeamColor.ORANGE),
                TeamColor.MAGENTA, new LobbyTeam("07", "magenta", "Magenta", "M", TeamColor.MAGENTA),
                TeamColor.PURPLE, new LobbyTeam("08", "purple", "Purple", "P", TeamColor.PURPLE)
        );
    }

    public void startGame() {
        gameStarter.accept(gameSelector.getGameContext(
                getTeamsWithOnlinePlayers(),
                List.copyOf(players.values())));
    }

    public void deactivate() {

        active = false;

        for (LobbyTeam lobbyTeam : teams.values()) {
            lobbyTeam.removeTeam();
        }
        scoreboardManager.unregisterTeam(ffaTeam);
        scoreboardManager.unregisterTeam(spectatorTeam);
        ffaTeam = null;
        spectatorTeam = null;

        players.clear();
        teams.clear();

    }

    public LobbyGameSelector gameSelector () {
        return gameSelector;
    }

    public void randomizePlayersIntoTeams (Collection<LobbyPlayer> players,
                                           Collection<LobbyTeam> teams) {

        List<LobbyPlayer> playersShuffled = new ArrayList<>(players);
        Collections.shuffle(playersShuffled);
        List<LobbyTeam> teamsShuffled = new ArrayList<>(teams);
        Collections.shuffle(teamsShuffled);

        for (int p = 0; p < playersShuffled.size(); p++) {
            LobbyPlayer player = playersShuffled.get(p);
            LobbyTeam team = teamsShuffled.get(p % teamsShuffled.size());
            playerJoinTeam(player, team, false);
        }

    }

    public void randomizeTeams(Set<LobbyTeam> teamsSelected) {

        List<LobbyPlayer> playersToRandomize = players.values().stream()
                .filter(p -> !p.isSpectator())
                .filter(LobbyPlayer::isOnline)
                .toList();

        randomizePlayersIntoTeams(playersToRandomize, teamsSelected);

    }

    public void newPlayer(Player player) {
        player.playerListName(null);
        LobbyPlayer newPlayer = new LobbyPlayer(player);
        players.put(player.getUniqueId(), newPlayer);
        ffaTeam.addEntityUUID(player.getUniqueId());
    }

    public void playerJoinTeam(LobbyPlayer player, LobbyTeam team, boolean overrideCurrentTeam) {
        if (player.getAssignedTeam() != null && !overrideCurrentTeam) return;
        playerLeaveTeam(player, false);
        team.addPlayer(player);
    }

    public void playerLeaveTeam(LobbyPlayer player, boolean spectator) {

        LobbyTeam playerTeam = player.getAssignedTeam();
        if (playerTeam != null) {
            playerTeam.removePlayer(player);
        }

        if (!spectator) {
            ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
            spectatorTeam.removeEntityUUID(player.getOfflinePlayer().getUniqueId());
        } else {
            spectatorTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
            ffaTeam.removeEntityUUID(player.getOfflinePlayer().getUniqueId());
        }

    }

    public void playerToggleSpectator (LobbyPlayer player) {
        if (player.isSpectator()) playerUnsetSpectator(player);
        else playerSetSpectator(player);
    }

    public void playerSetSpectator (LobbyPlayer player) {
        player.setSpectator(true);
        playerLeaveTeam(player, true);
        player.sendMessage(
                Component.text("You are now a spectator.").color(NamedTextColor.GOLD)
        );
    }

    public void playerUnsetSpectator (LobbyPlayer player) {
        player.setSpectator(false);
        player.sendMessage(
                Component.text("You are no longer a spectator.").color(NamedTextColor.GOLD)
        );
        ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
    }

    public Collection<LobbyTeam> getTeamsSet () {
        return teams.values();
    }

    public Map<TeamColor, LobbyTeam> getTeams () {
        return teams;
    }

    public Collection<LobbyPlayer> players() {
        return players.values();
    }

    public LobbyPlayer getPlayer(Player player) {
        return players.getOrDefault(player.getUniqueId(), null);
    }

    public List<LobbyTeam> getTeamsWithOnlinePlayers () {
        return teams.values().stream()
                .filter(l -> !l.onlinePlayers().isEmpty())
                .collect(Collectors.toList());
    }

    public boolean isGameStarting() {
        return gameStarting;
    }

    public void startGameCountdown() {

        gameStarting = true;

        if (gameSelector.gamemodeSelected().isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {

                // Make all offline players leave their teams
                if (!player.isOnline() && player.getAssignedTeam() != null) {
                    player.playerLeaveTeam();
                }

                // If a player is not in a team, set their team to spectator
                if (player.getAssignedTeam() == null && !player.isSpectator()) {
                    spectatorTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
                }

            }
        }

        // Start countdown
        startingCountdown = new StartCountdownTask(this, this::startGame);
        startingCountdown.runTaskTimer(plugin, 20, 20);

        sendMessage(
                Component.text("Game starting in ").color(NamedTextColor.GREEN)
                        .append(Component.text("15 seconds!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        );
        playSound(net.kyori.adventure.sound.Sound.sound(
                org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING,
                net.kyori.adventure.sound.Sound.Source.MASTER,
                100,
                1));

    }

    public void cancelGameCountdown (StartCountdownTask.CountdownCancelReason reason, Player cause) {

        gameStarting = false;

        // Cancel countdown task
        startingCountdown.cancelCountdown(reason, cause);

        if (gameSelector.gamemodeSelected().isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {
                if (player.getAssignedTeam() == null && !player.isSpectator()) {
                    ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
                }
            }
        }

    }

    public Location getLobbySpawn() {
        return lobbyTeleport;
    }

    public void playerJoinServer(Player entity) {

        entity.setGlowing(false);

        // Add player to lobby if not already in player list
        if (!players.containsKey(entity.getUniqueId())) {
            newPlayer(entity);
        }

        LobbyPlayer player = players.get(entity.getUniqueId());
        player.resetPlayer();
        entity.teleport(lobbyTeleport);
        entity.setRespawnLocation(lobbyTeleport, true);

    }

    public void playerLeaveServer(Player entity) {

        // Cancel countdown timer if player is in game
        LobbyPlayer lbPlayer = getPlayer(entity);
        if (isGameStarting()) {
            if (isPlaying(lbPlayer)) cancelGameCountdown(StartCountdownTask.CountdownCancelReason.DISCONNECT, entity);
        }

    }

    private boolean isPlaying (LobbyPlayer player) {
        if (player.getAssignedTeam() != null) return true;
        if (player.isSpectator()) return false;
        return gameSelector.gamemodeSelected().isTeamGamemode(); // Player must be associated with a team in a team gamemode
    }

    public boolean isActive () {
        return active;
    }

    public void clearAllTeams() {
        for (LobbyPlayer lobbyPlayer : players()) {
            playerLeaveTeam(lobbyPlayer, lobbyPlayer.isSpectator());
        }
    }

    public List<String> getLobbyTeamIds() {
        List<String> lobbyTeamIds = new ArrayList<>();
        for (LobbyTeam team : getTeamsSet()) {
            lobbyTeamIds.add(team.id());
        }
        return lobbyTeamIds;
    }

    public MapRepository mapRepository () {
        return mapRepository;
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences () {
        return players.values().stream()
                .filter(PlayerLike::isOnline)
                .map(PlayerLike::getPlayer)
                .toList();
    }

}
