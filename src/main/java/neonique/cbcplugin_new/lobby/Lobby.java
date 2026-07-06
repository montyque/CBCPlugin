package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.*;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.weapons.WeaponType;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mechanics.*;
import neonique.cbcplugin_new.lobby.listeners.MenuClickEvent;
import neonique.cbcplugin_new.lobby.listeners.PlayerDamageListener;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.cbcevents.CBCEventPlayer;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.lobby.tasks.GameCountdownTask;
import neonique.cbcplugin_new.lobby.tasks.LobbySidebarManagerTask;
import neonique.cbcplugin_new.lobby.tasks.PlayerSafetyTask;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.weapons.presets.WeaponPreset;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.setTextFont;

public class Lobby {

    private final GameManager gameManager;

    boolean active = false;

    // Lobby variables
    private Location lobbyTeleport;

    // Map selected
    private CBCGamemode gamemodeSelected;
    private GamemodeMapData mapSelected;
    private GameSettings gameSettings;

    private CreeperPreset creeperPreset;
    private FlamePreset flamePreset;
    private XbowPreset xbowPreset;

    private HashMap<String, CreeperPreset> creeperPresetTeamOverrides;
    private HashMap<String, FlamePreset> flamePresetTeamOverrides;
    private HashMap<String, XbowPreset> xbowPresetTeamOverrides;

    // If the game is starting
    private boolean gameStarting;
    private GameCountdownTask startingCountdown;

    // Players and teams
    private Map<UUID, LobbyPlayer> players = new HashMap<>();
    private Map<TeamColor, LobbyTeam> teams = new LinkedHashMap<>();
    private CBCScoreboardTeam spectatorTeam;
    private CBCScoreboardTeam ffaTeam;

    // Lobby sidebar manager
    private LobbySidebarManager sidebarManager;

    // Listeners and tasks
    private MenuClickEvent menuClickEvent;
    private PlayerDamageListener playerDamageListener;

    private PlayerSafetyTask playerSafetyTask;
    private LobbySidebarManagerTask lobbySidebarManagerTask;

    // Image maps
    private LobbyImageMaps imageMaps;

    public Lobby(GameManager gameManager) {

        this.gameManager = gameManager;

        World world = gameManager.getWorld();
        lobbyTeleport = new Location(world, -1069.5, 126, -1668.5);

        menuClickEvent = new MenuClickEvent(this, gameManager);
        playerDamageListener = new PlayerDamageListener(this, gameManager);

        // Image maps manager
        imageMaps = new LobbyImageMaps(this);

        creeperPreset = CreeperPreset.getDefaultPreset();
        flamePreset = FlamePreset.getDefaultPreset();
        xbowPreset = XbowPreset.getDefaultPreset();

        creeperPresetTeamOverrides = new HashMap<>();
        flamePresetTeamOverrides = new HashMap<>();
        xbowPresetTeamOverrides = new HashMap<>();
    }

    public void activate() {

        if (active) return;

        // Reset variables
        gameStarting = false;
        gamemodeSelected = null;
        mapSelected = null;
        gameSettings = GameSettings.blank();

        creeperPresetTeamOverrides = new HashMap<>();
        flamePresetTeamOverrides = new HashMap<>();
        xbowPresetTeamOverrides = new HashMap<>();

        creeperPreset = CreeperPreset.getDefaultPreset();
        flamePreset = FlamePreset.getDefaultPreset();
        xbowPreset = XbowPreset.getDefaultPreset();

        players.clear();
        teams.clear();

        // Activate lobby
        active = true;

        // Set world spawn
        gameManager.getWorld().setSpawnLocation(lobbyTeleport);

        // Activate listeners and tasks
        CBCPlugin plugin = CBCPlugin.getPlugin();
        plugin.getServer().getPluginManager().registerEvents(menuClickEvent, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerDamageListener, plugin);

        playerSafetyTask = new PlayerSafetyTask(gameManager, this);
        playerSafetyTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 60);

        // Create teams
        teams = createTeams();

