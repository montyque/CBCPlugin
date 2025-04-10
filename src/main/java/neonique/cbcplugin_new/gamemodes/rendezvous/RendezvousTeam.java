package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.util.StringUtil;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.titleTimes;

public class RendezvousTeam extends CBCTeam {

    private final RendezvousGame game;
    private final GameManager gameManager;
    private final ResourcePackManager resourcePackManager;

    // Checkpoint/runner related variables
    private RendezvousPlayer runner;
    private RendezvousCheckpoint targetCheckpoint;
    private boolean runnerInCheckpoint = false;
    private int targetProgress = 30;
    private int progressMax = 30;
    private boolean hasHalfStick = false;
    private List<RendezvousPlayer> runnerQueue;
    private double currentCheckpointTargetDistance;
    private ItemStack runnerCompassItem;

    boolean outOfGame = false;

    // Team start spawns
    private Set<Location> spawns;

    // Placement
    int placement = 1;
    boolean tied = true;

    // Score related variables
    private int score;

    // Other stats
    private int checkpointsCleared = 0;
    private int runnerDeaths = 0;

    // Queue show
    private Component footerQueueComponent;

    public RendezvousTeam(RendezvousGame game, String teamId, String teamIdNum, String teamName, NamedTextColor teamColor,
                   String prefix, ItemStack item, ItemStack glassHead) {
        super(teamId, teamIdNum, teamName, teamColor, prefix, item, glassHead);

        this.game = game;
        this.score = game.getScoreStart();

        this.gameManager = game.getGameManager();
        resourcePackManager = CBCPlugin.getResourcePackManager();
    }

    public void setRunnerQueue (String numberOrder) {

        runnerQueue = new ArrayList<>();

        // Check if number order is only one character
        if (numberOrder.length() <= 1) {
            // Shuffle runner queue and return
            shuffleRunnerQueue();
            return;
        }

        // Get players
        List<RendezvousPlayer> players = getRendezvousPlayers();

        // Sort players by name
        players.sort(Comparator.comparing(RendezvousPlayer::getLowercaseName));
        List<RendezvousPlayer> playersNotListed = getRendezvousPlayers();

        // Go through each character in number order list
        for (String chr : numberOrder.split("")) {
            // Try parse character as number
            int num;
            try {
                num = Integer.parseInt(chr);
            } catch (NumberFormatException e) {
                continue;
            }

            // Check if num is within the boundaries
            if (num < 1 || num > players.size()) continue;

            // Find which player this number is referring to
            RendezvousPlayer player = players.get(num - 1);

            // Check if this player is already in the queue
            if (runnerQueue.contains(player)) continue;

            // Add player to queue and remove player from players not listed
            runnerQueue.add(player);
            playersNotListed.remove(player);
        }

        // Randomise other players and add them to queue
        Collections.shuffle(playersNotListed);
        runnerQueue.addAll(playersNotListed);

    }

    public void shuffleRunnerQueue () {
        // Get players
        List<RendezvousPlayer> players = getRendezvousPlayers();
        // Shuffle players
        Collections.shuffle(players);
        runnerQueue = players;
    }

    public List<RendezvousPlayer> getOnlinePlayersInQueue () {
        List<RendezvousPlayer> onlineQueueOnly = new ArrayList<>();
        for (RendezvousPlayer player : runnerQueue) {
            if (player.isOnline()) {
                onlineQueueOnly.add(player);
            }
        }
        return onlineQueueOnly;
    }

    public void setRunnerNextPlayerInQueue () {

        List<RendezvousPlayer> onlinePlayersInQueue = getOnlinePlayersInQueue();
        if (!onlinePlayersInQueue.isEmpty()) {

            // Get next player in queue
            RendezvousPlayer nextPlayerInQueue = onlinePlayersInQueue.get(0);

            // Move them (and any players ahead of them) to the back of the queue
            List<RendezvousPlayer> newQueue = new ArrayList<>(runnerQueue);
            for (RendezvousPlayer player : runnerQueue) {
                newQueue.remove(player);
                newQueue.add(player);
                if (nextPlayerInQueue == player) {
                    break;
                }
            }

            // Set runner
            setRunner(nextPlayerInQueue);

            // Update queue
            runnerQueue.clear();
            runnerQueue.addAll(newQueue);
        }
        else {
            // No runner available, so set runner when player joins team
            runner = null;
        }

        // Set queue component
        setFooterQueueComponent();
    }

