package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.enums.ResourcePackFont;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gameobjects.*;
import neonique.cbcplugin_new.listeners.lobby.MenuClickEvent;
import neonique.cbcplugin_new.listeners.lobby.PlayerDamageListener;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.cbcevents.CBCEventPlayer;
import neonique.cbcplugin_new.tasks.lobbytasks.GameCountdownTask;
import neonique.cbcplugin_new.tasks.lobbytasks.LobbySidebarManagerTask;
import neonique.cbcplugin_new.tasks.lobbytasks.PlayerSafetyTask;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.setTextFont;

public class Lobby {

    private final GameManager gameManager;

    boolean active = false;

    // Lobby variables
    private Location lobbyTeleport;

    // Map selected
    private CBCGamemode gamemodeSelected;
    private CBCMap mapSelected;

    // Game variables
    private HashMap<String, Integer> gameInts;
    private HashMap<String, Boolean> gameBools;
    private HashMap<String, String> gameStrings;

    private HashMap<String, Integer> defaultGameInts = new HashMap<>();
    private HashMap<String, Boolean> defaultGameBools = new HashMap<>();
    private HashMap<String, String> defaultGameStrings = new HashMap<>();

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
    private HashMap<UUID, LobbyPlayer> players = new HashMap<>();
    private LinkedHashMap<String, LobbyTeam> teams = new LinkedHashMap<>();
    private Team spectatorTeam;
    private Team ffaTeam;

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

        // Setting all default game variables
        defaultGameBools.put("beaconHeads", false);
        defaultGameBools.put("nightVisionDisabled", false);
        defaultGameBools.put("doDayCycle", false);
        defaultGameBools.put("globalKillsEnabled", true);
        defaultGameBools.put("restoreTeamsAfterGame", true);
        defaultGameStrings.put("dayTime", "dusk");

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

        gameBools = new HashMap<>(defaultGameBools);
        gameInts = new HashMap<>(defaultGameInts);
        gameStrings = new HashMap<>(defaultGameStrings);

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
        teams.put("red", new LobbyTeam(gameManager, this, "01", "red",
                "Red", "R", Material.RED_STAINED_GLASS, NamedTextColor.RED, ChatColor.RED));
        teams.put("blue", new LobbyTeam(gameManager, this, "02", "blue",
                "Blue", "B", Material.BLUE_STAINED_GLASS, NamedTextColor.BLUE, ChatColor.BLUE));
        teams.put("green", new LobbyTeam(gameManager, this, "03", "green",
                "Green", "G", Material.LIME_STAINED_GLASS, NamedTextColor.GREEN, ChatColor.GREEN));
        teams.put("yellow", new LobbyTeam(gameManager, this, "04", "yellow",
                "Yellow", "Y", Material.YELLOW_STAINED_GLASS, NamedTextColor.YELLOW, ChatColor.YELLOW));
        teams.put("cyan", new LobbyTeam(gameManager, this, "05", "cyan",
                "Cyan", "C", Material.CYAN_STAINED_GLASS, NamedTextColor.AQUA, ChatColor.AQUA));
        teams.put("orange", new LobbyTeam(gameManager, this, "06", "orange",
                "Orange", "O", Material.ORANGE_STAINED_GLASS, NamedTextColor.GOLD, ChatColor.GOLD));
        teams.put("magenta", new LobbyTeam(gameManager, this, "07", "magenta",
                "Magenta", "M", Material.MAGENTA_STAINED_GLASS, NamedTextColor.LIGHT_PURPLE, ChatColor.LIGHT_PURPLE));
        teams.put("purple", new LobbyTeam(gameManager, this, "08", "purple",
                "Purple", "P", Material.PURPLE_STAINED_GLASS, NamedTextColor.DARK_PURPLE, ChatColor.DARK_PURPLE));