        // Creating spectator and FFA scoreboard teams
        gameManager.getCbcScoreboardManager().activate();
        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();
        ffaTeam = scoreboardManager.registerNewTeam("09ffaLobby");
        ffaTeam.setFriendlyFireEnabled(true);
        ffaTeam.setPrefix(Component.text(" ■ ").color(NamedTextColor.WHITE));
        spectatorTeam = scoreboardManager.registerNewTeam("10spectatorLobby");
        spectatorTeam.setFriendlyFireEnabled(true);
        spectatorTeam.setPrefix(Component.text(" □ ").color(NamedTextColor.WHITE));

        // Create lobby players for all online players
        for (Player player : CBCPlugin.getPlugin().getServer().getOnlinePlayers()) {
            addNewLobbyPlayer(player);
        }

        // Setup sidebar manager
        sidebarManager = new LobbySidebarManager(gameManager, this);
        sidebarManager.setupSidebar();

        // Activate sidebar task
        lobbySidebarManagerTask = new LobbySidebarManagerTask(this);
        lobbySidebarManagerTask.runTaskTimer(plugin, 0, 40);

        // Create list header
        if (gameManager.isCBCEventActive()) {
            gameManager.setPlayerListHeader(
                    setTextFont(gameManager.getEventManager().getEventName() + " ", ResourcePackFont.SMALL_5X5).color(NamedTextColor.YELLOW).append(
                            setTextFont("Lobby", ResourcePackFont.SMALL_5X5).color(NamedTextColor.AQUA))
            );
        }
        else {
            gameManager.setPlayerListHeader(
                    setTextFont("Crossbow Champions ", ResourcePackFont.SMALL_5X5).color(NamedTextColor.YELLOW).append(
                            setTextFont("Lobby", ResourcePackFont.SMALL_5X5).color(NamedTextColor.AQUA))
            );
        }

