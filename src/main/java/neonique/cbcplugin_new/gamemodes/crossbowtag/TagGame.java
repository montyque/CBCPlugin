package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.TagNoMove;
import neonique.cbcplugin_new.listeners.gamemodes.TagTeleportListener;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.gamemodes.crossbowtag.tasks.TagNextRoundTimer;
import neonique.cbcplugin_new.gamemodes.crossbowtag.tasks.TagRoundTimer;
import neonique.cbcplugin_new.gamemodes.crossbowtag.tasks.TagStartRoundTimer;
import neonique.cbcplugin_new.gamemodes.crossbowtag.tasks.TaggerReleaseTimer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class TagGame extends TeamGame<TagPlayer, TagMap, TagTeam> {

    // Game variables
    private int roundLength = 150; // Round length in seconds
    private int roundsPerTeam = 1; // Amount of rounds each team gets to be tagger

    // Game info
    private List<TagTeam> taggerOrder;
    private int MAX_EVADER_KILL_POINTS = 50;
    private int MIN_EVADER_KILL_POINTS = 15;
    private int taggerRespawnTimer = 4;
    private int turnNum = 1; // Current turn amount

    private int maxScorePerSecond = 1;

    // Round info
    private TagTeam taggers;
    private int roundNumber = 0;
    private int roundTimer = 0;
    private int roundSurviveTimer = 0;
    private int taggerReleaseTimer = 0;
    private boolean roundInPlay = false;
    private boolean finalGlow = false;
    private boolean startRoundTimer = false;

    // Round points info
    private int currentEvaderKillValue = 0;
    private int survivorPointsAdded = 0; // Current amount of points

    // Points info
    private int MAX_POINTS_FOR_SURVIVAL = 150; // Surviving entire round gives you this amount of points
    private int SURVIVAL_BONUS = 10;
    private int MAX_POINTS_FOR_WIPEOUT = 150; // Killing an entire round gives you this many points
    private int WIPEOUT_BONUS = 20;

    // Map related variables
    private HashMap<String, Set<Location>> teamEvaderSpawns;
    private HashMap<String, Set<Location>> teamTaggerSpawns;
    private List<Set<Location>> randomEvaderSpawns;
    private Set<Location> equalTaggerSpawns;

    // Listeners
    private TagNoMove noMoveListener;
    private TagTeleportListener teleportListener;

    // Tasks
    private TaggerReleaseTimer taggerReleaseTimerTask;
    private TagRoundTimer roundTimerTask;

    private Component footerComponent;


    // !!! Add radar locations maybe?

    public TagGame(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.CBCTAG;
    }

    @Override
    public TagPlayer createPlayer(Player playerEntity) {
        return new TagPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public TagTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        return new TagTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
    }

    public GameSidebarManager createSidebarManager () {
        return new TagSidebarManager(getGameManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new TagBossbarManager(this);
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        TagMap map = (TagMap) ctx.getMap();
        setupMap(map);

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        setGameCommands(new TagGameCommands(this));

        // Setup gamemode game variables
        this.roundLength = ctx.getIntVars().getOrDefault("roundLength", 150);
        this.roundsPerTeam = ctx.getIntVars().getOrDefault("roundsPerTeam", 1);
        this.taggerRespawnTimer = ctx.getIntVars().getOrDefault("taggerRespawnTimer", 4);

        // Setup point game variables
        this.MAX_POINTS_FOR_SURVIVAL = ctx.getIntVars().getOrDefault("MAX_POINTS_FOR_SURVIVAL", 150);
        this.MAX_POINTS_FOR_WIPEOUT = ctx.getIntVars().getOrDefault("MAX_POINTS_FOR_WIPEOUT", 150);
        this.MAX_EVADER_KILL_POINTS = ctx.getIntVars().getOrDefault("MAX_EVADER_KILL_POINTS", 35);
        this.MIN_EVADER_KILL_POINTS = ctx.getIntVars().getOrDefault("MIN_EVADER_KILL_POINTS", 10);
        this.WIPEOUT_BONUS = ctx.getIntVars().getOrDefault("WIPEOUT_BONUS", 25);
        this.SURVIVAL_BONUS = ctx.getIntVars().getOrDefault("SURVIVAL_BONUS", 10);

        // Setup teams/players
        createTeams(ctx.getTeams());
        teleportSpectators();

        // Set team spawns
        for (TagTeam team : getTeams()) {

            String teamId = team.getTeamId();

            // Set tagger spawns
            if (map.isTaggerSpawnsEqual()) {
                team.setTaggerSpawns(equalTaggerSpawns);
            }
            else {
                team.setTaggerSpawns(teamTaggerSpawns.get(teamId));
            }

            // Set evader spawns if the spawns are not random
            if (!map.isEvaderSpawnsRandom()) {
                team.setEvaderSpawns(teamEvaderSpawns.get(teamId));
            }

            // Get maximum amount of players on one team
            if (maxScorePerSecond < team.getPlayers().size()) {
                maxScorePerSecond = team.getPlayers().size();
            }
        }

        // Choose the tagger order
        taggerOrder = new ArrayList<>(getTeams());
        Collections.shuffle(taggerOrder);

        noMoveListener = new TagNoMove(gameManager, this);

        // Make taggers unable to teleport
        teleportListener = new TagTeleportListener(this);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(teleportListener, CBCPlugin.getPlugin());

        updatePlacements();
        // Create Bossbar/Sidebar managers
        createUIManagers();

        // Start first round
        setupRound();
    }

    @Override
    public void gameWon (TagTeam team) {

        super.gameWon(team);

        for (TagPlayer player : team.getPlayers()) {
            player.setBonusGameScore(40);
        }

        updateBossbarManager();
        updateServerSidebar();

        // Play fireworks
        playVictoryFireworks(team);
    }

    @Override
    public void resetGame() {

        super.resetGame();

        // Unregister no move listener from player move event
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);
        PlayerTeleportEvent.getHandlerList().unregister(teleportListener);

        cancelTask(roundTimerTask);
        cancelTask(taggerReleaseTimerTask);

    }

    public void setupMap (TagMap map) {

        super.setupMap(map);

        if (!map.isEvaderSpawnsRandom()) {
            teamEvaderSpawns = map.getTeamEvaderSpawns();
        }
        else {
            randomEvaderSpawns = map.getEvaderSpawns();
        }

        if (!map.isTaggerSpawnsEqual()) {
            teamTaggerSpawns = map.getTeamTaggerSpawns();
        }
        else {
            equalTaggerSpawns = map.getTaggerSpawns();
        }
    }

    public void setupRound() {

        // Round variables
        roundInPlay = false;
        roundNumber++;
        taggerReleaseTimer = 6;
        startRoundTimer = true;
        finalGlow = false;

        roundTimer = roundLength;
        roundSurviveTimer = 0;
        survivorPointsAdded = 0;

        currentEvaderKillValue = MAX_EVADER_KILL_POINTS;

        // Choose which team is tagger
        taggers = taggerOrder.get(roundNumber - 1);
        getMap().fillBlocksAtStart();

        // Enable heal pads
        getCombatManager().enableAllHealPads();
        getCombatManager().setAllPlayersImmune(false);

        // Randomise evader spawns if evader spawns are random
        if (getMap().isEvaderSpawnsRandom()) {

            // Randomise list of spawns
            List<Set<Location>> randomisedSpawns = new ArrayList<>(randomEvaderSpawns);
            Collections.shuffle(randomisedSpawns);

            // Assign evader spawn to each team
            int i = 0;
            for (TagTeam team : getTeams()) {
                if (team == taggers) continue;
                int spawnIndex = i % randomisedSpawns.size();
                team.setEvaderSpawns(randomisedSpawns.get(spawnIndex));
                i++;
            }

        }

        // Setup team for round
        for (TagTeam team : getTeams()) {
            team.setupRound();
            team.clearAlliedTeams();
            // If this team is not tagging, make it so that they are treated as an ally by the other non-tagging teams
            if (taggers != team) {

                // Add other teams
                for (TagTeam team2 : getTeams()) {
                    if (team2 == team) continue;
                    if (taggers != team2) team.addAlliedTeam(team2);
                }

                // Make sure tagging team cannot see name tags of runners while blinded
                team.scoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OTHER_TEAMS);

            }
        }

        // Start countdown for next round
        if (roundNumber == 1) {
            new TagStartRoundTimer(getGameManager(), this, 16, true).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        } else {
            new TagStartRoundTimer(getGameManager(), this, 10, false).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        }

        // Make players unable to move
        noMoveListener.setEvadersMove(false);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(noMoveListener, CBCPlugin.getPlugin());

        if (getMap().canEvadersMoveAtRoundStart()) {
            noMoveListener.setEvadersMove(true);
        }

        for (TagPlayer player : getPlayers()) {
            if (player.isOnline()) {
                player.setCanMove(false);
            }
        }

        // Update footer component
        updateListFooter();

        // Update bossbar
        updateBossbarManager();
        updateServerSidebar();
    }

    public void releaseEvaders () {

        getMap().fillBlocksAtEnd();

        // Round is now in play
        roundInPlay = true;
        startRoundTimer = false;

        getCombatManager().setVoidKill(true);

        // Make it so evaders can move and make them alive
        noMoveListener.setEvadersMove(true);
        for (TagPlayer player : getPlayers()) {
            if (player.getTeam() != taggers) {
                if (!player.isOnline()) {
                    if (!player.isInGame()) continue;
                    player.automaticElimination();
                    continue;
                }
                player.playerStartRound();
            }
        }

        checkPlayerCounts();

        if (roundInPlay) {
            // Start timer to release taggers
            taggerReleaseTimerTask = new TaggerReleaseTimer(this);
            taggerReleaseTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
            if (roundNumber == 1) {
                new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
            }
        }

        // Update UI elements
        updateServerSidebar();
        updateBossbarManager();

    }

    public void decrementTaggerReleaseTimer() {

        // Decrement timer
        taggerReleaseTimer--;

        // Release taggers if tagger release timer is 0
        if (taggerReleaseTimer == 0) {

            // Iterate through each player in the world
            for (Player player : getWorld().getPlayers()) {
                // Clear titles and play sound
                player.clearTitle();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);
            }
            releaseTaggers();
            // Cancel timer
            cancelTask(taggerReleaseTimerTask);
            taggerReleaseTimerTask = null;
        }
        else if (taggerReleaseTimer <= 3) {
            // Play title to count down release of taggers
            Component titleComponent = blankComponent();
            Component subtitleComponent = Component.text("Taggers released in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(taggerReleaseTimer).decorate(TextDecoration.BOLD).color(taggers.getColor()));
            // Show title
            // Iterate through each player in the world
            for (Player player : getWorld().getPlayers()) {
                // Play title and play sound
                player.showTitle(Title.title(
                        titleComponent,
                        subtitleComponent,
                        TextUtil.titleTimes(0, 3000, 500)
                ));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
            }
        }

        // Update bossbar
        updateBossbarManager();

    }

    public void releaseTaggers () {

        // Make it so taggers can move and make them alive
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);

        for (TagPlayer player : getPlayers()) {
            if (player.getTeam() == taggers) {
                player.playerStartRound();
            }
            player.setCanMove(true);
        }

        // Turn on nametag visibility for all teams
        for (TagTeam team : getTeams()) {
            team.scoreboardTeam().setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }

        // Start round timer
        roundTimerTask = new TagRoundTimer(this);
        roundTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

    }

    public void decrementRoundTimer () {

        if (!roundInPlay) return;

        // Decrement round timer by 1
        roundTimer--;
        roundSurviveTimer++;

        // Recalculate kill worth
        float roundPercentageLeft = ((float) roundTimer / roundLength);
        currentEvaderKillValue = Math.round(((float) MAX_EVADER_KILL_POINTS - (float) MIN_EVADER_KILL_POINTS) * roundPercentageLeft) + MIN_EVADER_KILL_POINTS;

        // Calculate amount of points players score
        int oldSurvivorPointsAdded = survivorPointsAdded;
        int newSurvivorPointsAdded = Math.round((1 - roundPercentageLeft) * (float) MAX_POINTS_FOR_SURVIVAL);

        int newPoints = newSurvivorPointsAdded - oldSurvivorPointsAdded;
        for (TagPlayer player : getEvaders()) {
            if (!player.isAlive()) continue;

            if (newPoints > 0) {
                // Give currently alive players points
                for (int i = 0; i < newPoints; i++) {
                    player.playerSurviveScore();
                }
            }

            // Increase seconds survived stat
            player.playerSurvivedSecond();
        }

        survivorPointsAdded = newSurvivorPointsAdded;

        updatePlacements();

        // Check if round is over by timer
        if (roundTimer == 0) {
            endRound(true);
        }
        else {
            // Make all evaders glow at certain points
            if (roundTimer <= 30 && !finalGlow) {
                finalGlow = true;
                giveGlowingToEvaders(200000);
            }
            else if (roundTimer <= 60) {
                if (roundTimer % 10 == 0) {
                    giveGlowingToEvaders(35);
                }
            }
            else {
                if (roundTimer % 15 == 0) {
                    giveGlowingToEvaders(35);
                }
            }

            // Show timer warnings at certain points
            if (roundTimer == 60 || roundTimer == 30 || roundTimer <= 10) {
                NamedTextColor timeColor = NamedTextColor.RED;
                if (roundTimer == 60) {
                    timeColor = NamedTextColor.YELLOW;
                    getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 0);
                }
                else if (roundTimer == 30) {
                    timeColor = NamedTextColor.GOLD;
                    getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
                }
                else {
                    getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
                }

                // Plural seperation
                if (roundTimer == 1) {
                    getGameManager().sendGlobalMessage(
                            Component.text("The round ends in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                                    .append(Component.text("1 second!").decorate(TextDecoration.BOLD).color(timeColor))
                    );
                }
                else {
                    getGameManager().sendGlobalMessage(
                            Component.text("The round ends in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                                    .append(Component.text(roundTimer + " seconds!").decorate(TextDecoration.BOLD).color(timeColor))
                    );
                }
            }
        }

        // Update UI elements
        updateServerSidebar();
        updateBossbarManager();

    }

    public void giveGlowingToEvaders (int lengthInTicks) {

        for (TagPlayer evader : getEvaders()) {

            // Check if player is alive
            if (!evader.isAlive()) continue;
            Player evaderEntity = evader.getPlayer();

            evaderEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, lengthInTicks, 0, false, false, true));

        }

        // Play sound
        getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BELL, 200, 2);

    }

    public void checkPlayerCounts () {
        int evadersAlive = getEvadersAlive();
        // End round if there are 0 evaders alive and the round is still in play
        if (roundInPlay && evadersAlive == 0) {
            endRound(false);
        }
    }

    public int getEvadersAlive () {
        // Find how many evaders are alive
        int evadersAlive = 0;
        for (TagPlayer player : getEvaders()) {
            // Check if player is alive
            if (player.isAlive()) {
                evadersAlive++;
            }
        }
        return evadersAlive;
    }

    public void endRound (boolean byTimer) {

        roundInPlay = false;

        getCombatManager().setAllPlayersImmune(true);
        getCombatManager().setVoidKill(false);
        getCombatManager().disableAllHealPads();

        // Display title of round being over
        Component title = Component.text("ROUND OVER!")
                .decorate(TextDecoration.BOLD).color(taggers.getColor());
        Component subtitle;
        Component roundOverMessage;

        if (!byTimer) {

            // Give team points for ending round
            float roundPercentageLeft = ((float) roundTimer / roundLength);
            int bonusPoints = Math.round((float) MAX_POINTS_FOR_WIPEOUT * roundPercentageLeft) + WIPEOUT_BONUS;

            // If the round was not ended by a timer, show how long it took for the taggers to kill everyone
            subtitle = Component.text("Taggers finished in ").color(NamedTextColor.WHITE)
                    .append(Component.text(roundSurviveTimer + "s " + "(+" + bonusPoints + " points)").color(taggers.getColor()));

            roundOverMessage = Component.newline()
                    .append(Component.text("ROUND OVER > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                    .append(Component.text(taggers.getTeamName()).decorate(TextDecoration.BOLD).color(taggers.getColor()))
                    .append(Component.text(" killed all evaders in " + roundSurviveTimer + " seconds, earning a bonus of ").color(NamedTextColor.WHITE))
                    .append(Component.text(bonusPoints).color(taggers.getColor()))
                    .append(Component.text(" points!").color(NamedTextColor.WHITE))
                    .append(Component.newline());

            taggers.taggerRoundCompletedPoints(bonusPoints);
            updatePlacements();

        }
        else {
            // If the round was ended by time running out, show how many evaders survived
            int evadersAlive = getEvadersAlive();
            int totalEvaders = getEvaders().size();
            int evadersKilled = getEvaders().size() - evadersAlive;
            if (evadersAlive == 1) {
                subtitle = Component.text("1").color(taggers.getColor())
                        .append(Component.text(" evader survived").color(NamedTextColor.WHITE));
            } else {
                subtitle = Component.text(evadersAlive).color(taggers.getColor())
                        .append(Component.text(" evaders survived").color(NamedTextColor.WHITE));
            }

            // Send message
            roundOverMessage = Component.newline()
                        .append(Component.text("ROUND OVER > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(taggers.getTeamName()).decorate(TextDecoration.BOLD).color(taggers.getColor()))
                        .append(Component.text(" were able to kill ").color(NamedTextColor.WHITE))
                        .append(Component.text(evadersKilled + "/" + totalEvaders).color(taggers.getColor()))
                        .append(Component.text(" evaders! ").color(NamedTextColor.WHITE))
                        .append(Component.text("All surviving evaders earn ").color(NamedTextColor.WHITE))
                        .append(Component.text(SURVIVAL_BONUS + " bonus points.").color(NamedTextColor.GREEN))
                        .append(Component.newline());

            // Give all alive players bonus
            for (TagPlayer evader : getEvaders()) {
                if (evader.isAlive()) {
                    evader.giveSurvivalBonus(SURVIVAL_BONUS);
                    updatePlacements();
                }
            }
        }

        getGameManager().sendGlobalMessage(roundOverMessage);
        getGameManager().sendGlobalTitle(Title.title(title, subtitle,
                TextUtil.titleTimes(0, 3000, 500)));
        getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 200, 0);

        // Start the next round
        boolean nextRound = true;
        if (roundNumber % getTeams().size() == 0) {
            // Check if game should be ended
            if (turnNum == roundsPerTeam) {
                // End game
                nextRound = false;
            }
            else {
                // Set team order of next cycle -- team with lowest score goes first
                List<TagTeam> teamsByScore = getTeamsByScore();
                Collections.reverse(teamsByScore);
                taggerOrder.addAll(teamsByScore);
                updateListFooter();
                turnNum++;
            }
        }

        if (nextRound) {
            new TagNextRoundTimer(getGameManager(), this, 10).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }
        else {

            // End game if only one team is winner
            TagTeam firstPlace = getTeamsByScore().get(0);

            if (getTeams().size() > 1) {
                if (firstPlace.getIntScore() == getTeamsByScore().get(1).getIntScore()) {
                    // Send to overtime
                    return;
                }
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (isGameOver()) return;
                    gameWon(firstPlace);
                }
            }.runTaskLater(CBCPlugin.getPlugin(), 60);

        }
    }

    public List<TagTeam> getTeamsByScore() {

        List<TagTeam> sortedTeamList = new ArrayList<>(getTeams());
        sortedTeamList.sort(Comparator.comparingInt(TagTeam::getIntScore).reversed());

        return sortedTeamList;

    }

    public void updatePlacements () {

        List<TagTeam> teamsByScore = getTeamsByScore();

        int placement = 0;
        int currentScore = 100000;
        int i = 0;

        for (TagTeam team : teamsByScore) {

            boolean tied = false;
            if (team.getIntScore() < currentScore) {
                placement = i + 1;
                currentScore = team.getIntScore();
                if (teamsByScore.size() - 1 != i) {
                    if (teamsByScore.get(i + 1).getIntScore() == currentScore) {
                        tied = true;
                    }
                }
            }
            else if (currentScore == team.getIntScore()) {
                tied = true;
            }

            team.setPlacement(placement, tied);

            i++;

        }

        // Update UI elements
        updateServerSidebar();
    }

    public TagTeam getTaggers () {
        return taggers;
    }

    public void updateListFooter () {

        NamedTextColor taggerColor = NamedTextColor.AQUA;
        if (taggers != null) {
            taggerColor = taggers.getColor();
        }

        footerComponent = smallText("ROUND " + roundNumber + " - ").color(taggerColor).decorate(TextDecoration.BOLD);
        int maxRounds = getTeams().size() * roundsPerTeam;

        for (int rd = 0; rd < maxRounds; rd++) {

            NamedTextColor color = NamedTextColor.GRAY;
            if (rd < taggerOrder.size()) {
                TagTeam team = taggerOrder.get(rd);
                color = team.getColor();
            }

            if (roundNumber == rd + 1) {
                footerComponent = footerComponent.append(smallText(String.valueOf(rd + 1)).color(color)
                        .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE).decoration(TextDecoration.UNDERLINED, TextDecoration.State.TRUE));
            }
            else {
                footerComponent = footerComponent.append(smallText(String.valueOf(rd + 1)).color(color)
                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).decoration(TextDecoration.UNDERLINED, TextDecoration.State.FALSE));
            }

            if (rd != maxRounds - 1) {
                if (rd + 1 >= roundNumber) {
                    footerComponent = footerComponent.append(smallText(" \uE000 ").color(NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));
                }
                else {
                    footerComponent = footerComponent.append(smallText(" \uE000 ").color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));
                }
            }
        }

        getGameManager().setPlayerListFooter(footerComponent);
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new TagPostGameStats(this);
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isRoundInPlay() {
        return roundInPlay;
    }

    public Set<TagPlayer> getEvaders () {
        Set<TagPlayer> evaders = new HashSet<>();
        for (TagPlayer player : getPlayers()) {
            // Add player to evaders list if they are not a tagger
            if (!player.isTagger()) evaders.add(player);
        }
        return evaders;
    }

    public int getRoundTimer() {
        return roundTimer;
    }

    public int getTaggerReleaseTimer() {
        return taggerReleaseTimer;
    }

    public boolean isStartRoundTimer () {
        return startRoundTimer;
    }

    public int getCurrentEvaderKillValue() {
        return currentEvaderKillValue;
    }

    @Override
    public void playerJoinServer(Player playerEntity) {

        super.playerJoinServer(playerEntity);

        // Check if player is CBC player
        TagPlayer player = getPlayer(playerEntity);
        if (player == null) return;

        TagTeam team = getTypedTeam(player.getTeam());
        if (team == null) return;

        // If player is tagger and joined before taggers released, put them in
        if (player.isTagger()) {
            if (taggerReleaseTimer > 0) {
                // Setup player round and teleport them to tagger spawn
                player.playerSetupRound();
                player.teleportPlayerToSpawn(team.getRandomTaggerSpawn(), getMap().getMapCentre());
                player.setCanMove(false);
            }
        } else {
            if (!roundInPlay && taggerReleaseTimer > 0) {
                // Setup player round and teleport them to tagger spawn
                player.playerSetupRound();
                player.teleportPlayerToSpawn(team.getRandomEvaderSpawn(), getMap().getMapCentre());
                player.setCanMove(false);
            }
        }

    }

    public int getMaxScorePerSecond() {
        return maxScorePerSecond;
    }

    public int getTaggerRespawnTimer () {
        return taggerRespawnTimer;
    }
}
