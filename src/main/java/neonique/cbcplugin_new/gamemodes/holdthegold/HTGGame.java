package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.UpdateBossbarsTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.holdthegold.*;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static neonique.cbcplugin_new.util.StatsUtil.sortPlayerStatList;

public class HTGGame extends TeamGame {

    private final List<HTGTeam> teams = new ArrayList<>();

    // Game related variables
    private int pointsStart = 40;
    private int ticksToScore = 40; // Every ticksToScore ticks (20 ticks = 1 second) someone holds the gold, they score

    private HTGPlayer goldHolder = null;
    private boolean playerScored = false;
    private ArmorStand goldArmorStand = null;
    private Team goldTeam;

    private int teamsToWin = 1;
    private int teamsWon = 0;
    private HTGTeam originalWinningTeam = null;

    // Map related variables
    private HTGMap map;
    private List<HTGSpawn> spawns;
    private Location goldSpawn;

    // Event listeners and tasks
    private HTGStartGameTimer startGameTimer;
    private HTGPlayerNearbyGold pickupGoldTask;
    private HTGPlayerTrackingTask goldTrackingTask;
    private HTGScoreTask scoreTask;

    // Current leaderboards
    private List<PlayerStatObject> topGameScore;
    private List<PlayerStatObject> topKills;
    private List<PlayerStatObject> topGoldScore;

    public HTGGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupGame(CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                          HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Setup map
        setupMap(mapChosen);
        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Set gamemode information
        setGamemode(CBCGamemode.HOLDTHEGOLD);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Setup gamemode game variables
        this.pointsStart = intVars.getOrDefault("pointsStart", 40);
        this.ticksToScore = intVars.getOrDefault("ticksToScore", 40);
        this.teamsToWin = intVars.getOrDefault("teamsToWin", 1);

        // Setup game commands
        setGameCommands(new HTGGameCommands(gameManager, combatManager, this));

        // Create teams and players
        HashMap<String, Location> teamSpawns = map.getTeamSpawns();
        createTeams(teams);
        teleportSpectators();

        for (HTGTeam team : this.teams) {

            // Setup team's spawn
            Location spawn = teamSpawns.get(team.getTeamId());
            if (spawn == null) {
                throw new IllegalArgumentException("Team with id '" + team.getTeamId() + "' does not have a spawn!");
            }
            team.setStartSpawn(spawn);
            team.createSpawnBox();

            // Teleport all players to their spawn
            for (CBCPlayer player : team.getPlayers()) {
                HTGPlayer htgPlayer = (HTGPlayer) player;
                htgPlayer.resetPlayer();
                htgPlayer.teleportPlayerToSpawn(spawn);
            }
        }

        // Update leaderboards
        updateTopGoldScoreList();
        updateTopGameScoreList();
        updateTopKillsList();

        // Create gold team
        goldTeam = CBCPlugin.getPlugin().getServer().getScoreboardManager().getMainScoreboard().registerNewTeam("goldcolor");
        goldTeam.color(NamedTextColor.GOLD);

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(goldTeam);
        }

        // Summon gold
        summonGoldArmorStand(map.getGoldSpawn());

        // Setup sidebar and bossbar
        createUIManagers();