        gameManager.setPlayerListFooter(Component.text(""));
        imageMaps.createItemFrames();

    }

    private Map<TeamColor, LobbyTeam> createTeams () {
        return Map.of(
                TeamColor.RED, new LobbyTeam(gameManager, this, "01", "red", "Red", "R", TeamColor.RED),
                TeamColor.BLUE, new LobbyTeam(gameManager, this, "02", "blue", "Blue", "B", TeamColor.BLUE),
                TeamColor.GREEN, new LobbyTeam(gameManager, this, "03", "green", "Green", "G", TeamColor.GREEN),
                TeamColor.YELLOW, new LobbyTeam(gameManager, this, "04", "yellow", "Yellow", "Y", TeamColor.YELLOW),
                TeamColor.CYAN, new LobbyTeam(gameManager, this, "05", "cyan", "Cyan", "C", TeamColor.CYAN),
                TeamColor.ORANGE, new LobbyTeam(gameManager, this, "06", "orange", "Orange", "O", TeamColor.ORANGE),
                TeamColor.MAGENTA, new LobbyTeam(gameManager, this, "07", "magenta", "Magenta", "M", TeamColor.MAGENTA),
                TeamColor.PURPLE, new LobbyTeam(gameManager, this, "08", "purple", "Purple", "P", TeamColor.PURPLE)
        );
    }

    public void deactivate() {

        active = false;

        // Deactivate listeners and tasks
        InventoryClickEvent.getHandlerList().unregister(menuClickEvent);
        EntityDamageEvent.getHandlerList().unregister(playerDamageListener);

        playerSafetyTask.cancel();
        sidebarManager.removeSidebar();
        lobbySidebarManagerTask.cancel();

        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeam(ffaTeam);
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeam(spectatorTeam);

        for (LobbyTeam lobbyTeam : teams.values()) {
            lobbyTeam.removeTeam();
        }

        players.clear();
        teams.clear();        // Unregister all teams

        ffaTeam = null;
        spectatorTeam = null;

        imageMaps.deleteItemFrames();

        // Check if practice is off, or if there is no game, then disable the scoreboard manager
        if (!gameManager.practiceManager.isEnabled() && gameManager.getCurrentGame() == null) {
            gameManager.getCbcScoreboardManager().deactivate();
        }

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

        // Convert set of teams into list
        randomizePlayersIntoTeams(playersToRandomize, teamsSelected);

        gameManager.sendGlobalMessage(Component.text("Randomised "));

        // Update sidebars
        updateClientSidebars();

    }

    public void openTeamRandomizeMenu(Player user) {

        int minTeams = 1;
        int maxTeams = 8;
        List<TeamColor> teamsAllowed = List.of(TeamColor.values());

        // Make sure gamemode is team gamemode
        if (gamemodeSelected != null && mapSelected != null) {
            if (gamemodeSelected.isTeamGamemode()) {
                TeamMapData teamMap = (TeamMapData) mapSelected;
                minTeams = teamMap.minTeams();
                maxTeams = teamMap.maxTeams();
                teamsAllowed = teamMap.validTeamColors();
            }
        }

        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Randomize Teams").decorate(TextDecoration.BOLD));

        int itemSlot = 0;
        int randomisingTeams = 0;
        for (TeamColor teamColor : teamsAllowed) {

            LobbyTeam team = teams.get(teamColor);
            ItemStack item = team.getIconItem();
            ItemMeta itemMeta = item.getItemMeta();

            if (itemSlot < maxTeams) {

                // Set randomising to true
                itemMeta.addEnchant(Enchantment.THORNS, 1, false);
                List<Component> loreList = List.of(Component.text("Click to remove from randomisation")
                        .color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                itemMeta.lore(loreList);
                item.setItemMeta(itemMeta);
                randomisingTeams++;

            } else {

                List<Component> loreList = List.of(Component.text("Click to add to randomisation")
                        .color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                itemMeta.lore(loreList);

                // Set item to transparent version
                int newCustomModelData = (itemMeta.getCustomModelData() % 8) + 8;
                if (itemMeta.getCustomModelData() == 8) {
                    newCustomModelData = 16;
                }

                itemMeta.setCustomModelData(newCustomModelData);
                itemMeta.lore(loreList);
                item.setItemMeta(itemMeta);

            }

            gui.setItem(itemSlot, item);
            itemSlot++;

        }

        // Create magenta glazed terracotta to press when randomising
        ItemStack item = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA);
        ItemMeta itemMeta = item.getItemMeta();

        // Set item title
        Component itemTitle = Component.text("Randomise players into " + randomisingTeams + " teams").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        item.setItemMeta(itemMeta);
        gui.setItem(26, item);

        user.openInventory(gui);
    }

    public void openGamemodeMenu(Player user) {
        Inventory gui = getGamemodeMenu(user);
        user.openInventory(gui);
    }

    public Inventory getGamemodeMenu (Player user) {

        List<CBCGamemode> gamemodes = gameManager.getLoadedGamemodes();
        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Select Gamemode").decorate(TextDecoration.BOLD));

        int teamGamemodeCount = 0;
        int ffaGamemodeCount = 0;

        for (CBCGamemode gamemode : gamemodes) {
            ItemStack item = getGamemodeItem(gamemode);
            // Team gamemodes go on top row, FFA gamemodes go on middle row
            if (gamemode.isTeamGamemode()) {
                gui.setItem(teamGamemodeCount, item);
                teamGamemodeCount++;
            } else {
                gui.setItem(ffaGamemodeCount + 9, item);
                ffaGamemodeCount++;
            }
        }

        return gui;

    }

    private ItemStack getGamemodeItem (CBCGamemode gamemode) {
        ItemStack item = gamemode.getGamemodeIconItem();
        item.editMeta((m) -> editGamemodeItemMeta(m, gamemode));
        return item;
    }

    private void editGamemodeItemMeta (ItemMeta meta, CBCGamemode gamemode) {

        Component itemTitle = Component.text(gamemode.getGamemodeName())
                .color(gamemode.getColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);

        meta.displayName(itemTitle);

        if (gamemode.isTeamGamemode()) {
            meta.lore(List.of(
                    Component.text("Teams")
                            .color(NamedTextColor.DARK_GREEN)
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            ));
        } else {
            meta.lore(List.of(
                    Component.text("Free for all")
                            .color(NamedTextColor.DARK_PURPLE)
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            ));
        }

        meta.getPersistentDataContainer().set(MenuClickEvent.MENU_ITEM_ID_KEY,
                PersistentDataType.STRING, gamemode.name()
        );

    }


    public void openMapMenu (Player user, CBCGamemode gamemode) {
        Inventory menu = getMapMenu(user, gamemode);
        user.openInventory(menu);
    }

    public Inventory getMapMenu (Player user, CBCGamemode gamemode) {

        // Find all maps loaded for this gamemode
        List<GamemodeMapData> mapList = gameManager.getGamemodeMaps(gamemode);
        mapList.sort(Comparator.comparing(m -> m.getMap().getName()));
        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Select Map - " + gamemode.getGamemodeName())
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        // Put map items in inventory slots
        int mapCount = 0;
        for (GamemodeMapData map : mapList) {
            ItemStack mapItem = getMapItem(gamemode.getColor(), map.getMap());
            gui.setItem(mapCount, mapItem);
        }

        return gui;

    }

    private ItemStack getMapItem (TextColor nameColor, CBCMap map) {
        ItemStack item = new ItemStack(map.getBlockSymbol());
        item.editMeta((m) -> {
            m.getPersistentDataContainer().set(MenuClickEvent.MENU_ITEM_ID_KEY,
                    PersistentDataType.STRING, map.getId()
            );
            Component itemTitle = Component.text(map.getName()).color(nameColor)
                    .decorate(TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            m.displayName(itemTitle);
        });
        return item;
    }

    public void setGamemodeAndMapSelected (CBCGamemode gamemode, GamemodeMapData map) {

        // Check if gamemode has changed
        if (gamemodeSelected != gamemode) {
            gameSettings = gamemode.defaultGameSettings();
        }

        gamemodeSelected = gamemode;
        mapSelected = map;

        // Send message to say what map has been selected
        Component message = Component.text().content(gamemodeSelected.getGamemodeName() + " - " + map.getMap().getName())
                        .color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                        .append(
                                Component.text().content(" has been selected!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD,
                                        TextDecoration.State.FALSE)
                        ).build();
        gameManager.getWorld().sendMessage(message);

        // Update server sidebars
        updateServerSidebars();

        // Change image map
        imageMaps.clearImage();

        String imageFileName = gameManager.getImageFile(gamemode, map.getMap().getId());
        if (imageFileName != null) {
            imageMaps.setImage(imageFileName);
        }
    }

    public CBCGamemode getGamemodeSelected () {
        return gamemodeSelected;
    }

    public CBCMap getMapSelected () {
        return mapSelected != null ? mapSelected.getMap() : null;
    }

    public void addNewLobbyPlayer(Player player) {

        player.playerListName(null);
        LobbyPlayer newPlayer = new LobbyPlayer(gameManager, this, player);
        players.put(player.getUniqueId(), newPlayer);
        ffaTeam.addEntityUUID(player.getUniqueId());

        if (!gameManager.isPracticeActive() || !gameManager.hasPlayer(player)) {
            if (gameManager.isCBCEventActive()) {
                CBCEventManager eventManager = gameManager.getEventManager();
                if (eventManager.isPlayerWinner(player)) {
                    player.teleport(gameManager.getEventManager().getWinnerTeleportLocation());
                }
                else {
                    player.teleport(gameManager.getEventManager().getLobbyTeleportLocation());
                }
            }
            else {
                player.teleport(lobbyTeleport);
            }
            player.setRespawnLocation(lobbyTeleport, true);
        }
        newPlayer.resetPlayer();

    }

    public void playerJoinTeam(LobbyPlayer player, LobbyTeam team, boolean overrideCurrentTeam) {
        if (player.getAssignedTeam() != null) {
            // Make them leave their current team if overrideCurrentTeam is enabled
            if (overrideCurrentTeam) {
                playerLeaveTeam(player, false);
            }
        }
        team.addPlayer(player);
    }

    public void playerLeaveTeam(LobbyPlayer player, boolean spectator) {
        LobbyTeam playerTeam = player.getAssignedTeam();
        if (playerTeam == null) return;
        playerTeam.removePlayer(player);
        if (!spectator) {
            ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        } else {
            spectatorTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        }
    }

    public void playerToggleSpectator (LobbyPlayer player) {
        if (player.isSpectator()) playerUnsetSpectator(player);
        else playerSetSpectator(player);
    }

    public void playerSetSpectator (LobbyPlayer player) {
        player.setSpectator(true);
        if (!player.isOnline()) return;
        playerLeaveTeam(player, true);
        spectatorTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.getPlayer().sendMessage(
                Component.text("You are now a spectator. To get out of spectator mode, use ").color(NamedTextColor.GOLD)
                        .append(Component.text("/lobby spectator toggle").color(NamedTextColor.YELLOW))
        );
    }

    public void playerUnsetSpectator (LobbyPlayer player) {
        player.setSpectator(false);
        if (!player.isOnline()) return;
        ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
        player.getPlayer().sendMessage(
                Component.text("You are no longer a spectator. To go back into spectator mode, use ").color(NamedTextColor.GOLD)
                        .append(Component.text("/lobby spectator toggle").color(NamedTextColor.YELLOW))
        );
    }

    public Collection<LobbyTeam> getTeamsSet () {
        return teams.values();
    }

    public Map<TeamColor, LobbyTeam> getTeams () {
        return teams;
    }

    public Collection<LobbyPlayer> getLobbyPlayers() {
        return players.values();
    }

    public LobbyPlayer getLobbyPlayer (Player player) {
        return players.getOrDefault(player.getUniqueId(), null);
    }

    public Set<LobbyTeam> getTeamsWithOnlinePlayers () {
        return teams.values().stream()
                .filter(l -> !l.getOnlinePlayers().isEmpty())
                .collect(Collectors.toSet());
    }

    public boolean isGameStarting() {
        return gameStarting;
    }

    public void checkStartConditions() {

        if (gamemodeSelected == null) throw new IllegalStateException("Gamemode has not been selected");
        if (mapSelected == null) throw new IllegalStateException("Map has not been selected");

        if (gamemodeSelected.isTeamGamemode()) {
            TeamMapData teamMap = (TeamMapData) mapSelected;
            Set<LobbyTeam> teams = getTeamsWithOnlinePlayers();

            // Check team amount is within boundaries
            if (teams.size() < teamMap.minTeams()) {
                throw new IllegalStateException("Not enough teams, at least " + teams + " required!");
            } else if (teams.size() > teamMap.maxTeams()) {
                throw new IllegalStateException("Not enough teams, at least " + teams + " required!");
            }

            // Check all team colors are allowed
            List<TeamColor> colorsAllowed = teamMap.validTeamColors();
            teams.forEach(t -> {
                if (!colorsAllowed.contains(t.teamColor()))
                    throw new IllegalStateException("Team color " + t.teamColor() + " is not allowed on this map!");
            });

        } else {

            // TODO: Check player count is within boundaries

        }

    }

    public void startGameCountdown() {

        gameStarting = true;

        // If the gamemode selected is a team gamemode, set all players with ffa to spectator team
        if (gamemodeSelected.isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {
                // Make all offline players leave their teams
                if (!player.isOnline() && player.getAssignedTeam() != null) {
                    player.playerLeaveTeam();
                }
                // If player is not in team, set their team to spectator
                if (player.getAssignedTeam() == null) {
                    if (!player.isSpectator()) {
                        spectatorTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
                    }
                }
            }
        }

        // Start countdown
        startingCountdown = new GameCountdownTask(gameManager, this, 15);
        startingCountdown.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        gameManager.getWorld().sendMessage(
                Component.text("Game starting in ").color(NamedTextColor.GREEN)
                        .append(Component.text("15 seconds!").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
        );
        for (Player player : gameManager.getWorld().getPlayers()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
        }
    }

    public void cancelGameCountdown (Player playerCause, int cause) {

        gameStarting = false;

        // Cancel countdown task
        startingCountdown.cancel();

        // Clear titles for world
        gameManager.getWorld().clearTitle();

        if (gamemodeSelected.isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {
                if (player.getAssignedTeam() == null && !player.isSpectator()) {
                    ffaTeam.addEntityUUID(player.getOfflinePlayer().getUniqueId());
                }
            }
        }

        for (Player player : gameManager.getWorld().getPlayers()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 100, 1);
        }

        // Send message saying why the game was cancelled
        if (cause == 1) {
            // Player used command
            gameManager.getWorld().sendMessage(
                    Component.text("Game cancelled by " + playerCause.getName() + " using commands!").color(NamedTextColor.YELLOW)
            );
        } else if (cause == 2) {
            // Player left
            gameManager.getWorld().sendMessage(
                    Component.text("Game automatically cancelled as " + playerCause.getName() + " disconnected!").color(NamedTextColor.YELLOW)
            );
        }
    }

    public Location getLobbySpawn() {
        return lobbyTeleport;
    }

    public LobbySidebarManager getSidebarManager () {
        return sidebarManager;
    }

    public void updateClientSidebars () {
        sidebarManager.updateAllClientBoards();
    }

    public void updateServerSidebars () {
        sidebarManager.updateServerBoard();
    }

    public void startGame () {
        gameManager.startGame(gamemodeSelected, getGameContext());
    }

    public void playerJoinServer(Player entity) {

        entity.setGlowing(false);

        // Add player to lobby if not already in player list
        if (!players.containsKey(entity.getUniqueId())) {
            addNewLobbyPlayer(entity);
        }

        LobbyPlayer player = players.get(entity.getUniqueId());
        player.resetPlayer();
        entity.teleport(lobbyTeleport);
        entity.setRespawnLocation(lobbyTeleport, true);
        sidebarManager.addPlayerSidebar(entity);

    }

    public void playerLeaveServer(Player entity) {

        // Cancel countdown timer if player is in game
        LobbyPlayer lbPlayer = getLobbyPlayer(entity);
        if (isGameStarting()) {
            if (isPlaying(lbPlayer)) cancelGameCountdown(entity, 2);
        }

        sidebarManager.removePlayerSidebar(entity);

    }

    private boolean isPlaying (LobbyPlayer player) {
        if (player.getAssignedTeam() != null) return true;
        if (player.isSpectator()) return false;
        return gamemodeSelected.isTeamGamemode(); // Player must be associated with a team in a team gamemode
    }

    public GameSettings gameSettings () {
        return gameSettings;
    }

    public GameContext getGameContext () {
        if (gamemodeSelected == null) return null;
        if (gamemodeSelected.isTeamGamemode()) {
            return new TeamGameContext((TeamMapData) mapSelected, List.copyOf(teams.values()), gameSettings);
        } else {
            return new FFAGameContext(mapSelected, List.copyOf(players.values()), gameSettings);
        }

    }

    public boolean isActive () {
        return active;
    }

    public void restoreTeams(HashMap<UUID, String> playersTeamIds) {
        for (LobbyPlayer lobbyPlayer : getLobbyPlayers()) {
            if (!lobbyPlayer.isOnline()) return;
            UUID playerUUID = lobbyPlayer.getOfflinePlayer().getUniqueId();
            String teamId = playersTeamIds.getOrDefault(playerUUID, "none");
            if (teams.containsKey(teamId)) {
                // Add player to that team
                LobbyTeam team = teams.get(teamId);
                if (team != null) {
                    team.addPlayer(lobbyPlayer);
                }
            }
        }
    }

    public void clearAllTeams() {
        for (LobbyPlayer lobbyPlayer : getLobbyPlayers()) {
            playerLeaveTeam(lobbyPlayer, lobbyPlayer.isSpectator());
        }
    }

    public WeaponPreset getWeaponPreset (WeaponType weaponType) {
        if (weaponType == WeaponType.CREEPER) {
            return creeperPreset;
        }
        else if (weaponType == WeaponType.FLAME) {
            return flamePreset;
        }
        else if (weaponType == WeaponType.XBOW) {
            return xbowPreset;
        }
        return null;
    }

    public void setCreeperPreset (CreeperPreset preset) {
        creeperPreset = preset;
    }

    public void setFlamePreset (FlamePreset preset) {
        flamePreset = preset;
    }

    public void setXbowPreset (XbowPreset preset) {
        xbowPreset = preset;
    }

    public void addTeamCreeperOverrides(String teamId, CreeperPreset preset) {
        if (creeperPresetTeamOverrides.containsKey(teamId) && preset == creeperPreset) {
            creeperPresetTeamOverrides.remove(teamId);
        }
        creeperPresetTeamOverrides.put(teamId, preset);
    }

    public void addTeamFlameOverrides(String teamId, FlamePreset preset) {
        if (flamePresetTeamOverrides.containsKey(teamId) && preset == flamePreset) {
            flamePresetTeamOverrides.remove(teamId);
        }
        flamePresetTeamOverrides.put(teamId, preset);
    }

    public void addTeamXbowOverrides(String teamId, XbowPreset preset) {
        if (xbowPresetTeamOverrides.containsKey(teamId) && preset == xbowPreset) {
            xbowPresetTeamOverrides.remove(teamId);
        }
        xbowPresetTeamOverrides.put(teamId, preset);
    }

    public List<String> getLobbyTeamIds() {
        List<String> lobbyTeamIds = new ArrayList<>();
        for (LobbyTeam team : getTeamsSet()) {
            lobbyTeamIds.add(team.id());
        }
        return lobbyTeamIds;
    }

    public NamedTextColor getColorForTeamId(String teamId) {
        if (getTeams().get(teamId) != null) {
            return getTeams().get(teamId).textColor();
        } else {
            return null;
        }
    }

    public void putTeamOverridesPresetsIntoWeaponManager () {

        CombatManager combatManager = gameManager.combatManager;

        for (String teamId : creeperPresetTeamOverrides.keySet()) {
            combatManager.getWeaponFactory().addTeamCreeperOverrides(teamId, creeperPresetTeamOverrides.get(teamId));
        }
        for (String teamId : flamePresetTeamOverrides.keySet()) {
            combatManager.getWeaponFactory().addTeamFlameOverrides(teamId, flamePresetTeamOverrides.get(teamId));
        }
        for (String teamId : xbowPresetTeamOverrides.keySet()) {
            combatManager.getWeaponFactory().addTeamXbowOverrides(teamId, xbowPresetTeamOverrides.get(teamId));
        }

    }

    public void pasteEventTeams() {

        if (!gameManager.isCBCEventActive()) return;
        CBCEventManager eventManager = gameManager.getEventManager();

        // Go through all lobby players and change their teams
        for (LobbyPlayer lobbyPlayer : getLobbyPlayers()) {

            UUID lobbyPlayerUUID = lobbyPlayer.getOfflinePlayer().getUniqueId();
            if (eventManager.hasPlayer(lobbyPlayerUUID)) {
                CBCEventPlayer player = eventManager.getPlayer(lobbyPlayerUUID);
                if (player.getTeam() != null) {
                    LobbyTeam lobbyTeam = teams.getOrDefault(player.getTeam().getTeamId(), null);
                    if (lobbyTeam != null) {
                        playerJoinTeam(lobbyPlayer, lobbyTeam, true);
                    }
                    else {
                        playerLeaveTeam(lobbyPlayer, false);
                    }
                } else {
                    playerLeaveTeam(lobbyPlayer, false);
                }
            }
            else {
                playerLeaveTeam(lobbyPlayer, false);
            }
        }
    }

    public World getWorld () {
        return gameManager.getWorld();
    }

}
