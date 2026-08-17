package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamGameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.TagNoMove;
import neonique.cbcplugin_new.listeners.gamemodes.TagTeleportListener;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.mapconfig.CBCMap;
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

public class TagGame extends TeamGame<TagPlayer, TagTeam> {

    // Game information
    private TagMapData mapData;
    private TagSettings settings;

    // Game variables
    private int roundLength = 150; // Round length in seconds
    private int roundsPerTeam = 1; // Amount of rounds each team gets to be tagger

    // Game info
    private List<TagTeam> taggerOrder;
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
    public CBCMap getMap () {
        return mapData.mapData();
    }

    @Override
    public TagPlayer createPlayer(Player playerEntity) {
        return new TagPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public TagTeam createGamemodeTeam (TeamLike team, int teamNum) {
        return new TagTeam(this, team, Integer.toString(teamNum));
    }

    public GameSidebarManager createSidebarManager () {
        return new TagSidebarManager(getGameManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new TagBossbarManager(this);
    }

    @Override
    public void setupGame (TeamGameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Create teams and players
        List<TeamLike> teamTemplates = ctx.teams();
        createTeams(teamTemplates);

        // Set up settings and map
        settings = (TagSettings) ctx.gameSettings();
        setupMap(ctx);

        // Set gamemode information
        createHeaderTitle();

        // Activate combat manager
        combatManager.activate(this);
        combatManager.setupMap(getMap());

        // Setup game commands
        setGameCommands(new TagGameCommands(this));

        // Setup teams/players
        setupTeams();
        teleportSpectators();

        // Choose the tagger order
        taggerOrder = new ArrayList<>(getTeams());
        Collections.shuffle(taggerOrder);
        noMoveListener = new TagNoMove(gameManager, this);

        // Create Bossbar/Sidebar managers
        createUIManagers();

        // Make taggers unable to teleport
        teleportListener = new TagTeleportListener(this);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(teleportListener, CBCPlugin.getPlugin());

        // Start first round
        setupRound();

    }

    private void setupMap (TeamGameContext ctx) {
        mapData = (TagMapData) ctx.mapData();
        roundLength = settings.roundLength();
        roundsPerTeam = settings.taggerRoundsPerTeam();
    }

    private void setupTeams () {
        for (TagTeam team : getTeams()) {
            if (maxScorePerSecond < team.players().size()) {
                maxScorePerSecond = team.players().size();
            }
        }
        updatePlacements();
    }

    @Override
    public void gameWon (TagTeam team) {

        super.gameWon(team);

        for (TagPlayer player : team.players()) {
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

        currentEvaderKillValue = settings.maxTaggerKillPoints();

        // Choose which team is tagger
        taggers = taggerOrder.get(roundNumber - 1);
        this.getMap().fillBlocksAtStart();

        // Enable heal pads
        getCombatManager().enableAllHealPads();
        getCombatManager().setAllPlayersImmune(false);

        // Setup all teams for round
        Map<TagTeam, List<Location>> taggerSpawns = mapData.taggerSpawnList().getTeamSpawnLocations(getTeams(), getWorld());
        Map<TagTeam, List<Location>> evaderSpawns = mapData.evaderSpawnList().getTeamSpawnLocations(getTeams(), getWorld());

        for (TagTeam team : getTeams()) {

            team.clearAlliedTeams();

            // If this team is not tagging, make it so that they are treated as an ally by the other non-tagging teams
            if (taggers == team) {

                team.setupRound(taggerSpawns.get(team));

            } else {

                team.setupRound(evaderSpawns.get(team));

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

        if (!mapData.evadersFrozenOnSetup()) {
            noMoveListener.setEvadersMove(true);
        }

        for (TagPlayer player : this.players()) {
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

        this.getMap().fillBlocksAtEnd();

        // Round is now in play
        roundInPlay = true;
        startRoundTimer = false;

        getCombatManager().setVoidKill(true);

        // Make it so evaders can move and make them alive
        noMoveListener.setEvadersMove(true);
        for (TagPlayer player : this.players()) {
            if (player.team() != taggers) {
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

        } else if (taggerReleaseTimer <= 3) {

            // Show tagger release title

            Component titleComponent = blankComponent();
            Component subtitleComponent = Component.text("Taggers released in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(taggerReleaseTimer).decorate(TextDecoration.BOLD).color(taggers.textColor()));

            showTitle(Title.title(titleComponent, subtitleComponent,
                    TextUtil.titleTimes(0, 3000, 500)
            ));

            playSound(net.kyori.adventure.sound.Sound.sound(Sound.BLOCK_NOTE_BLOCK_PLING,
                    net.kyori.adventure.sound.Sound.Source.MASTER, 100, 1));

        }

        // Update bossbar
        updateBossbarManager();

    }

    public void releaseTaggers () {

        // Make it so taggers can move and make them alive
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);

        for (TagPlayer player : this.players()) {
            if (player.team() == taggers) {
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
        currentEvaderKillValue = Math.round(((float) settings.maxTaggerKillPoints() - (float) settings.minTaggerKillPoints()) * roundPercentageLeft)
                + settings.minTaggerKillPoints();

        // Calculate amount of points players score
        int oldSurvivorPointsAdded = survivorPointsAdded;
        int newSurvivorPointsAdded = Math.round((1 - roundPercentageLeft) * (float) settings.maxEvaderSurvivalPoints());

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
                .decorate(TextDecoration.BOLD).color(taggers.textColor());
        Component subtitle;
        Component roundOverMessage;

        if (!byTimer) {

            // Give team points for ending round
            float roundPercentageLeft = ((float) roundTimer / roundLength);
            int bonusPoints = Math.round((float) settings.maxTaggerWipeoutPoints() * roundPercentageLeft) + settings.taggerWipeoutBonus();

            // If the round was not ended by a timer, show how long it took for the taggers to kill everyone
            subtitle = Component.text("Taggers finished in ").color(NamedTextColor.WHITE)
                    .append(Component.text(roundSurviveTimer + "s " + "(+" + bonusPoints + " points)").color(taggers.textColor()));

            roundOverMessage = Component.newline()
                    .append(Component.text("ROUND OVER > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                    .append(Component.text(taggers.name()).decorate(TextDecoration.BOLD).color(taggers.textColor()))
                    .append(Component.text(" killed all evaders in " + roundSurviveTimer + " seconds, earning a bonus of ").color(NamedTextColor.WHITE))
                    .append(Component.text(bonusPoints).color(taggers.textColor()))
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
                subtitle = Component.text("1").color(taggers.textColor())
                        .append(Component.text(" evader survived").color(NamedTextColor.WHITE));
            } else {
                subtitle = Component.text(evadersAlive).color(taggers.textColor())
                        .append(Component.text(" evaders survived").color(NamedTextColor.WHITE));
            }

            // Send message
            roundOverMessage = Component.newline()
                        .append(Component.text("ROUND OVER > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(taggers.name()).decorate(TextDecoration.BOLD).color(taggers.textColor()))
                        .append(Component.text(" were able to kill ").color(NamedTextColor.WHITE))
                        .append(Component.text(evadersKilled + "/" + totalEvaders).color(taggers.textColor()))
                        .append(Component.text(" evaders! ").color(NamedTextColor.WHITE))
                        .append(Component.text("All surviving evaders earn ").color(NamedTextColor.WHITE))
                        .append(Component.text(settings.evaderSurvivalBonus() + " bonus points.").color(NamedTextColor.GREEN))
                        .append(Component.newline());

            // Give all alive players bonus
            for (TagPlayer evader : getEvaders()) {
                if (evader.isAlive()) {
                    evader.giveSurvivalBonus(settings.evaderSurvivalBonus());
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
            taggerColor = taggers.textColor();
        }

        footerComponent = smallText("ROUND " + roundNumber + " - ").color(taggerColor).decorate(TextDecoration.BOLD);
        int maxRounds = getTeams().size() * roundsPerTeam;

        for (int rd = 0; rd < maxRounds; rd++) {

            NamedTextColor color = NamedTextColor.GRAY;
            if (rd < taggerOrder.size()) {
                TagTeam team = taggerOrder.get(rd);
                color = team.textColor();
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
        for (TagPlayer player : this.players()) {
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

        TagTeam team = getTypedTeam(player.team());
        if (team == null) return;

        // If player is tagger and joined before taggers released, put them in
        if (player.isTagger()) {
            if (taggerReleaseTimer > 0) {
                // Setup player round and teleport them to tagger spawn
                player.playerSetupRound();
                player.teleportPlayerToSpawn(team.getRandomTaggerSpawn(), this.getMap().getMapCentre());
                player.setCanMove(false);
            }
        } else {
            if (!roundInPlay && taggerReleaseTimer > 0) {
                // Setup player round and teleport them to tagger spawn
                player.playerSetupRound();
                player.teleportPlayerToSpawn(team.getRandomEvaderSpawn(), this.getMap().getMapCentre());
                player.setCanMove(false);
            }
        }

    }

    public int getMaxScorePerSecond() {
        return maxScorePerSecond;
    }

    public int getTaggerRespawnTimer () {
        return settings.taggerRespawnTimer();
    }
}
