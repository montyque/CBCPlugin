package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.cbcevents.CBCEventTeam;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gameobjects.GamemodeOptions;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.GlobalKillsManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.cbcevents.CBCEventPlayer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.normalText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class LobbySidebarManager {

    private final GameManager gameManager;
    private final Lobby lobby;

    private final World world;
    private final CBCScoreboardManager scoreboardManager;

    private HashMap<UUID, ClientSidebar> clientSidebars;

    private List<Component> displayToEveryone;

    private boolean active;

    public LobbySidebarManager (GameManager gameManager, Lobby lobby) {

        this.gameManager = gameManager;
        this.lobby = lobby;

        this.scoreboardManager = gameManager.getCbcScoreboardManager();

        this.world = gameManager.getWorld();

    }

    public void setupSidebar () {

        active = true;

        clientSidebars = new HashMap<>();


        updateServerBoard();
        // Create sidebars for all online players
        for (Player player : world.getPlayers()) {

            addPlayerSidebar(player);

        }
    }

    public void addPlayerSidebar (Player player) {

        // Get scoreboard from player
        Scoreboard playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        if (playerScoreboard == null) {
            scoreboardManager.addPlayer(player);
            playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        }

        Component sidebarTitle = smallText("CROSSBOW CHAMPIONS").color(NamedTextColor.AQUA);
        if (gameManager.isCBCEventActive()) {
            CBCEventManager eventManager = gameManager.getEventManager();
            sidebarTitle = smallText(" " + eventManager.getEventNameShorthand() + ": ").color(NamedTextColor.AQUA).append(
                    smallText(eventManager.getEventTagline() + " ").color(NamedTextColor.YELLOW)
            );
        }
        ClientSidebar playerSidebar = new ClientSidebar(player, playerScoreboard, "lobbySidebar",
                sidebarTitle, true);
        clientSidebars.put(player.getUniqueId(), playerSidebar);

        updateClientBoard(player);

    }

    public void removePlayerSidebar (Player player) {

        ClientSidebar playerSidebar = clientSidebars.getOrDefault(player.getUniqueId(), null);
        if (playerSidebar == null) return;
        playerSidebar.removeSidebar();

        clientSidebars.remove(player.getUniqueId());

    }

    public void removeSidebar () {

        clientSidebars.clear();
        active = false;

    }

    public void updateServerBoard () {

        if (gameManager.isCBCEventActive()) {
            updateServerEventBoard();
            return;
        }

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        if (lobby.getGamemodeSelected() != null) {
            GamemodeOptions gamemodeOptions = gameManager.getGamemodes().get(lobby.getGamemodeSelected());
            Component gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("GAMEMODE: ").color(NamedTextColor.YELLOW))
                    .append(smallText(gamemodeOptions.getGamemodeName()).color(NamedTextColor.GREEN));
            displayToEveryone.add(gamemodeComponent);
        } else {
            Component gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("GAMEMODE: ").color(NamedTextColor.YELLOW))
                    .append(smallText("NOT SELECTED").color(NamedTextColor.GREEN));
            displayToEveryone.add(gamemodeComponent);
        }

        Component gamemodeComponent;
        if (lobby.getMapSelected() == null) {
            gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("MAP: ").color(NamedTextColor.YELLOW))
                    .append(smallText("NOT SELECTED").color(NamedTextColor.GREEN));
        } else {
            gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("MAP: ").color(NamedTextColor.YELLOW))
                    .append(smallText(lobby.getMapSelected().getMapName()).color(NamedTextColor.GREEN));
        }
        displayToEveryone.add(gamemodeComponent);

        displayToEveryone.add(blankComponent());

        updateAllClientBoards();
    }

    public Component getGameOrderComponent (int num) {

        if (!gameManager.isCBCEventActive()) {
            return blankComponent();
        }

        CBCEventManager eventManager = gameManager.getEventManager();
        int nextGameNum = eventManager.getNextGameNum();

        Component gameComponent = Component.text("");

        // Set colors
        NamedTextColor numberColor = NamedTextColor.YELLOW;
        NamedTextColor gamemodeColor = NamedTextColor.GREEN;

        CBCGamemode gamemode = eventManager.getGamemode(num);
        // Default icon
        String gamemodeIcon = gamemode.getUnicodeIcon(NamedTextColor.WHITE);

        // Add triangle
        if (num == nextGameNum) {
            gameComponent = gameComponent.append(Component.text("\uE880 ").color(NamedTextColor.YELLOW));
            gamemodeColor = NamedTextColor.YELLOW;
        }
        else {

            CBCEventTeam gameWinner = eventManager.getGameWinner(num);
            NamedTextColor gameWinnerColor = NamedTextColor.WHITE;
            if (gameWinner != null) {
                gameWinnerColor = gameWinner.getTeamColor();
            }

            gameComponent = gameComponent.append(Component.text("\uF824 ").color(NamedTextColor.WHITE));
            if (num < nextGameNum) {
                if (eventManager.getEventWinner() == null) {
                    numberColor = NamedTextColor.GRAY;
                    gamemodeColor = NamedTextColor.GRAY;
                }
                else {
                    gamemodeColor = gameWinnerColor;
                }
            }

            // Set game winner to icon
            gamemodeIcon = gamemode.getUnicodeIcon(gameWinnerColor);

        }

        if (num == CBCEventManager.getGameAmount() + 1) {
            numberColor = NamedTextColor.GOLD;
            if (eventManager.getEventWinner() != null) {
                gamemodeColor = NamedTextColor.GOLD;
            }
            gameComponent = gameComponent.append(Component.text("F.").color(numberColor));
        }
        else {
            gameComponent = gameComponent.append(Component.text(num + ".").color(numberColor));
        }

        // Add gamemode icon
        gameComponent = gameComponent.append(Component.text(" " + gamemodeIcon + " ").color(NamedTextColor.WHITE));

        // Add gamemode name
        gameComponent = gameComponent.append(Component.text(gamemode.getGamemodeName()).color(gamemodeColor));

        return gameComponent;
    }

    public void updateServerEventBoard () {

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        if (lobby.getGamemodeSelected() != null) {
            GamemodeOptions gamemodeOptions = gameManager.getGamemodes().get(lobby.getGamemodeSelected());
            Component gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("NEXT: ").color(NamedTextColor.YELLOW))
                    .append(smallText(gamemodeOptions.getGamemodeName()).color(NamedTextColor.GREEN));
            displayToEveryone.add(gamemodeComponent);
        } else {
            Component gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("NEXT: ").color(NamedTextColor.YELLOW))
                    .append(smallText("NOT SELECTED").color(NamedTextColor.GREEN));
            displayToEveryone.add(gamemodeComponent);
        }

        Component gamemodeComponent;
        if (lobby.getMapSelected() == null) {
            gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("MAP: ").color(NamedTextColor.YELLOW))
                    .append(smallText("NOT SELECTED").color(NamedTextColor.GREEN));
        } else {
            gamemodeComponent = normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("MAP: ").color(NamedTextColor.YELLOW))
                    .append(smallText(lobby.getMapSelected().getMapName()).color(NamedTextColor.GREEN));
        }

        displayToEveryone.add(gamemodeComponent);
        displayToEveryone.add(blankComponent());

        // Display game order
        displayToEveryone.add(normalText(TextUtil.getSpaceOfLength(8)).append(smallText("GAME ORDER:").color(NamedTextColor.AQUA)));

        for (int i = 1; i <= CBCEventManager.getGameAmount(); i++) {
            displayToEveryone.add(normalText(TextUtil.getSpaceOfLength(8)).append(getGameOrderComponent(i)));
        }

        // Add final if required
        CBCEventManager eventManager = gameManager.getEventManager();
        int nextGameNum = eventManager.getNextGameNum();
        if (nextGameNum == CBCEventManager.getGameAmount() + 1 && eventManager.getEventWinner() == null) {
            displayToEveryone.add(normalText(TextUtil.getSpaceOfLength(8)).append(getGameOrderComponent(CBCEventManager.getGameAmount() + 1)));
        }
        else if (nextGameNum >= CBCEventManager.getGameAmount() + 1 && eventManager.getGameWinner(CBCEventManager.getGameAmount() + 1) != null) {
            displayToEveryone.add(normalText(TextUtil.getSpaceOfLength(8)).append(getGameOrderComponent(CBCEventManager.getGameAmount() + 1)));
        }


        displayToEveryone.add(blankComponent());

        updateAllClientBoards();

    }

    public void updateAllClientBoards () {
        for (UUID uuid : clientSidebars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            updateClientBoard(player);
        }
    }

    public void updateClientBoard (Player player) {

        if (gameManager.isCBCEventActive()) {
            updateEventClientBoard(player);
            return;
        }

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Display current team
        if (lobby.getLobbyPlayer(player) != null) {
            LobbyPlayer lobbyPlayer = lobby.getLobbyPlayer(player);
            if (lobbyPlayer.getAssignedTeam() != null) {
                LobbyTeam playerTeam = lobbyPlayer.getAssignedTeam();
                clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                        .append(smallText("TEAM: ").color(NamedTextColor.YELLOW))
                        .append(smallText("■ " + playerTeam.getTeamName()).color(playerTeam.getColor())));
            } else {
                if (lobbyPlayer.isSpectator()) {
                    clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                            .append(smallText("TEAM: ").color(NamedTextColor.YELLOW))
                            .append(smallText("□ Spectator").color(NamedTextColor.WHITE)));
                } else {
                    clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                            .append(smallText("TEAM: ").color(NamedTextColor.YELLOW))
                            .append(smallText("■ None").color(NamedTextColor.WHITE)));
                }
            }
        } else {
            clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                    .append(smallText("TEAM: ").color(NamedTextColor.YELLOW))
                    .append(smallText("■ None").color(NamedTextColor.WHITE)));
        }

        // Display global kills
        GlobalKillsManager globalKillsManager = gameManager.getGlobalKillsManager();
        int playerScoreValue = globalKillsManager.getPlayerKills(player);
        clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                .append(smallText("GLOBAL KILLS: ").color(NamedTextColor.YELLOW))
                .append(smallText(String.valueOf(playerScoreValue)).color(NamedTextColor.GREEN)));

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);
    }

    public void updateEventClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);
        CBCEventManager eventManager = gameManager.getEventManager();

        if (eventManager.hasPlayer(player.getUniqueId())) {
            CBCEventPlayer eventPlayer = eventManager.getPlayer(player.getUniqueId());
            if (eventPlayer != null) {

                // Display event score
                clientStringList.add(normalText(TextUtil.getSpaceOfLength(8)).append(
                        Component.text(eventManager.getEventNameShorthand() + " Score:").color(NamedTextColor.AQUA)
                ).append(
                        eventManager.getPointsIconComponent()
                ).append(
                        Component.text(eventPlayer.getEventScore()).color(NamedTextColor.YELLOW)
                ));

                // Display CBC event team
                if (eventPlayer.getTeam() != null) {
                    NamedTextColor teamColor = eventPlayer.getTeamColor();
                    String teamName = eventPlayer.getTeam().getTeamName();
                    clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                            .append(smallText(eventManager.getEventNameShorthand() + " TEAM: ").color(NamedTextColor.YELLOW))
                            .append(smallText("■ " + teamName).color(teamColor)));
                } else {
                    clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                            .append(smallText(eventManager.getEventNameShorthand() + " TEAM: ").color(NamedTextColor.YELLOW))
                            .append(smallText("■ None").color(NamedTextColor.WHITE)));
                }
            }
        }

        // Display global kills
        GlobalKillsManager globalKillsManager = gameManager.getGlobalKillsManager();
        int playerScoreValue = globalKillsManager.getPlayerKills(player);
        clientStringList.add(normalText(TextUtil.getSpaceOfLength(8))
                .append(smallText("GLOBAL KILLS: ").color(NamedTextColor.YELLOW))
                .append(smallText(String.valueOf(playerScoreValue)).color(NamedTextColor.GREEN)));

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);
    }

    public boolean isActive() {
        return active;
    }
}