    public RendezvousPlayer getNextOnlinePlayerInQueue () {
        List<RendezvousPlayer> onlinePlayersInQueue = getOnlinePlayersInQueue();
        if (!onlinePlayersInQueue.isEmpty()) {
            // Get next player in queue
            return onlinePlayersInQueue.get(0);
        }
        return null;
    }

    public void setFooterQueueComponent () {

        footerQueueComponent = smallText("RUNNER ORDER: ").color(NamedTextColor.AQUA);

        List<RendezvousPlayer> queueCopy = new ArrayList<>(runnerQueue);
        // Add runner to component
        if (runner != null) {
            Component runnerPlayerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL,
                    runner.getOfflinePlayer()).color(NamedTextColor.WHITE);
            footerQueueComponent = footerQueueComponent.append(runnerPlayerHeadComponent);

            queueCopy.remove(runner);
        }

        for (RendezvousPlayer player : queueCopy) {
            footerQueueComponent = footerQueueComponent.append(smallText(" \uE000 ").color(NamedTextColor.YELLOW));
            Component playerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL,
                    player.getOfflinePlayer()).color(NamedTextColor.WHITE);
            footerQueueComponent = footerQueueComponent.append(playerHeadComponent);
        }

        for (CBCPlayer player : getPlayers()) {
            if (!player.isOnline()) continue;
            player.getPlayer().sendPlayerListFooter(footerQueueComponent);
        }
    }

    public void setPlayerListFooterForPlayer (Player player) {
        player.sendPlayerListFooter(Objects.requireNonNullElseGet(footerQueueComponent, TextUtil::blankComponent));
    }

    public void setRunner (RendezvousPlayer runner) {

        if (runner == null) return;
        boolean newCheckpoint = false;

        if (this.runner != null) {
            if (this.runner != runner) {
                newCheckpoint = true;
                changeInRunner();
            }
        }
        else {
            newCheckpoint = true;
        }

        this.runner = runner;
        PlayerInventory inventory = runner.getPlayer().getInventory();
        runner.giveRunnerCompass(inventory);

        if (newCheckpoint) {
            if (onFinalCheckpoint() && runner.isAlive()) {
                runner.finalCheckpointTeleport();
            } else {
                selectNewCheckpoint();
            }
        }

        for (RendezvousPlayer player : getRendezvousPlayers()) {
            if (!player.isAlive()) continue;
            if (player.isPlayerRunner()) {
                player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000,
                        0, false, false, false));
            }
            else {
                player.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
            }
        }

        game.updateServerSidebar();
    }

    public void changeInRunner () {

        for (RendezvousPlayer player : getRendezvousPlayers()) {
            if (player.isOnline()) break;
            Player playerEntity = player.getPlayer();
            if (player.isPlayerRunner()) {

                // Play title to the runner
                Component titleComponent = Component.text("You're the runner!").color(getColor())
                        .decorate(TextDecoration.BOLD);

                Component subtitleComponent = Component.text("Run to the new checkpoint!").color(NamedTextColor.WHITE);

                Title title = Title.title(titleComponent, subtitleComponent, Title.Times.times(
                        Duration.ofMillis(250), Duration.ofMillis(2500), Duration.ofMillis(250)
                ));

                playerEntity.showTitle(title);

                // Send message
                gameManager.sendGlobalMessage(
                        Component.text("RUNNER SWAP > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                                .append(player.getNameComponent().decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                                .append(Component.text(" is the new runner for ").color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                                .append(Component.text(getTeamName()).color(getColor())
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                                .append(Component.text("!").color(NamedTextColor.WHITE)
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                );
            }
            else {
                // Play title to all non runners
                Component titleComponent = Component.text("Runner switch!").color(getColor())
                        .decorate(TextDecoration.BOLD);
                Component subtitleComponent = Component.text("New runner is ").color(NamedTextColor.WHITE)
                        .append(runner.getNameComponent());
                Title title = Title.title(titleComponent, subtitleComponent, Title.Times.times(
                        Duration.ofMillis(250), Duration.ofMillis(2000), Duration.ofMillis(250)
                ));
                playerEntity.showTitle(title);

                // Remove compass from other players
                playerEntity.getInventory().remove(ItemStack.of(Material.COMPASS));
                playerEntity.updateInventory();
            }
        }


    }

    public void selectNewCheckpoint () {

        if (targetCheckpoint != null) {
            this.targetCheckpoint.removeGlowingMarker(this);
            this.targetCheckpoint.removeHologram(this);
        }

        if (onFinalCheckpoint()) {
            // Final checkpoint has been reached, which takes 7 seconds to fully capture
            targetCheckpoint = game.getFinalCheckpoint();
            currentCheckpointTargetDistance = targetCheckpoint.distance(runner.getPlayer().getLocation());
            setProgressMax(70);
        }
        else {

            int targetDistances = game.getTargetDistancesSize();

            if (targetDistances <= checkpointsCleared) {
                targetCheckpoint = selectCheckpoint(game.getRandomCheckpointDistance(), 10);
                currentCheckpointTargetDistance = targetCheckpoint.distance(runner.getPlayer().getLocation());
                game.addCheckpointTargetDistance(currentCheckpointTargetDistance);
            }
            else {
                currentCheckpointTargetDistance = game.getCheckpointTargetDistances().get(checkpointsCleared);
                targetCheckpoint = selectCheckpoint(currentCheckpointTargetDistance, 10);
            }
        }

        createCheckpointCompass();

        if (runner != null) {
            if (!runner.isOnline()) return;
            PlayerInventory inventory = runner.getPlayer().getInventory();
            runner.giveRunnerCompass(inventory);
        }

        targetCheckpoint.createGlowingMarker(this);
        targetProgress = progressMax;
        targetCheckpoint.setHologramTitle(this);
    }

    public RendezvousCheckpoint selectCheckpoint (double targetDistance, double acceptanceThreshold) {

        List<RendezvousCheckpoint> checkpoints = new ArrayList<>(game.getCheckpoints());
        Collections.shuffle(checkpoints);

        RendezvousCheckpoint closestCheckpoint = null;
        double smallestDif = 2000000000;

        Location runnerLocation = runner.getPlayer().getLocation();

        for (RendezvousCheckpoint checkpoint : checkpoints) {

            if (checkpoint.isCheckpointTaken()) continue;

            double distance = checkpoint.distance(runnerLocation);
            double difference = Math.abs(targetDistance - distance);

            if (difference < smallestDif) {
                closestCheckpoint = checkpoint;
                smallestDif = difference;
            }

            if (difference < acceptanceThreshold) {
                // Check if final checkpoint is enabled and team is at their penultimate checkpoint
                if (game.isFinalCheckpointEnabled() || score == 2) {
                    // Get distance between checkpoint and final checkpoint
                    double distanceToFinalCheckpoint = checkpoint.distance(game.getFinalCheckpoint());
                    double dif = Math.abs(70 - distanceToFinalCheckpoint);
                    // Check if difference is less than 15
                    if (dif < 15) {
                        break;
                    }
                    else {
                        continue;
                    }
                }
                break;
            }
        }

        if (closestCheckpoint == null) {
            return checkpoints.get(0);
        }
        else {
            return closestCheckpoint;
        }
    }

    public void updateCheckpointStatus() {

        int oldTargetProgress = targetProgress;
        if (!runnerInCheckpoint) {
            resetCheckpointProgress();
        } else {
            if (runner == null) return;
            progressCheckpoint();
        }

        // Update the hologram title if required
        if (targetProgress != oldTargetProgress) {
            targetCheckpoint.setHologramTitle(this);
        }

    }

    public void resetCheckpointProgress () {
        if (onFinalCheckpoint()) {
            targetProgress = Math.max(20, targetProgress);
        }
        else {
            // Reset checkpoint capture progress to progressMax
            if (game.canHalfStick()) {
                if (targetProgress <= progressMax / 2) {
                    targetProgress = progressMax / 2;
                    hasHalfStick = true;
                } else {
                    targetProgress = progressMax;
                    hasHalfStick = false;
                }
            } else {
                targetProgress = progressMax;
                hasHalfStick = false;
            }
        }
    }

    public void progressCheckpoint () {
        // Start the checkpoint capturing process
        targetProgress--;
        if (!onFinalCheckpoint()) {
            if (targetProgress <= progressMax / 2 && game.canHalfStick() && !hasHalfStick) {
                runner.getPlayer().playSound(runner.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 100, 1);
                hasHalfStick = true;
            }
        }
        else {
            // On final checkpoint, play the sound when player is at 2 seconds left
            if (targetProgress == 20 && !hasHalfStick) {
                runner.getPlayer().playSound(runner.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 100, 1);
                hasHalfStick = true;
            }
        }

        // Show title
        if (targetProgress == 0) {
            // Cleared checkpoint
            runner.checkpointCleared();
        }
        else {
            float percentileProgress = 1f - (((float) targetProgress) / (float) progressMax);
            runner.checkpointCapturingTitle(percentileProgress);
        }
    }

    public void checkpointCleared () {

        // Team has scored
        score--;
        checkpointsCleared++;

        targetCheckpoint.playSoundOnCheckpointClear();

        // Remove snowball from checkpoint
        targetCheckpoint.removeGlowingMarker(this);
        targetCheckpoint.removeHologram(this);
        targetCheckpoint = null;

        targetProgress = progressMax;
        hasHalfStick = false;
        runnerInCheckpoint = false;

        if (score > 0) {
            // Select new checkpoint
            selectNewCheckpoint();
        }
        else {
            // Win game
            game.checkTeamWon(this);
        }

        game.updatePlacements();
        game.updateServerSidebar();
    }

    public void teamOutOfGame (int place) {

        outOfGame = true;

        Component titleComponent = Component.text(StringUtil.getPlacementString(place).toUpperCase() + " PLACE").decorate(TextDecoration.BOLD).color(getColor());
        Component subtitleComponent = Component.text("You've earned your spot!");

        Title title = Title.title(titleComponent, subtitleComponent, TextUtil.titleTimes(0, 3000, 700));

        for (CBCPlayer player : getPlayers()) {
            // Kill player if still alive
            if (player.isAlive()) {
                // Set player unalive
                player.setAlive(false);
                player.playerAfterDeath(null);
            }

            player.setRespawning(false);

            // Show title to players
            if (player.isOnline()) {
                player.getPlayer().setGameMode(GameMode.SPECTATOR);
                player.getPlayer().showTitle(title);
            }
        }
    }

    public void createCheckpointCompass () {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta itemMeta = compass.getItemMeta();

        CompassMeta compassMeta = (CompassMeta) itemMeta;

        compassMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        compassMeta.setLodestone(targetCheckpoint);
        compassMeta.setLodestoneTracked(false);

        compass.setItemMeta(compassMeta);

        runnerCompassItem = compass;

    }

    public ItemStack getRunnerCompassItem() {
        return runnerCompassItem;
    }

    public List<RendezvousPlayer> getRunnerQueue () {
        return runnerQueue;
    }

    public List<RendezvousPlayer> getRendezvousPlayers () {

        List<RendezvousPlayer> rendezvousPlayers = new ArrayList<>();
        for (CBCPlayer player : getPlayers()) {
            if (player instanceof RendezvousPlayer) {
                rendezvousPlayers.add((RendezvousPlayer) player);
            }
        }
        return rendezvousPlayers;
    }

    public int getScore () {
        return score;
    }

    public RendezvousPlayer getRunner() {
        return runner;
    }

    public void setSpawns(Set<Location> spawns) {
        this.spawns = spawns;
    }

    public Set<Location> getSpawns() {
        return spawns;
    }

    public RendezvousCheckpoint getTargetCheckpoint() {
        return targetCheckpoint;
    }

    public void setRunnerInCheckpoint(boolean b) {

        if (runner != null) {
            if (runner.isAlive()) {
                if (runnerInCheckpoint && !b) {
                    runner.getPlayer().clearTitle();
                }
            }
        }

        runnerInCheckpoint = b;
    }

    @Override
    public void removeTeam () {
        super.removeTeam();
        if (targetCheckpoint != null) {
            targetCheckpoint.removeGlowingMarker(this);
            targetCheckpoint.removeHologram(this);
        }
    }

    public void setPlacement(int placement, boolean tied) {
        this.placement = placement;
        this.tied = tied;
    }

    public int getPlacement() {
        return placement;
    }

    public double getCurrentCheckpointTargetDistance() {
        return currentCheckpointTargetDistance;
    }

    public void incrementRunnerDeaths () {
        runnerDeaths++;
    }

    public int getCheckpointsCleared() {
        return checkpointsCleared;
    }

    public int getRunnerDeaths() {
        return runnerDeaths;
    }

    public void setProgressMax (int progressMax) {
        this.progressMax = progressMax;
    }

    public int getProgressMax () {
        return progressMax;
    }

    public int getTargetProgress () {
        return targetProgress;
    }

    public void nextRunnerWarning() {

        // Get next online player in queue
        RendezvousPlayer next = getNextOnlinePlayerInQueue();
        if (next == null) return;

        // Check if player is not the same as the current runner
        if (next == runner) return;

        // Display warning to next runner
        Component warningComponent = smallText("YOU'RE THE NEXT RUNNER!").color(getColor());
        next.getPlayer().showTitle(Title.title(blankComponent(), warningComponent,titleTimes(0, 1500, 0)));

    }

    public boolean onFinalCheckpoint () {
        return (game.isFinalCheckpointEnabled() && score == 1);
    }

    public boolean isOutOfGame() {
        return outOfGame;
    }

    @Override
    public void addPlayer (CBCPlayer player) {

        super.addPlayer(player);

        if (!(player instanceof RendezvousPlayer rdvPlayer)) return;

        if (runnerQueue == null) return;

        // Add player to runner queue before the runner
        for (int i = 0; i < runnerQueue.size(); i++) {
            if (runnerQueue.get(i) == runner) {
                runnerQueue.add(i, rdvPlayer);
                break;
            }
        }

        // Check if team does not have a runner right now
        if (runner == null) {
            // Change runner to player
            setRunnerNextPlayerInQueue();
            for (CBCPlayer plr : getOnlinePlayers()) {
                plr.getPlayer().sendMessage(
                        Component.text("This team had no runner, so you were automatically assigned to be the runner.").color(NamedTextColor.YELLOW)
                );
            }
        }

        // Update team footer to show correct order
        setFooterQueueComponent();
    }

    @Override
    public void removePlayer (CBCPlayer player) {

        super.removePlayer(player);

        if (!(player instanceof RendezvousPlayer rdvPlayer)) return;

        // Check if player is runner
        if (runner == rdvPlayer) {
            // Change runner to the next online player in the queue
            setRunnerNextPlayerInQueue();

            // Send message to team
            for (CBCPlayer plr : getOnlinePlayers()) {
                plr.getPlayer().sendMessage(
                        Component.text("Your current runner was removed from your team, so your team's runner has switched.").color(NamedTextColor.YELLOW)
                );
            }
        }

        // Remove player from runner queue
        runnerQueue.remove(rdvPlayer);

        // Update team footer to show correct order
        setFooterQueueComponent();

    }
}