        // Creating spectator and FFA scoreboard teams
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        ffaTeam = scoreboard.registerNewTeam("09ffaLobby");
        ffaTeam.setAllowFriendlyFire(true); // Allow friendly fire
        ffaTeam.prefix(Component.text(" ■ ").color(NamedTextColor.WHITE));
        // If there's a team prefix, set it
        spectatorTeam = scoreboard.registerNewTeam("10spectatorLobby");
        spectatorTeam.setAllowFriendlyFire(true); // Allow friendly fire
        spectatorTeam.prefix(Component.text(" □ ").color(NamedTextColor.WHITE));

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(ffaTeam);
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(spectatorTeam);
        }

        // Create lobby players for all online players
        for (Player player : CBCPlugin.getPlugin().getServer().getOnlinePlayers()) {
            addNewLobbyPlayer(player);
        }

        // Activate scoreboard manager
        gameManager.getCbcScoreboardManager().activate();

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

    public void deactivate() {

        active = false;

        // Deactivate listeners and tasks
        InventoryClickEvent.getHandlerList().unregister(menuClickEvent);
        EntityDamageEvent.getHandlerList().unregister(playerDamageListener);

        playerSafetyTask.cancel();

        sidebarManager.removeSidebar();
        lobbySidebarManagerTask.cancel();

        // Unregister all teams
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(ffaTeam.getName());
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(spectatorTeam.getName());

        ffaTeam.unregister();
        spectatorTeam.unregister();

        for (LobbyTeam lobbyTeam : teams.values()) {
            // Remove all team objects
            lobbyTeam.removeTeam();
        }

        players.clear();
        teams.clear();

        ffaTeam = null;
        spectatorTeam = null;

        imageMaps.deleteItemFrames();

        // Check if practice is off, or if there is no game, then disable the scoreboard manager
        if (!gameManager.practiceManager.isEnabled() && gameManager.getCurrentGame() == null) {
            gameManager.getCbcScoreboardManager().deactivate();
        }

    }

    public void randomizeTeams(Set<LobbyTeam> teamsSelected) {

        List<LobbyPlayer> playersToRandomize = new ArrayList<>();
        // Make all players leave their teams
        for (LobbyPlayer player : players.values()) {
            // Check that player is not a spectator
            if (player.isSpectator()) {
                continue;
            }
            playerLeaveTeam(player, false); // Make player leave team
            // Check that player is online
            if (!player.isOnline()) {
                continue;
            }
            playersToRandomize.add(player);
        }

        // Convert set of teams into list
        List<LobbyTeam> teamsRandomizing = new ArrayList<>(teamsSelected);

        // Shuffle list of players and list of teams
        Collections.shuffle(teamsRandomizing);
        Collections.shuffle(playersToRandomize);

        // Iterate through players and distribute them into teams
        int playersGoneThrough = 0;
        for (LobbyPlayer player : playersToRandomize) {
            LobbyTeam teamToJoin = teamsRandomizing.get(playersGoneThrough % teamsRandomizing.size());
            playerJoinTeam(player, teamToJoin, false);
            playersGoneThrough++;
        }

        // Send message to confirm randomizing teams is finished
        gameManager.getWorld().sendMessage(Component.text(
                playersToRandomize.size() + " players have been randomized into " + teamsRandomizing.size() + " teams!"
        ).color(NamedTextColor.GREEN));

        // Update sidebars
        updateClientSidebars();

    }

    public void openTeamRandomizeMenu(Player user) {

        int minTeams;
        int maxTeams;

        // Make sure gamemode is team gamemode
        if (gamemodeSelected != null && mapSelected != null) {
            GamemodeOptions gamemode = gameManager.getGamemodes().get(gamemodeSelected);
            if (!gamemode.isTeamGamemode()) return; else {
                if (mapSelected.getMinTeams() != null) {
                    minTeams = mapSelected.getMinTeams();
                    maxTeams = mapSelected.getMaxTeams();
                } else return;
            }
        } else {
            minTeams = 2;
            maxTeams = 8;
        }

        List<String> teamsAllowed = null;
        if (mapSelected != null) {
            if (mapSelected.getTeamsAllowed() != null) {
                teamsAllowed = mapSelected.getTeamsAllowed();
            }
        }

        LinkedHashMap<String, LobbyTeam> teamsAvailable = new LinkedHashMap<>();
        for (String teamId : teams.keySet().toArray(new String[0])) {
            if (teamsAllowed != null) {
                if (teamsAllowed.contains(teamId)) {
                    teamsAvailable.put(teamId, teams.get(teamId));
                }
            }
            else {
                teamsAvailable.put(teamId, teams.get(teamId));
            }
        }

        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Randomize Teams").decorate(TextDecoration.BOLD));

        int itemSlot = 0;
        int randomisingTeams = 0;
        for (String teamId : teamsAvailable.keySet().toArray(new String[0])) {

            LobbyTeam team = teamsAvailable.get(teamId);

            // Create item
            ItemStack item = team.getItem();

            ItemMeta itemMeta = item.getItemMeta();
            if (itemSlot < maxTeams) {
                // Set randomising to true
                itemMeta.addEnchant(Enchantment.THORNS, 1, false);
                Component itemLore;
                itemLore = Component.text("Click to remove from randomisation").color(NamedTextColor.DARK_GREEN)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                List<Component> loreList = new ArrayList<>();
                loreList.add(itemLore);
                itemMeta.lore(loreList);

                item.setItemMeta(itemMeta);

                randomisingTeams++;
            } else {
                Component itemLore;
                itemLore = Component.text("Click to add to randomisation").color(NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                List<Component> loreList = new ArrayList<>();
                loreList.add(itemLore);

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

        LinkedHashMap<CBCGamemode, GamemodeOptions> gamemodeList = gameManager.getGamemodes();
        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Select Gamemode").decorate(TextDecoration.BOLD));

        int teamGamemodeCount = 0;
        int ffaGamemodeCount = 0;

        for (CBCGamemode gamemode : gamemodeList.keySet()) {
            GamemodeOptions gamemodeVariables = gamemodeList.get(gamemode);

            ItemStack item = gamemodeVariables.getGamemodeIcon();
            ItemMeta itemMeta = getGamemodeItemMeta(item, gamemodeVariables);

            item.setItemMeta(itemMeta);

            // Sort the inventory to put team gamemodes on the top row and free for all gamemodes on the middle row
            if (gamemodeVariables.isTeamGamemode()) {
                gui.setItem(teamGamemodeCount, item);
                teamGamemodeCount++;
            } else {
                gui.setItem(ffaGamemodeCount + 9, item);
                ffaGamemodeCount++;
            }
        }
        user.openInventory(gui);
    }

    private static @NotNull ItemMeta getGamemodeItemMeta(ItemStack item, GamemodeOptions gamemodeVariables) {

        ItemMeta itemMeta = item.getItemMeta();
        // Set item title
        Component itemTitle = Component.text(gamemodeVariables.getGamemodeName()).color(gamemodeVariables.getColor())
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        Component itemLore;
        if (gamemodeVariables.isTeamGamemode()) {
            int minTeams = gamemodeVariables.getMinTeams();
            int maxTeams = gamemodeVariables.getMaxTeams();
            itemLore = Component.text(minTeams + "-" + maxTeams + " teams").color(NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        } else {
            if (gamemodeVariables.getMinPlayers() > 1) {
                itemLore = Component.text("Free for all, " + gamemodeVariables.getMinPlayers() + "+ players").color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            }
            else {
                itemLore = Component.text("Free for all").color(NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            }
        }
        List<Component> loreList = new ArrayList<>();
        loreList.add(itemLore);
        itemMeta.lore(loreList);
        return itemMeta;

    }

    public void openMapMenu(Player user, CBCGamemode gamemode, GamemodeOptions gamemodeOptions) {

        if (!gameManager.getGamemodeAndMapList().containsKey(gamemode)) return;

        // Setting gamemode variables which will be used in displays
        TextColor gmColor = gamemodeOptions.getColor();
        String gmName = gamemodeOptions.getGamemodeName();

        List<CBCMap> mapList = gameManager.getGamemodeAndMapList().get(gamemode);
        Inventory gui = Bukkit.createInventory(user, 27, Component.text("Select Map - " + gmName)
                .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        int mapCount = 0;

        for (CBCMap map : mapList) {
            ItemStack item = new ItemStack(map.getBlockSymbol());
            ItemMeta itemMeta = item.getItemMeta();
            // Set item title
            Component itemTitle = Component.text(map.getMapName()).color(gmColor)
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            itemMeta.displayName(itemTitle);
            item.setItemMeta(itemMeta);

            if (map.isMapRush()) {
                gui.setItem(26, item);
            }
            else {
                gui.setItem(mapCount, item);
                mapCount++;
            }
        }
        user.openInventory(gui);
    }

    public void setGamemodeAndMapSelected (CBCGamemode gamemode, CBCMap map) {

        GamemodeOptions gamemodeVariables = gameManager.getGamemodes().get(gamemode);

        // Check if gamemode has changed
        if (gamemodeSelected != gamemode) {

            // Reset variables
            HashMap<String, Integer> newGameInts = new HashMap<>();
            HashMap<String, Boolean> newGameBools = new HashMap<>();
            HashMap<String, String> newGameStrings = new HashMap<>();

            for (String varId : defaultGameBools.keySet()) {
                if (gameBools.containsKey(varId)) {
                    newGameBools.put(varId, gameBools.get(varId));
                } else {
                    newGameBools.put(varId, defaultGameBools.get(varId));
                }
            }
            for (String varId : defaultGameInts.keySet()) {
                if (gameInts.containsKey(varId)) {
                    newGameInts.put(varId, gameInts.get(varId));
                } else {
                    newGameInts.put(varId, defaultGameInts.get(varId));
                }
            }
            for (String varId : defaultGameStrings.keySet()) {
                if (gameStrings.containsKey(varId)) {
                    newGameStrings.put(varId, gameStrings.get(varId));
                } else {
                    newGameInts.put(varId, defaultGameInts.get(varId));
                }
            }

            gameInts = newGameInts;
            gameBools = newGameBools;
            gameStrings = newGameStrings;

            // Add gamemode default values
            for (String varId : gamemodeVariables.getDefaultGameBooleans().keySet()) {
                gameBools.put(varId, gamemodeVariables.getDefaultGameBooleans().get(varId));
            }
            for (String varId : gamemodeVariables.getDefaultGameInts().keySet()) {
                gameInts.put(varId, gamemodeVariables.getDefaultGameInts().get(varId));
            }
            for (String varId : gamemodeVariables.getDefaultGameStrings().keySet()) {
                gameStrings.put(varId, gamemodeVariables.getDefaultGameStrings().get(varId));
            }
        }

        gamemodeSelected = gamemode;
        mapSelected = map;

        // Send message to say what map has been selected
        Component message = Component.text().content(gamemodeVariables.getGamemodeName() + " - " + map.getMapName())
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

        String imageFileName = gameManager.getImageFile(gamemode, map.getMapId());
        if (imageFileName != null) {
            imageMaps.setImage(imageFileName);
        }
    }

    public CBCGamemode getGamemodeSelected () {
        return gamemodeSelected;
    }

    public CBCMap getMapSelected () {
        return mapSelected;
    }

    public void addNewLobbyPlayer(Player player) {

        System.out.println("New player added to lobby: " + player.getName());

        player.playerListName(null);

        players.put(player.getUniqueId(), new LobbyPlayer(gameManager, this, player.getUniqueId()));
        gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), ffaTeam);
        gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), ffaTeam);

        // Remove potion effects
        if (gameManager.isPracticeActive()) {
            if (!gameManager.hasPlayer(player)) {
                for (PotionEffect effect : player.getActivePotionEffects())
                    player.removePotionEffect(effect.getType());
                player.setGameMode(GameMode.ADVENTURE);
                player.teleport(lobbyTeleport);
                player.getInventory().clear();
                player.updateInventory();
                player.setBedSpawnLocation(lobbyTeleport, true);
                player.setHealth(20);
                player.removeScoreboardTag("NVDisable");
            }
        } else {
            for (PotionEffect effect : player.getActivePotionEffects())
                player.removePotionEffect(effect.getType());
            player.setGameMode(GameMode.ADVENTURE);

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
            player.getInventory().clear();
            player.updateInventory();
            player.setBedSpawnLocation(lobbyTeleport, true);
            player.setHealth(20);
            player.removeScoreboardTag("NVDisable");
        }
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
        if (playerTeam == null) {
            return;
        }

        playerTeam.removePlayer(player);
        if (!spectator) {
            gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), ffaTeam);
        } else {
            gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), spectatorTeam);
        }
    }

    public void playerToggleSpectator(LobbyPlayer player) {

        if (player.isSpectator()) {
            // Make player not spectator
            playerUnsetSpectator(player);
        } else {
            // Make player spectator
            playerSetSpectator(player);
        }
    }

    public void playerSetSpectator(LobbyPlayer player) {

        if (!player.isOnline()) return;

        playerLeaveTeam(player, true);
        gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), spectatorTeam);
        player.setSpectator();
        player.getPlayer().sendMessage(
                Component.text("You are now a spectator. This means you cannot be added to any" +
                                " teams during randomization. To get out of spectator mode, use ").color(NamedTextColor.GOLD)
                        .append(Component.text("/lobby spectator toggle").color(NamedTextColor.YELLOW))
        );
    }

    public void playerUnsetSpectator(LobbyPlayer player) {

        if (!player.isOnline()) return;

        gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), ffaTeam);

        player.setNoSpectator();
        player.getPlayer().sendMessage(
                Component.text("You are no longer a spectator. To go back into spectator mode, use ").color(NamedTextColor.GOLD)
                        .append(Component.text("/lobby spectator toggle").color(NamedTextColor.YELLOW))
        );
    }

    public Collection<LobbyTeam> getTeamsSet () {
        return teams.values();
    }

    public LinkedHashMap<String, LobbyTeam> getTeams () {
        return teams;
    }

    public Set<Player> getPlayerEntities() {
        Set<Player> playerEntities = new HashSet<>();
        for (UUID playerUUID : players.keySet()) {
            Player player = CBCPlugin.getPlugin().getServer().getPlayer(playerUUID);
            if (player == null) continue;
            playerEntities.add(player);
        }
        return playerEntities;
    }

    public Collection<LobbyPlayer> getLobbyPlayers() {
        return players.values();
    }

    public Set<LobbyPlayer> getLobbyPlayersPlayingAndOnline() {
        Set<LobbyPlayer> playerList = new HashSet<>();
        for (LobbyPlayer player : getLobbyPlayers()) {
            if (!player.isSpectator() && player.isOnline()) {
                playerList.add(player);
            }
        }
        return playerList;
    }

    public LobbyPlayer getLobbyPlayer(Player player) {
        return players.getOrDefault(player.getUniqueId(), null);
    }

    public void replacePlayerEntityKey(Player origin, Player newPlayer) {
        if (players.containsKey(origin.getUniqueId())) {
            LobbyPlayer lobbyPlayerInstance = players.get(origin.getUniqueId());
            players.remove(origin.getUniqueId());
            players.put(newPlayer.getUniqueId(), lobbyPlayerInstance);
            lobbyPlayerInstance.setNewPlayer(newPlayer);
        }
    }

    public Set<LobbyTeam> getTeamsWithOnlinePlayers () {
        Set<LobbyTeam> teamsWithOnlinePlayers = new HashSet<>();
        for (LobbyTeam team : teams.values()) {
            if (team.getOnlinePlayers().size() > 0) {
                teamsWithOnlinePlayers.add(team);
            }
        }
        return teamsWithOnlinePlayers;
    }

    public boolean isGameStarting() {
        return gameStarting;
    }

    public int canStartGame() {
        // Function that checks whether the game can be started or not
        // Check if gamemode and map have been selected
        if (gamemodeSelected == null) return 1;
        if (mapSelected == null) return 2;
        // Check if there are the required amount of teams
        GamemodeOptions gamemodeVars = gameManager.getGamemodes().get(gamemodeSelected);
        if (gamemodeVars == null) return 3;
        if (gamemodeVars.isTeamGamemode()) {
            // Get the amount of teams with an online player in it
            int teamsWithOnlinePlayers = getTeamsWithOnlinePlayers().size();
            int minTeams;
            int maxTeams;
            if (mapSelected.getMinTeams() == null) {
                minTeams = gamemodeVars.getMinTeams();
                maxTeams = gamemodeVars.getMaxTeams();
            } else {
                minTeams = mapSelected.getMinTeams();
                maxTeams = mapSelected.getMaxTeams();
            }
            // Check if teams fit within the boundaries
            if (teamsWithOnlinePlayers < minTeams) return 4;
            if (teamsWithOnlinePlayers > maxTeams) return 5;
            // Check if no teams are disallowed
            if (mapSelected.getTeamsAllowed() != null) {
                for (LobbyTeam team : getTeamsWithOnlinePlayers()) {
                    if (!mapSelected.getTeamsAllowed().contains(team.getTeamId())) {
                        return 6;
                    }
                }
            }
        } else {
            // Check if in free for all gamemode, minimum amount of players is not met
            if (getLobbyPlayersPlayingAndOnline().size() < gamemodeVars.getMinPlayers()) {
                return 7;
            }
        }
        return 0;
    }

    public void startGameCountdown() {
        gameStarting = true;
        // If the gamemode selected is a team gamemode, set all players with ffa to spectator team
        if (gameManager.getGamemodes().get(gamemodeSelected).isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {
                // Make all offline players leave their teams
                if (!player.isOnline() && player.getAssignedTeam() != null) {
                    player.playerLeaveTeam();
                }
                // If player is not in team, set their team to spectator
                if (player.getAssignedTeam() == null) {
                    if (!player.isSpectator()) {
                        gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), spectatorTeam);
                    }
                }
            }
        }

        // Close all inventories that players are looking at
        for (Player player : getPlayerEntities()) {
            if (player.isOnline()) player.getOpenInventory().close();
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

    public void cancelGameCountdown(Player playerCause, int cause) {
        gameStarting = false;
        // If the gamemode selected is a team gamemode, set all players without a team that are not spectator to ffa team
        if (gameManager.getGamemodes().get(gamemodeSelected).isTeamGamemode()) {
            for (LobbyPlayer player : players.values()) {
                // If player is not in team, set their team to not spectator
                if (player.getAssignedTeam() == null && !player.isSpectator()) {
                    gameManager.getCbcScoreboardManager().addTeamEntry(player.getName(), ffaTeam);
                }
            }
        }

        // Cancel countdown
        startingCountdown.cancel();

        // Clear titles for world
        gameManager.getWorld().clearTitle();

        for (Player player : gameManager.getWorld().getPlayers()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 100, 1);
        }

        // Send message saying why the game was cancelled
        if (cause == 1) {
            // Player used command
            gameManager.getWorld().sendMessage(
                    Component.text("Game cancelled by " + playerCause.getName() + " using commands!").color(NamedTextColor.YELLOW)
            );
        }
        else if (cause == 2) {
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
        gameManager.startGame(gamemodeSelected, mapSelected);
    }

    public void playerJoinServer(Player player) {

        player.setGlowing(false);
        player.removeScoreboardTag("NVDisable");

        // Handle lobby sidebar
        sidebarManager.addPlayerSidebar(player);

        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(lobbyTeleport);
        player.getInventory().clear();
        player.updateInventory();
        player.setBedSpawnLocation(lobbyTeleport, true);
        player.setHealth(20);

        // Add night vision back
        player.removeScoreboardTag("NVDisable");

        System.out.println(getPlayerEntities().size());
        for (Player playerEntity : getPlayerEntities()) {
            if (playerEntity.getUniqueId().equals(player.getUniqueId())) {
                replacePlayerEntityKey(playerEntity, player);
                return;
            }
        }

        // If not already in player list
        addNewLobbyPlayer(player);

    }

    public void playerLeaveServer(Player player) {

        // Handle lobby sidebar
        getSidebarManager().removePlayerSidebar(player);

        UUID offlinePlayerId = player.getUniqueId();

        for (Player playerEntity : getPlayerEntities()) {
            if (playerEntity.getUniqueId().equals(offlinePlayerId)) {
                LobbyPlayer lbPlayer = getLobbyPlayer(playerEntity);

                // Cancel countdown if game is starting and player that left is a player
                if (isGameStarting()) {
                    if (gameManager.getGamemodes().get(getGamemodeSelected()).isTeamGamemode()) {
                        if (lbPlayer.getAssignedTeam() != null) {
                            cancelGameCountdown(playerEntity, 2);
                        }
                    } else {
                        if (!lbPlayer.isSpectator()) {
                            cancelGameCountdown(playerEntity, 2);
                        }
                    }
                }

                replacePlayerEntityKey(playerEntity, player);
                return;
            }
        }
    }

    public Set<String> getAllGameVarKeys () {
        Set<String> allVarKeys = new HashSet<>(gameBools.keySet());
        allVarKeys.addAll(gameInts.keySet());
        allVarKeys.addAll(gameStrings.keySet());
        return allVarKeys;
    }

    public List<String> getGameVarTabCompletions (String gameVar) {

        List<String> gameVarTabCompletions = new ArrayList<>();

        if (gameBools.containsKey(gameVar)) {
            gameVarTabCompletions.add("true");
            gameVarTabCompletions.add("false");
        }
        else if (gameVar.equals("dayTime")) {
            gameVarTabCompletions.add("dawn");
            gameVarTabCompletions.add("noon");
            gameVarTabCompletions.add("dusk");
            gameVarTabCompletions.add("midnight");
        }

        return gameVarTabCompletions;
    }

    public Boolean getBooleanGameVar (String gameVar) {
        return gameBools.getOrDefault(gameVar, null);
    }

    public Integer getIntGameVar (String gameVar) {
        return gameInts.getOrDefault(gameVar, null);
    }

    public String getStringGameVar (String gameVar) {
        return gameStrings.getOrDefault(gameVar, null);
    }

    public void setBooleanGameVar (String gameVar, boolean value) {
        gameBools.put(gameVar, value);
    }

    public void setIntGameVar (String gameVar, int value) {
        gameInts.put(gameVar, value);
    }

    public void setStringGameVar (String gameVar, String value) {
        gameStrings.put(gameVar, value);
    }

    public HashMap<String, Boolean> getBoolVars() {
        return gameBools;
    }

    public HashMap<String, Integer> getIntVars() {
        return gameInts;
    }

    public HashMap<String, String> getStringVars() {
        return gameStrings;
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
            lobbyTeamIds.add(team.getTeamId());
        }
        return lobbyTeamIds;
    }

    public NamedTextColor getColorForTeamId(String teamId) {
        if (getTeams().get(teamId) != null) {
            return getTeams().get(teamId).getColor();
        } else {
            return null;
        }
    }

    public void putTeamOverridesPresetsIntoWeaponManager () {

        CombatManager combatManager = gameManager.combatManager;

        for (String teamId : creeperPresetTeamOverrides.keySet()) {
            combatManager.addTeamCreeperOverrides(teamId, creeperPresetTeamOverrides.get(teamId));
        }
        for (String teamId : flamePresetTeamOverrides.keySet()) {
            combatManager.addTeamFlameOverrides(teamId, flamePresetTeamOverrides.get(teamId));
        }
        for (String teamId : xbowPresetTeamOverrides.keySet()) {
            combatManager.addTeamXbowOverrides(teamId, xbowPresetTeamOverrides.get(teamId));
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
