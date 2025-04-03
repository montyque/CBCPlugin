package neonique.cbcplugin_new.cbcevents;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.enums.ResourcePackFont;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.gamemodes._base.TeamGame;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.tasks.FireworkTask;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.*;
import static neonique.cbcplugin_new.util.TextUtil.*;

public class CBCEventManager {

    private final GameManager gameManager;
    private final Lobby lobby;

    private boolean active;

    // Players and teams
    private final HashMap<UUID, CBCEventPlayer> players = new HashMap<>();
    private final List<CBCEventTeam> teams = new ArrayList<>();

    // The winner of the event
    private CBCEventTeam eventWinner = null;

    // Game status - game 4 is finale
    private int nextGameNum = 1;

    // Display variables
    private final String eventName = "The Crossbow Championship 13";
    private final String eventNameShorthand = "CBC 13";
    private final String eventTagline = "THE EXPERIMENT";
    private final String eventTime = "March 9 2025 00:00 UTC";

    private final char eventLogoUnicode = 0xE900;

    // Game end
    private boolean isCurrentGameEventGame = false;

    private final Location lobbyTeleportLocation;
    private final Location winnerTeleportLocation;

    // Store gamemodes
    private final List<CBCGamemode> gamemodeList = new ArrayList<>();
    private final List<String> mapNameList = new ArrayList<>();

    // Store the games and their information
    private final List<TeamGame> games = new ArrayList<>();
    private final List<CBCEventTeam> winners = new ArrayList<>();
    private final List<PostGameStats> postGameStats = new ArrayList<>();

    // Store the points scored
    private final List<LinkedHashMap<CBCEventPlayer, Integer>> gameScores = new ArrayList<>();
    private final List<LinkedHashMap<CBCEventPlayer, Integer>> eventScoresForGames = new ArrayList<>();

    // Holograms
    private final List<Location> hologramLocations = new ArrayList<>();
    private final List<UUID> hologramUUIDs = new ArrayList<>();

    // Beacon blocks and locations
    private final HashMap<NamedTextColor, Location> beaconGlassBlocksLocs = new HashMap<>();
    private final HashMap<NamedTextColor, Material> beaconAltGlassBlock = new HashMap<>();
    private final HashMap<NamedTextColor, Material> beaconGlassBlock = new HashMap<>();

    public CBCEventManager (GameManager gameManager) {

        final World world = gameManager.getWorld();

        this.gameManager = gameManager;
        this.lobby = gameManager.getLobby();

        lobbyTeleportLocation = new Location(world, -1051.00, 124.00, -1654.00);
        winnerTeleportLocation = new Location(world, -1051.00, 126.00, -1644.00);

        // Set hologram locations
        hologramLocations.add(new Location(world, -1051.00, 126.20, -1649.00));
        hologramLocations.add(new Location(world, -1051.00, 125.60, -1649.00));
        hologramLocations.add(new Location(world, -1051.00, 125.20, -1649.00));
        hologramLocations.add(new Location(world, -1051.00, 124.80, -1649.00));

        // Create holograms
        recreateHolograms();

        beaconGlassBlocksLocs.put(NamedTextColor.RED, new Location(world, -1044, 120, -1640));
        beaconGlassBlocksLocs.put(NamedTextColor.BLUE, new Location(world, -1048, 120, -1638));
        beaconGlassBlocksLocs.put(NamedTextColor.GREEN, new Location(world, -1055, 120, -1638));
        beaconGlassBlocksLocs.put(NamedTextColor.YELLOW, new Location(world, -1059, 120, -1640));

        // Beacon information
        beaconGlassBlock.put(NamedTextColor.RED, Material.RED_STAINED_GLASS);
        beaconGlassBlock.put(NamedTextColor.BLUE, Material.BLUE_STAINED_GLASS);
        beaconGlassBlock.put(NamedTextColor.GREEN, Material.LIME_STAINED_GLASS);
        beaconGlassBlock.put(NamedTextColor.YELLOW, Material.YELLOW_STAINED_GLASS);

        beaconAltGlassBlock.put(NamedTextColor.RED, Material.PINK_STAINED_GLASS);
        beaconAltGlassBlock.put(NamedTextColor.BLUE, Material.LIGHT_BLUE_STAINED_GLASS);
        beaconAltGlassBlock.put(NamedTextColor.GREEN, Material.YELLOW_STAINED_GLASS);
        beaconAltGlassBlock.put(NamedTextColor.YELLOW, Material.RED_STAINED_GLASS);

        // Game 1
        gamemodeList.add(CBCGamemode.CBCTAG);
        mapNameList.add("Marbury Mansion");

        // Game 2
        gamemodeList.add(CBCGamemode.KOTH);
        mapNameList.add("Volcanic Rift");

        // Game 3
        gamemodeList.add(CBCGamemode.RENDEZVOUS);
        mapNameList.add("Garden of Eeshol");

        // Final -- BY DEFAULT
        gamemodeList.add(CBCGamemode.SHOWDOWN);
        mapNameList.add("Champions Crossroads");


    }

