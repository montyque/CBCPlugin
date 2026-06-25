package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gamemodes.holdthegold.tasks.HTGPlayerNearbyGold;
import neonique.cbcplugin_new.gamemodes.holdthegold.tasks.HTGScoreTask;
import neonique.cbcplugin_new.gamemodes.holdthegold.tasks.HTGStartGameTimer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
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

import java.util.*;

public class HTGGame extends TeamGame<HTGPlayer, HTGMap, HTGTeam> {

    // Game related variables
    private int pointsStart = 40;
    private int ticksToScore = 40; // Every ticksToScore ticks (20 ticks = 1 second) someone holds the gold, they score
    private int finalRunLength = 7;

    private HTGPlayer goldHolder = null;
    private boolean playerScored = false;
    private ArmorStand goldArmorStand = null;
    private CBCScoreboardTeam goldTeam;

    private int teamsToWin = 1;
    private int teamsWon = 0;
    private HTGTeam originalWinningTeam = null;

    // Map related variables
    private List<HTGSpawn> spawns;
    private Location goldSpawn;

    // Event listeners and tasks
    private HTGStartGameTimer startGameTimer;
    private HTGPlayerNearbyGold pickupGoldTask;
    private HTGScoreTask scoreTask;

    public HTGGame (GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.HOLDTHEGOLD;
    }

    @Override
    public HTGTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        return new HTGTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
    }

    @Override
    public HTGPlayer createPlayer(Player playerEntity) {
        return new HTGPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager () {
        return new HTGSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new HTGBossbarManager(this);
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        HTGMap map = (HTGMap) ctx.getMap();
        setupMap(map);

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Setup gamemode game variables
        this.pointsStart = ctx.getIntVars().getOrDefault("pointsStart", 40);
        this.ticksToScore = ctx.getIntVars().getOrDefault("ticksToScore", 40);
        this.teamsToWin = ctx.getIntVars().getOrDefault("teamsToWin", 1);
        this.finalRunLength = Math.min(ctx.getIntVars().getOrDefault("finalRunLength", 7), pointsStart);

        // Setup game commands
        setGameCommands(new HTGGameCommands(this));

        // Create teams and players
        HashMap<String, Location> teamSpawns = map.getTeamSpawns();
        createTeams(ctx.getTeams());
        teleportSpectators();

        for (HTGTeam team : getTeams()) {

            // Setup team's spawn
            Location spawn = teamSpawns.get(team.getTeamId());
            if (spawn == null) {
                throw new IllegalArgumentException("Team with id '" + team.getTeamId() + "' does not have a spawn!");
            }

            team.setStartSpawn(spawn);
            team.createSpawnBox();

            // Teleport all players to their spawn
            for (HTGPlayer player : team.getPlayers()) {
                player.resetPlayer();
                player.teleportPlayerToSpawn(spawn, map.getGoldSpawn());
            }

        }

        // Create gold team
        CBCScoreboardManager sbManager = gameManager.getCbcScoreboardManager();
        CBCScoreboardTeam goldTeam = new CBCScoreboardTeam(sbManager, "goldTeam");
        goldTeam.setColor(NamedTextColor.GOLD);
        sbManager.registerTeam(goldTeam);

        // Summon gold
        summonGoldArmorStand(map.getGoldSpawn());

        // Setup sidebar and bossbar
        createUIManagers();

        // Start the countdown timer
        startGameTimer = new HTGStartGameTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

    }

    @Override
    public void gameWon (HTGTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (HTGPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

    }

    @Override
    public void resetGame () {

        super.resetGame();

        getGameManager().getCbcScoreboardManager().unregisterTeam(goldTeam);

        cancelTask(scoreTask);
        cancelTask(startGameTimer);
        cancelTask(pickupGoldTask);

    }

    @Override
    public PostGameStats getPostGameStats () {
        return new HTGPostGameStats(this);
    }

    public void startGame () {

        getMap().fillBlocksAtEnd();

        // Remove boxes in spawns
        for (HTGTeam team : getTeams()) {
            team.removeSpawnBox();

            // Initialise all players
            for (HTGPlayer player : team.getPlayers()) {
                if (!player.isOnline()) continue;
                player.playerSetup(2);
                player.setTempImmune(60);
                player.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }

        // Start important tasks
        pickupGoldTask = new HTGPlayerNearbyGold(this);
        pickupGoldTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 1);

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

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

        goldTeam.addEntityUUID(goldArmorStand.getUniqueId());
        goldArmorStand.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1000000, 0, false, false, false));

    }

    public void setupMap (HTGMap map) {

        super.setupMap(map);

        // Get spawns for players and for the gold
        spawns = map.getHTGSpawns();
        goldSpawn = map.getGoldSpawn();

    }

    public void playerPickupGold (HTGPlayer player) {

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

        HTGTeam team = getPlayerTeam(goldHolder);

        // Reset team score to finalRunLength if they got below 7
        if (!team.isOutOfGame()) {
            if (team.getScore() <= finalRunLength) {
                team.setScore(finalRunLength);
                getGameManager().sendGlobalMessage(
                        Component.text(team.getTeamName() + "'s score has been reset to " + finalRunLength + "!").color(team.getColor()).decorate(TextDecoration.BOLD)
                );
            } else {
                if (playerScored) {
                    team.setScore(team.getScore() + 1);
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
        HTGTeam teamScoring = getPlayerTeam(playerScoring);
        teamScoring.score();

        if (teamScoring.getScore() > finalRunLength) {
            getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, 1);
        } else if (teamScoring.getScore() > 0) {
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
        updateServerSidebar();
        updateBossbarManager();
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
        List<HTGTeam> sortedTeamList = getTeams();
        sortedTeamList.sort(Comparator.comparingInt(HTGTeam::getScore));
        return sortedTeamList;
    }

    public List<HTGSpawn> getSpawns() {
        return spawns;
    }

    public int getStartScore() {
        return pointsStart;
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

    public ArmorStand getGoldArmorStand () {
        return goldArmorStand;
    }

    public ItemStack getGoldHead () {
        // Create enchanted gold block
        ItemStack goldBlock = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta goldBlockMeta = goldBlock.getItemMeta();
        goldBlockMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        goldBlock.setItemMeta(goldBlockMeta);
        return goldBlock;
    }

    public HTGPlayer getGoldHolder () {
        return goldHolder;
    }

}