        // Start the countdown timer
        startGameTimer = new HTGStartGameTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
    }

    public void startGame() {

        map.fillBlocksAtEnd();

        // Remove boxes in spawns
        for (HTGTeam team : teams) {
            team.removeSpawnBox();

            // Initialise all players
            for (CBCPlayer player : team.getPlayers()) {
                if (!player.isOnline()) continue;
                player.resetPlayer();
                player.setAlive(true);
                player.setReloadsBySecond(3);
                player.loadout();
                player.setTempImmune(60);
                player.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        // Start important tasks
        pickupGoldTask = new HTGPlayerNearbyGold(this);
        pickupGoldTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 1);
        goldTrackingTask = new HTGPlayerTrackingTask(this);
        goldTrackingTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 4);

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        HTGTeam createdTeam = new HTGTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new HTGPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new HTGSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new HTGBossbarManager(this);
    }

    public void summonGoldArmorStand (Location loc) {

        goldArmorStand = (ArmorStand) getWorld().spawnEntity(loc, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.COMMAND,
                armorStand -> {
                    armorStand.setInvulnerable(true);
                    armorStand.customName(
                            Component.text("⬛ Get the Gold! ⬛").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                    );
                    armorStand.setCustomNameVisible(true);
                });
        goldArmorStand.setInvisible(true);

        Objects.requireNonNull(goldArmorStand.getEquipment()).setHelmet(getGoldHead());

        getGameManager().getCbcScoreboardManager().addTeamEntry(goldArmorStand.getUniqueId().toString(), goldTeam);
        goldArmorStand.addPotionEffect(
            new PotionEffect(PotionEffectType.GLOWING, 1000000, 0, false, false, false)
        );
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        this.map = (HTGMap) generalMap;

        // Get spawns for players and for the gold
        spawns = map.getHTGSpawns();
        goldSpawn = map.getGoldSpawn();

    }

    public void playerPickupGold(HTGPlayer player) {

        goldHolder = player;
        player.pickupGold();

        // Remove the gold armor stand
        goldArmorStand.remove();
        goldArmorStand = null;

        playerScored = false;

        // Send message
        getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("GOLD PICKED UP > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(player.getNameComponent())
                        .append(Component.text(" has picked up the gold!").color(NamedTextColor.WHITE))
        );

        // Play sound to all players
        getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);

        // Start scoring
        if (scoreTask != null) {
            cancelTask(scoreTask);
        }

        scoreTask = new HTGScoreTask(this, player);
        scoreTask.runTaskTimer(CBCPlugin.getPlugin(), ticksToScore, ticksToScore);

        // Update UI elements
        updateServerSidebar();
        updateBossbarManager();
    }

    public void playerDropGold() {

        HTGTeam gTeam = (HTGTeam) goldHolder.getTeam();
        // Reset team score to 7 if they got below 7
        if (!gTeam.isOutOfGame()) {
            if (gTeam.getScore() <= 7) {
                gTeam.setScore(7);
                getGameManager().sendGlobalMessage(
                        Component.text(gTeam.getTeamName() + "'s score has been reset to 7!").color(gTeam.getColor()).decorate(TextDecoration.BOLD)
                );
            } else {
                if (playerScored) {
                    gTeam.setScore(gTeam.getScore() + 1);
                }
            }

        }
        summonGoldArmorStand(goldHolder.getLastValidPosition().add(0.5, 0.0, 0.5));

        // Send message
        getGameManager().sendGlobalMessage(
                Component.newline().append(Component.text("GOLD DROPPED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                        .append(goldHolder.getNameComponent())
                        .append(Component.text(" has dropped the gold!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Play sound to all players
        getGameManager().playGlobalSound(Sound.UI_BUTTON_CLICK, 100, 1);

        cancelTask(scoreTask);
        scoreTask = null;

        goldHolder = null;

        updatePlacements();

        // Update UI elements
        updateServerSidebar();
        updateBossbarManager();
    }

    public void score (HTGPlayer playerScoring) {

        if (goldHolder == null) return; // Check if the gold is currently held
        if (goldHolder != playerScoring) return; // Check to make sure the player holding the gold has not changed

        playerScored = true;

        playerScoring.addPointsScored();
        HTGTeam teamScoring = (HTGTeam) playerScoring.getTeam();
        teamScoring.score();

        if (teamScoring.getScore() > 7) {
            getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, 1);
        } else if (teamScoring.getScore() > 0 ){
            getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, 2);
            if (teamScoring.getScore() == 1) {
                getGameManager().sendGlobalMessage(
                        Component.text(teamScoring.getTeamName() + " is 1 point away from winning!")
                                .color(teamScoring.getColor()).decorate(TextDecoration.BOLD)
                );
            } else {
                getGameManager().sendGlobalMessage(
                        Component.text(teamScoring.getTeamName() + " is " + teamScoring.getScore() + " points away from winning!")
                                .color(teamScoring.getColor()).decorate(TextDecoration.BOLD)
                );
            }
        }

        // Check if team has won game
        if (teamScoring.getScore() == 0) {

            // Don't end game yet
            if (teamsToWin > 1) {

                // Don't end game yet if the team amount to win has not been met
                teamsWon++;
                if (teamsWon == 1) {
                    originalWinningTeam = teamScoring;
                }

                // Say a team's placement
                getGameManager().sendGlobalMessage(
                        Component.newline()
                                .append(Component.text("GAME PLACEMENT > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                                .append(Component.text(teamScoring.getTeamName()).decorate(TextDecoration.BOLD).color(teamScoring.getColor()))
                                .append(Component.text(" has placed " + StringUtil.getPlacementString(teamsWon) + "!").color(NamedTextColor.WHITE))
                                .append(Component.newline())
                );

                // Play sound
                getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 100, 0);

                // Check if team amount to win has been met
                if (teamsWon == teamsToWin) {
                    gameWon(originalWinningTeam);
                    scoreTask.cancel();
                }
                else {
                    // Has not been met yet, so do not end the game
                    teamScoring.teamOutOfGame(teamsWon);
                }

            }
            else {
                gameWon(teamScoring);
                scoreTask.cancel();
            }
        }

        updatePlacements();
        updateTopGoldScoreList();
        updateTopGameScoreList();
        updateServerSidebar();
        updateBossbarManager();
    }

    @Override
    public void gameWon (CBCTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

    }

    public void resetGame() {

        super.resetGame();

        getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(goldTeam.getName());
        goldTeam.unregister();

        cancelTask(scoreTask);
        cancelTask(startGameTimer);
        cancelTask(pickupGoldTask);
        cancelTask(goldTrackingTask);

    }

    public void updatePlacements () {

        List<HTGTeam> teamsByScore = getTeamsByScore();

        int placement = 0;
        int currentScore = -1;
        int i = 0;

        for (HTGTeam team : teamsByScore) {

            boolean tied = false;
            if (team.getScore() > currentScore) {
                placement = i + 1;
                currentScore = team.getScore();
                if (teamsByScore.size() - 1 != i) {
                    if (teamsByScore.get(i + 1).getScore() == currentScore) {
                        tied = true;
                    }
                }
            }
            else if (currentScore == team.getScore()) {
                tied = true;
            }

            team.setPlacement(placement, tied);

            i++;

        }
    }

    public List<HTGTeam> getTeamsByScore() {

        List<HTGTeam> sortedTeamList = new ArrayList<>(teams);
        sortedTeamList.sort(Comparator.comparingInt(HTGTeam::getScore));

        return sortedTeamList;

    }

    public List<HTGSpawn> getSpawns() {
        return spawns;
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new HTGPostGameStats(this);
    }

    @Override
    public void playerJoinServer(Player player) {
        super.playerJoinServer(player);
        // Handle sidebar
        getSidebarManager().addPlayerSidebar(player);
    }

    @Override
    public void playerLeaveServer(Player player) {
        super.playerLeaveServer(player);
        // Handle sidebar
        getSidebarManager().removePlayerSidebar(player);
    }

    public int getStartScore() {
        return pointsStart;
    }

    public HTGMap getMap() {
        return map;
    }

    public Location getGoldLocation () {
        if (goldHolder != null) {
            return goldHolder.getPlayer().getLocation();
        } else {
            if (goldArmorStand != null) {
                if (!goldArmorStand.isDead()) {
                    return goldArmorStand.getLocation();
                } else {
                    return goldSpawn;
                }
            } else {
                return goldSpawn;
            }
        }
    }

    public boolean isGoldHeld () {
        return goldHolder != null;
    }

    public ArmorStand getGoldArmorStand() {
        return goldArmorStand;
    }

    public ItemStack getGoldHead() {
        // Create enchanted gold block
        ItemStack goldBlock = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta goldBlockMeta = goldBlock.getItemMeta();
        goldBlockMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        goldBlock.setItemMeta(goldBlockMeta);
        return goldBlock;
    }

    public HTGPlayer getGoldHolder() {
        return goldHolder;
    }

    public List<HTGTeam> getTeams() {
        return teams;
    }

    public void updateTopKillsList () {
        // Create new top kills list
        topKills = new ArrayList<>();
        for (HTGPlayer player : getHTGPlayers()) {
            // Add player's kills to the list
            topKills.add(new PlayerStatObject(player, player.getKills()));
        }
        // Sort list
        sortPlayerStatList(topKills, true);
    }

    public List<PlayerStatObject> getTopKillsList () {
        return topKills;
    }

    public void updateTopGameScoreList () {
        // Create new top game score list
        topGameScore = new ArrayList<>();
        for (HTGPlayer player : getHTGPlayers()) {
            // Add player's game score to the list
            topGameScore.add(new PlayerStatObject(player, player.getGamePoints()));
        }
        // Sort list
        sortPlayerStatList(topGameScore, true);
    }

    public List<PlayerStatObject> getTopGameScoreList () {
        return topGameScore;
    }

    public void updateTopGoldScoreList () {
        // Create new top gold score list
        topGoldScore = new ArrayList<>();
        for (HTGPlayer player : getHTGPlayers()) {
            // Add player's gold score to the list
            topGoldScore.add(new PlayerStatObject(player, player.getPointsScored()));
        }
        // Sort list
        sortPlayerStatList(topGoldScore, true);
    }

    public List<PlayerStatObject> getTopGoldScoreList () {
        return topGoldScore;
    }

    public Set<HTGPlayer> getHTGPlayers () {
        Set<HTGPlayer> htgPlayers = new HashSet<>();
        for (CBCPlayer player : getPlayers().values()) {
            if (player instanceof HTGPlayer) {
                htgPlayers.add((HTGPlayer) player);
            }
        }
        return htgPlayers;
    }
}