    public void initHolograms () {

        // Create holograms
        recreateHolograms();

        // Change first hologram to event title
        Component eventTitleComponent = getIconComponent().color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .append(Component.text(" " + eventName + " ")
                    .color(NamedTextColor.AQUA).decorate(TextDecoration.ITALIC))
                .append(getIconComponent().color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));

        changeHologram(0, eventTitleComponent);

        // Change second hologram
        Component timeDateComponent = smallText(eventTime).color(NamedTextColor.GOLD);
        changeHologram(1, timeDateComponent);

        Component nextGameMessage = getIconComponentWithBrackets(NamedTextColor.AQUA, true).append(
                Component.text("First up: Game 1 - ").color(NamedTextColor.AQUA)).append(Component.text(
                        gamemodeList.get(0).getGamemodeName() + " on " + mapNameList.get(0))
                        .color(NamedTextColor.GOLD));

        // Change hologram to next game
        changeHologram(3, nextGameMessage);

    }

    public void recreateHolograms () {

        hologramUUIDs.clear();

        // Incremented for scoreboard tags
        int hologramNum = 0;

        // Go through all hologram locations
        for (Location hLoc : hologramLocations) {

            // Attempt to find existing hologram
            Collection<TextDisplay> nearbyHolograms = hLoc.getNearbyEntitiesByType(TextDisplay.class, 0.1);

            // If any existing holograms exist, delete them
            for (TextDisplay h : nearbyHolograms) {
                if (!h.isDead()) {
                    if (h.getScoreboardTags().contains("EC_HOLOGRAM")) {
                        h.remove();
                    }
                }
            }

            // Create new hologram
            TextDisplay newHologram = (TextDisplay) gameManager.getWorld().spawnEntity(hLoc, EntityType.TEXT_DISPLAY);
            newHologram.setTransformation(new Transformation(new Vector3f(0, 0, 0), new Quaternionf(0.0f, 1.0f, 0.0f, 0.0f),
                    new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)));
            newHologram.setShadowed(true);
            newHologram.setBillboard(Display.Billboard.FIXED);

            if (hologramNum == 0) {
                newHologram.setTransformation(new Transformation(new Vector3f(0, 0, 0), new Quaternionf(0.0f, 1.0f, 0.0f, 0.0f),
                        new Vector3f(1.5f, 1.5f, 1.5f), new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)));
            }

            UUID hologramUUID = newHologram.getUniqueId();
            hologramUUIDs.add(hologramUUID);

            // Add scoreboard tags
            newHologram.addScoreboardTag("EC_HOLOGRAM");
            newHologram.addScoreboardTag("EC_HOLOGRAM_" + hologramNum);

            hologramNum++;

        }

    }

    public void changeHologram (int hologramNum, Component text) {

        // Get hologram UUID
        UUID hologramUUID;
        try {
            hologramUUID = hologramUUIDs.get(hologramNum);
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        // Get hologram from UUID
        TextDisplay hologram;
        try {
            hologram = (TextDisplay) CBCPlugin.getPlugin().getServer().getEntity(hologramUUID);
        } catch (ClassCastException e) {
            return;
        }

        if (hologram == null) return;

        // Change custom name of hologram
        hologram.text(text);

    }

    public void activate () {
        active = true;

        // Check if lobby is active
        if (lobby.isActive()) {
            gameManager.setPlayerListHeader(
                    setTextFont(gameManager.getEventManager().getEventName() + " ", ResourcePackFont.SMALL_5X5).color(NamedTextColor.YELLOW).append(
                            setTextFont("Lobby", ResourcePackFont.SMALL_5X5).color(NamedTextColor.AQUA))
            );
        }

        initHolograms();
        resetTeamBeacons();

    }

    public CBCEventTeam registerTeam (String teamId, String teamName, NamedTextColor teamColor) {
        // Register team
        CBCEventTeam team = new CBCEventTeam(this, teamId, teamName, teamColor);
        teams.add(team);
        return team;
    }

    public void unregisterTeam (CBCEventTeam team) {
        // Unregister team from list
        teams.remove(team);
        // Remove all players from team
        for (CBCEventPlayer player : team.getPlayers()) {
            player.setTeam(null);
        }
    }

    public CBCEventTeam getTeam (String teamId) {
        for (CBCEventTeam team : teams) {
            if (team.getTeamId().equals(teamId)) {
                return team;
            }
        }
        // No team found, so return null
        return null;
    }

    public void setCurrentGameEventGame (boolean b) {
        isCurrentGameEventGame = b;
    }

    public boolean isLastGameEventGame () {
        return isCurrentGameEventGame;
    }

    public void eventGameEnded (TeamGame game) {
        isCurrentGameEventGame = false;
        gameEnded(game);
    }

    public void gameEnded (TeamGame game) {

        // Make sure game has a winner - do not register game if it does not have a winner
        CBCTeam winner = game.getWinner();
        if (winner == null) return;

        // Find winning team
        CBCEventTeam winningTeam = getTeam(winner.getTeamId());
        if (winningTeam == null) return;

        // Set game winner and store game
        games.add(nextGameNum - 1, game);
        winners.add(nextGameNum - 1, winningTeam);

        // Check if a team has won the entire event
        if (nextGameNum == 3) {
            // Check if a team has won all three of the first games
            if (winners.get(0) == winners.get(1) && winners.get(0) == winners.get(2)) {
                eventWinner = winners.get(0);
            }
        }
        else if (nextGameNum == 4) {
            // A team has won the finale, so they win the event
            eventWinner = winners.get(3);
        }

        // Add to post game stats
        postGameStats.add(game.getPostGameStats());

        // Calculate player points
        calculateScores(game);

        // Send game end message
        sendGameEndMessage(game);

        // Update game number
        nextGameNum++;

        // Send message for next game
        if (eventWinner == null) {
            sendNextGameMessage();
        }
    }

    // Send game winner message
    public void sendGameEndMessage (Game g) {

        Component gameEndMessage = getIconComponentWithBrackets(NamedTextColor.AQUA, true);

        CBCEventTeam gameWinner = getGameWinner(nextGameNum);
        String gameDisplayName = getGameName();

        Component gameWinnerComponent = gameWinner.getNameComponent(true);

        // Send message depending on if team has won event or not
        if (eventWinner == null) {
            gameEndMessage = gameEndMessage.append(gameWinnerComponent.append(
                    Component.text(" has won " + gameDisplayName + "!").color(NamedTextColor.AQUA)
            ));

            // Change hologram to team who won
            changeHologram(0, gameWinnerComponent.append(
                    Component.text(" has won " + gameDisplayName + "!").color(NamedTextColor.AQUA)));

            // Send message about gamemode selection or qualification
            if (getGamesWon(gameWinner) == 2) {
                // They have been awarded gamemode selection
                gameEndMessage = gameEndMessage.append(Component.newline()
                    .append(
                        Component.text(" → ").color(NamedTextColor.GOLD)
                    ).append(
                        gameWinnerComponent
                    ).append(
                        Component.text(" has earned gamemode selection for the final!").color(NamedTextColor.GOLD)
                    )
                );
            }
            else {
                // They have qualified for final
                gameEndMessage = gameEndMessage.append(Component.newline()
                        .append(
                                Component.text(" → ").color(NamedTextColor.GOLD)
                        ).append(
                                gameWinnerComponent
                        ).append(
                                Component.text(" has qualified for the final!").color(NamedTextColor.GOLD)
                        )
                );
            }

            // If this is the third game, eliminate teams who have not won a game yet
            if (nextGameNum == 3) {

                Set<CBCEventTeam> eliminatedTeams = new HashSet<>();
                for (CBCEventTeam team : teams) {
                    if (getGamesWon(team) == 0) {
                        eliminatedTeams.add(team);
                    }
                }

                // Send messages for all the teams eliminated
                for (CBCEventTeam team : eliminatedTeams) {
                    gameEndMessage = gameEndMessage.append(Component.newline()
                            .append(
                                    Component.text(" → ").color(NamedTextColor.GRAY)
                            ).append(
                                    team.getNameComponent(true)
                            ).append(
                                    Component.text(" has been eliminated from " + eventNameShorthand + ".").color(NamedTextColor.GRAY)
                            )
                    );
                }
            }
        }
        else {
            gameEndMessage = getIconComponentWithBrackets(NamedTextColor.GOLD, true).append(
                    gameWinnerComponent.append(
                            Component.text(" has won " + eventName + "!").color(NamedTextColor.GOLD)
                    )
            );

            sendEventWonTitle();
        }

        // Add MVP for game and top three performers
        LinkedHashMap<CBCEventPlayer, Integer> lastGameScores = gameScores.get(nextGameNum - 1);
        LinkedHashMap<CBCEventPlayer, Integer> eventScores = eventScoresForGames.get(nextGameNum - 1);

        List<CBCEventPlayer> topThree = new ArrayList<>();

        CBCGamemode thisGamemode = getGamemode(nextGameNum);

        int rank = 0;
        for (CBCEventPlayer player : lastGameScores.keySet()) {
            rank++;
            if (rank >= 4) {
                break;
            }
            topThree.add(player);
        }

        if (!topThree.isEmpty()) {
            // Send MVP
            String mvpGameName = getGameName();
            if (mvpGameName.equals("Final")) {
                mvpGameName = "Finals";
            }

            Component MVPcomponent = topThree.get(0).getNameComponent();
            gameEndMessage = gameEndMessage.append(Component.newline()
                    .append(
                            Component.text(" → " + mvpGameName + " MVP: ").color(NamedTextColor.GOLD)
                    ).append(
                            MVPcomponent
                    )
            );

            if (eventWinner == null) {
                // Change hologram to MVP
                changeHologram(1, Component.text(mvpGameName + " MVP: ").color(NamedTextColor.GOLD)
                        .append(MVPcomponent));
            }

            // Add top three performers
            gameEndMessage = gameEndMessage.append(Component.newline()
                    .append(
                            Component.text(" → Top performers for " + mvpGameName + ":").color(NamedTextColor.GOLD)
                    ));

            int r = 0;
            for (CBCEventPlayer player : topThree) {

                // Add space to make names even
                int playerNameLength = TextUtil.getPixelLengthOfText(player.getName());
                Component spaceComponent = getComponentSpaceOfLength(92 - playerNameLength);

                r++;
                int gamePoints = lastGameScores.get(player);
                int eventPoints = eventScores.get(player);

                String gamePointsIcon = thisGamemode.getUnicodeIcon(player.getTeamColor());

                gameEndMessage = gameEndMessage.append(Component.newline()
                        .append(
                                Component.text("    " + r + ". ").color(NamedTextColor.GOLD)
                        ).append(
                                player.getNameComponent()
                        ).append(
                                spaceComponent
                        ).append(
                                Component.text(gamePointsIcon + " ").color(NamedTextColor.WHITE)
                        ).append(
                                getFourDigitNumberComponent(gamePoints, NamedTextColor.YELLOW)
                        ).append(
                                getPointsIconComponent()
                        ).append(
                                getFourDigitNumberComponent(eventPoints, NamedTextColor.YELLOW)
                        )
                );
            }
        }


        // Send game end message
        gameManager.getWorld().sendMessage(Component.newline().append(gameEndMessage));

        // Send points to each player
        for (CBCEventPlayer player : lastGameScores.keySet()) {
            int gamePoints = lastGameScores.get(player);
            int eventPoints = eventScores.get(player);

            String gamePointsIcon = thisGamemode.getUnicodeIcon(player.getTeamColor());

            if (!player.isOnline()) return;

            player.getPlayer().sendMessage(Component.newline()
                    .append(
                            Component.text(" → You earned ").color(NamedTextColor.GOLD)
                    ).append(
                            Component.text(gamePointsIcon + " ").color(NamedTextColor.WHITE)
                    ).append(
                            Component.text(gamePoints).color(NamedTextColor.YELLOW)
                    ).append(
                            Component.text(" in that game, earning you").color(NamedTextColor.GOLD)
                    ).append(
                            getPointsIconComponent()
                    ).append(
                            Component.text(eventPoints).color(NamedTextColor.YELLOW)
                    )
            );
        }

        gameManager.getWorld().sendMessage(Component.text(""));

    }

    public void sendEventWonTitle () {

        if (eventWinner == null) return;

        ResourcePackManager resourcePackManager = CBCPlugin.getResourcePackManager();

        Component crossbowCharacterComponent = Component.text(CBCGamemode.SHOWDOWN.getUnicodeIcon(eventWinner.getTeamColor())).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE);

        Component titleComponent = crossbowCharacterComponent.append(Component.text(" "));

        // Get all the players in the winning team

        Component hologram1component = blankComponent();
        Component hologram2component = blankComponent();

        int i = 0;
        for (CBCEventPlayer player : eventWinner.getPlayers()) {
            Component headComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, player.getOfflinePlayer());
            titleComponent = titleComponent.append(headComponent.append(Component.text(" ")));

            if (i == 0) {
                hologram1component = hologram1component.append(headComponent).append(Component.space())
                        .append(player.getNameComponent());
            }
            else if (i == 1) {
                hologram2component = hologram2component.append(headComponent).append(Component.space())
                        .append(player.getNameComponent());
            }
            else if (i == 2) {
                hologram1component = hologram1component.append(Component.space()).append(headComponent)
                        .append(Component.space()).append(player.getNameComponent());
            }
            else if (i == 3) {
                hologram2component = hologram2component.append(Component.space()).append(headComponent)
                        .append(Component.space()).append(player.getNameComponent());
            }
            i++;
        }

        titleComponent = titleComponent.append(crossbowCharacterComponent);

        // Create subtitle component
        Component subtitleComponent = eventWinner.getNameComponent(true).decorate(TextDecoration.BOLD).append(Component.text(" "));

        subtitleComponent = subtitleComponent.append(getIconComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));
        subtitleComponent = subtitleComponent.append(Component.text(" " + eventNameShorthand + " Champions").color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        Title title = Title.title(titleComponent, subtitleComponent, titleTimes(0, 11000, 2000));
        gameManager.getWorld().showTitle(title);

        // Play fireworks
        try {
            new FireworkTask(eventWinner.getTeamColor(), new Location(gameManager.getWorld(), -1051.00, 133.00, -1639.00)).runTaskTimer(CBCPlugin.getPlugin(), 20, 15);
            new FireworkTask(eventWinner.getTeamColor(), new Location(gameManager.getWorld(), -1051.00, 133.00, -1639.00)).runTaskTimer(CBCPlugin.getPlugin(), 60, 10);
        } catch (Exception ignored) {}

        // Change holograms
        changeHologram(0, Component.text(eventNameShorthand + " Champions: ").color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD).append(eventWinner.getNameComponent(true).decorate(TextDecoration.BOLD)));

        changeHologram(1, hologram1component);
        changeHologram(2, hologram2component);

        changeHologram(3, blankComponent());

        // Change beacons
        winnerTeamBeacons();

    }

    public void announceMVP () {

        ResourcePackManager resourcePackManager = CBCPlugin.getResourcePackManager();

        if (getEventPlayers().isEmpty()) return;

        // Calculate MVP
        CBCEventPlayer MVP = getEventPlayersSortedByScore().get(0);
        Component playerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, MVP.getOfflinePlayer());
        Component crossbowCharacterComponent = Component.text(CBCGamemode.SHOWDOWN.getUnicodeIcon(MVP.getTeamColor())).color(NamedTextColor.WHITE);
        int finalScore = MVP.getEventScore();

        // Create initial title component
        Component titleComponent = getIconComponent().append(
                Component.text(" " + eventNameShorthand + " MVP ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
        ).append(
                getIconComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
        );

        // Create initial subtitle component
        Component subtitleComponent = Component.text("MVP for " + eventNameShorthand + " goes to...").color(NamedTextColor.YELLOW);

        // Send title
        Title title = Title.title(titleComponent, subtitleComponent, titleTimes(0, 7000, 1000));
        gameManager.getWorld().showTitle(title);

        // Create MVP title component
        Component MVPtitleComponent = crossbowCharacterComponent.decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).append(
                Component.text(" ")
                        .append(
                                MVP.getNameComponent().decorate(TextDecoration.BOLD)
                        ).append(
                                Component.text(" ")
                        ).append(
                                playerHeadComponent.color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                )
        );

        // Create MVP subtitle component
        Component MVPsubtitleComponent = Component.text("Score:").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD)
                        .append(
                                getPointsIconComponent()
                        ).append(
                                Component.text(finalScore).color(NamedTextColor.YELLOW)
                        );

        // Send title
        Title mvpTitle = Title.title(MVPtitleComponent, MVPsubtitleComponent, titleTimes(0, 7000, 1000));
        gameManager.getWorld().showTitle(title);

        Component mvpMessageComponent = Component.newline().append(getIconComponentWithBrackets(NamedTextColor.GOLD, true).append(
                MVP.getNameComponent().append(
                        Component.text(" has won " + eventName + " MVP!").color(NamedTextColor.GOLD)
                ).append(Component.newline()))
        );

        // After 6 seconds, say who the MVP is and play a sound
        new BukkitRunnable() {
            @Override
            public void run () {
                gameManager.getWorld().showTitle(mvpTitle);
                for (Player player : gameManager.getWorld().getPlayers()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 0);
                }
                // Send message for MVP
                gameManager.getWorld().sendMessage(mvpMessageComponent);

                changeHologram(3, Component.text(eventNameShorthand + " MVP: ").color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD).append(MVP.getNameComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)).append(
                                getPointsIconComponent()
                        ).append(
                                Component.text(finalScore).color(NamedTextColor.YELLOW)
                        ));

            }

        }.runTaskLater(CBCPlugin.getPlugin(), 120);

        // After 16 seconds, say the final leaderboard
        Component leaderboard = generateLeaderboard();
        new BukkitRunnable() {
            @Override
            public void run () {
                // Send leaderboard message
                gameManager.getWorld().sendMessage(leaderboard);
            }
        }.runTaskLater(CBCPlugin.getPlugin(), 320);
    }

    public Component generateLeaderboard() {

        Component leaderboardComponent = Component.newline().append(Component.text(eventName + " Event Scores").color(NamedTextColor.GOLD));

        // Go through all players sorted by score
        int rank = 0;
        for (CBCEventPlayer eventPlayer : getEventPlayersSortedByScore()) {

            // Add new line
            Component playerComponent = Component.newline().append(Component.text(" "));

            String playerName = eventPlayer.getName();

            rank++;

            if (rank < 10) {
                playerComponent = playerComponent.append(getComponentSpaceOfLength(7));
            }

            // Add rank
            playerComponent = playerComponent.append(Component.text(rank + ". ").color(NamedTextColor.GOLD));

            // Add player name, adding space to make names even
            int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
            Component spaceComponent = getComponentSpaceOfLength(92 - playerNameLength);

            playerComponent = playerComponent.append(eventPlayer.getNameComponent()).append(spaceComponent);

            // Add points icon
            playerComponent = playerComponent.append(getIconComponent()).append(Component.text(" "));

            // Add player points
            int playerEventScore = eventPlayer.getEventScore();
            Component numberComponent = getFourDigitNumberComponentWithGrayZeroes(playerEventScore, NamedTextColor.AQUA);

            playerComponent = playerComponent.append(numberComponent);

            // Go through each game and put the score there
            NamedTextColor teamColor = eventPlayer.getTeamColor();
            for (int gameNum = 1; gameNum <= 4; gameNum++) {

                // Get icon for gamemode
                CBCGamemode gamemode = getGamemode(gameNum);
                String gamemodeIcon = gamemode.getUnicodeIcon(teamColor);

                playerComponent = playerComponent.append(Component.text(" " + gamemodeIcon + " ").color(NamedTextColor.WHITE));

                // Add player points for that game
                int playerGameScore = eventPlayer.getEventScoreForGame(gameNum);
                Component numberGameComponent = getFourDigitNumberComponentWithGrayZeroes(playerGameScore, NamedTextColor.YELLOW);

                playerComponent = playerComponent.append(numberGameComponent);

            }

            leaderboardComponent = leaderboardComponent.append(playerComponent);
        }

        leaderboardComponent = leaderboardComponent.append(Component.newline());
        return leaderboardComponent;

    }

    public void sendNextGameMessage() {

        CBCGamemode gamemode = null;
        String mapName = null;
        CBCEventTeam teamPick = null;

        String gameName = getGameName();

        if (nextGameNum == 4) {
            if (getGamesWon(winners.get(0)) > 1) {
                // Team has selection
                teamPick = winners.get(0);
            }
            else if (getGamesWon(winners.get(1)) > 1) {
                // Team has selection
                teamPick = winners.get(1);
            }
            else {
                gamemode = gamemodeList.get(3);
                mapName = mapNameList.get(3);
            }
        }
        else if (nextGameNum <= 3) {
            gamemode = gamemodeList.get(nextGameNum - 1);
            mapName = mapNameList.get(nextGameNum - 1);
        }

        // Send message
        Component nextGameMessage = getIconComponentWithBrackets(NamedTextColor.AQUA, true).append(
                Component.text("Next up: " + gameName + " - ").color(NamedTextColor.AQUA)
        );
        if (teamPick == null && gamemode != null) {
            nextGameMessage = nextGameMessage.append(Component.text(gamemode.getGamemodeName() + " on " + mapName)
                    .color(NamedTextColor.GOLD));
        }
        else {
            if (teamPick != null) {
                nextGameMessage = nextGameMessage.append(teamPick.getNameComponent(true)
                        .append(Component.text("'s pick").color(teamPick.getTeamColor())));
            }
        }

        gameManager.getWorld().sendMessage(nextGameMessage.append(Component.newline()));

        // Change hologram to next game
        changeHologram(3, nextGameMessage);


    }

    // Calculate scores
    public void calculateScores (Game g) {

        List<CBCPlayer> cbcPlayers = new ArrayList<>(g.getPlayers().values());

        if (cbcPlayers.isEmpty()) return;

        // Sort players by game score
        cbcPlayers.sort(Comparator.comparingInt(CBCPlayer::getGamePoints).reversed());

        // Get maximum game score
        int maxGameScore = cbcPlayers.get(0).getGamePoints();
        int minGameScore = 0;

        if (g.getGamemode() == CBCGamemode.CBCTAG || g.getGamemode() == CBCGamemode.RENDEZVOUS) {
            minGameScore = cbcPlayers.get(cbcPlayers.size() - 1).getGamePoints() / 2;
        }

        // Calculate multiplier
        double multiplier = 2500 / ((double) maxGameScore - (double) minGameScore);

        // Go through every player and get their score
        LinkedHashMap<CBCEventPlayer, Integer> lastGameScores = new LinkedHashMap<>();
        LinkedHashMap<CBCEventPlayer, Integer> eventScores = new LinkedHashMap<>();
        for (CBCPlayer player : cbcPlayers) {
            UUID playerUUID = player.getOfflinePlayer().getUniqueId();

            // Check if player is registered in event
            CBCEventPlayer eventPlayer = getPlayer(playerUUID);
            if (eventPlayer == null) continue;

            // Get player's score
            int gameScore = player.getGamePoints();
            lastGameScores.put(eventPlayer, gameScore);

            // Calculate player's event score
            int eventScore = (int) Math.round(((double) player.getGamePoints() - minGameScore) * multiplier);
            eventScores.put(eventPlayer, eventScore);

            eventPlayer.setGameScoreForGame(nextGameNum, eventScore);
        }

        // Store game scores in database
        gameScores.add(lastGameScores);
        eventScoresForGames.add(eventScores);
    }

    public Title getPregameTitle () {

        Component title = noShadowText(Component.text(String.valueOf(eventLogoUnicode)));

        // Get character for game title
        Component subtitle = noShadowText(Component.text(String.valueOf((char) (eventLogoUnicode + nextGameNum))));

        // Return title
        return Title.title(
                title,
                subtitle,
                TextUtil.titleTimes(0, 3000, 500)
        );

    }

    public void resetTeamBeacons () {

        for (NamedTextColor color : beaconGlassBlocksLocs.keySet()) {

            Location location = beaconGlassBlocksLocs.get(color);

            Material normalMat = beaconGlassBlock.get(color);
            Material altMat = beaconAltGlassBlock.get(color);

            Block altBlock = location.getBlock();
            altBlock.setType(altMat);

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 1; x++) {
                    for (int z = 0; z < 1; z++) {
                        Location newLocation = location.clone().add(x, y, z);
                        Block block = newLocation.getBlock();
                        if (y == 0) {
                            block.setType(altMat);
                        }
                        else {
                            block.setType(normalMat);
                        }
                    }
                }
            }

        }

    }

    public void winnerTeamBeacons () {

        Material normalMat = beaconGlassBlock.get(eventWinner.getTeamColor());
        Material altMat = beaconAltGlassBlock.get(eventWinner.getTeamColor());

        for (Location loc : beaconGlassBlocksLocs.values()) {

            for (int y = 0; y < 4; y++) {
                for (int x = 0; x < 1; x++) {
                    for (int z = 0; z < 1; z++) {
                        Location newLocation = loc.clone().add(x, y, z);
                        Block block = newLocation.getBlock();
                        if (y == 0) {
                            block.setType(altMat);
                        }
                        else {
                            block.setType(normalMat);
                        }
                    }
                }
            }

        }

    }

    public int getNextGameNum () {
        return nextGameNum;
    }

    public String getGameName () {
        if (nextGameNum == 4) {
            return ("Final");
        } else {
            return ("Game " + nextGameNum);
        }
    }

    public String getGameName (int gameNum) {
        if (gameNum == 4) {
            return ("Final");
        } else {
            return ("Game " + gameNum);
        }
    }


    public String getGameNameShorthand () {
        if (nextGameNum == 4) {
            return ("Final");
        } else {
            return ("G" + nextGameNum);
        }
    }

    public CBCEventPlayer createPlayer (Player playerEntity) {
        CBCEventPlayer player = new CBCEventPlayer(playerEntity);
        players.put(playerEntity.getUniqueId(), player);
        return player;
    }

    public boolean hasPlayer (UUID playerUUID) {
        return getPlayer(playerUUID) != null;
    }

    public CBCEventPlayer getPlayer (UUID playerUUID) {
        return players.getOrDefault(playerUUID, null);
    }


    public String getEventName() {
        return eventName;
    }

    public CBCGamemode getGamemode (int gameNum) {
        try {
            return gamemodeList.get(gameNum - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public CBCEventTeam getGameWinner (int gameNum) {
        try {
            return winners.get(gameNum - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    public int getGamesWon (CBCEventTeam team) {
        int gamesWon = 0;
        for (int i = 1; i <= 4; i++) {
            if (getGameWinner(i) == team) {
                gamesWon++;
            }
        }
        return gamesWon;
    }

    public CBCEventTeam getEventWinner () {
        return eventWinner;
    }

    public Location getLobbyTeleportLocation() {
        return lobbyTeleportLocation;
    }

    public Location getWinnerTeleportLocation() {
        return winnerTeleportLocation;
    }

    public void setGamemode(CBCGamemode gamemode, int gameNum) {

        gamemodeList.set(gameNum - 1, gamemode);

        // Update lobby sidebar if active
        if (lobby.isActive()) {
            lobby.updateServerSidebars();
        }
    }

    public String getEventNameShorthand() {
        return eventNameShorthand;
    }

    public void addPlayer (Player player) {
        // Create player
        CBCEventPlayer eventPlayer = new CBCEventPlayer(player);
        players.put(player.getUniqueId(), eventPlayer);
    }

    public void removePlayer (CBCEventPlayer player) {
        players.remove(player.getPlayerUUID());
        player.getTeam().removePlayer(player);
    }

    public boolean isPlayer (Player player) {
        // Check if player in players
        return players.containsKey(player.getUniqueId());
    }

    public void setPlayerTeam (CBCEventPlayer eventPlayer, CBCEventTeam team) {

        if (eventPlayer.getTeam() != null) {
            eventPlayer.getTeam().removePlayer(eventPlayer);
        }

        team.addPlayer(eventPlayer);
        eventPlayer.setTeam(team);

    }

    public Collection<CBCEventPlayer> getEventPlayers () {
        return players.values();
    }

    public List<CBCEventPlayer> getEventPlayersOnTeam (CBCEventTeam team) {
        return new ArrayList<>(team.getPlayers());
    }

    public List<CBCEventPlayer> getEventPlayersOnTeamSortedByScore (CBCEventTeam team) {
        List<CBCEventPlayer> teamPlayers = new ArrayList<>(team.getPlayers());
        teamPlayers.sort(Comparator.comparingInt(CBCEventPlayer::getEventScore).reversed().thenComparing(CBCEventPlayer::getName));
        return teamPlayers;
    }

    public List<CBCEventPlayer> getEventPlayersSortedByScore () {
        List<CBCEventPlayer> plrs = new ArrayList<>(getEventPlayers());
        plrs.sort(Comparator.comparingInt(CBCEventPlayer::getEventScore).reversed().thenComparing(CBCEventPlayer::getName));
        return plrs;
    }

    public Component getIconComponent () {
        String eventIcon = "\uE905";
        return Component.text(eventIcon).color(NamedTextColor.WHITE);
    }

    public Component getPointsIconComponent () {
        String pointsIcon = "\uE905";
        return Component.text(" " + pointsIcon + " ").color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE);
    }

    public Component getIconComponentWithBrackets (NamedTextColor bracketColor, boolean withSpace) {
        if (withSpace) {
            return Component.text("[").color(bracketColor).append(getIconComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).append(Component.text("] ").color(bracketColor)));
        } else {
            return Component.text("[").color(bracketColor).append(getIconComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).append(Component.text("]").color(bracketColor)));
        }
    }

    public Component getFourDigitNumberComponent (int number, NamedTextColor colorNumber) {
        Component c = Component.text(number).color(colorNumber);
        if (number < 10) {
            c = c.append(getComponentSpaceOfLength(18));
        }
        else if (number < 100) {
            c = c.append(getComponentSpaceOfLength(12));
        }
        else if (number < 1000) {
            c = c.append(getComponentSpaceOfLength(6));
        }
        return c;
    }

    public Component getFourDigitNumberComponentWithGrayZeroes (int number, NamedTextColor colorNumber) {

        Component c = Component.text("");

        if (number < 1000) {
            c = c.append(getComponentSpaceOfLength(7));
        }
        if (number < 100) {
            c = c.append(getComponentSpaceOfLength(7));
        }
        if (number < 10) {
            c = c.append(getComponentSpaceOfLength(7));
        }

        c = c.append(Component.text(number).color(colorNumber));
        return c;
    }

    public List<TeamGame> getGameList () {
        return games;
    }

    public PostGameStats getPostGameStats (int gameNum) {
        if (gameNum < 0 || gameNum > postGameStats.size()) {
            return null;
        }
        return postGameStats.get(gameNum - 1);
    }

    public String getEventTagline() {
        return eventTagline;
    }

    public boolean isPlayerWinner (Player playerEntity) {

        if (eventWinner == null) return false;

        CBCEventPlayer eventPlayer = getPlayer(playerEntity.getUniqueId());
        if (eventPlayer == null) return false;

        return eventWinner == eventPlayer.getTeam();
    }

    public List<CBCEventTeam> getTeams() {
        return teams;
    }

    public List<String> getAllTeamIds () {
        List<String> teamIds = new ArrayList<>();
        for (CBCEventTeam team : getTeams()) {
            teamIds.add(team.getTeamId());
        }
        return teamIds;
    }

    public List<String> getUnusedTeamIds () {
        List<String> teamIds = CBCEventTeam.getAllTeamIds();
        for (CBCEventTeam team : getTeams()) {
            teamIds.remove(team.getTeamId());
        }
        return teamIds;
    }
}
