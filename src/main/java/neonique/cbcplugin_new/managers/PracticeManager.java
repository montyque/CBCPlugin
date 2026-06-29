package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mechanics.FFASpawnpoint;
import neonique.cbcplugin_new.listeners.practice.PracticePlayerTeleport;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.playerclasses.PracticePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PracticeManager implements PlayerSession<PracticePlayer> {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    // Saving players, so they can leave and join practice and still have their stats intact
    private Map<UUID, PracticePlayer> players;

    private boolean enabled = false;

    public CBCPlugin plugin;

    private final World world;

    private Map<String, CBCMap> practiceMaps;

    private CBCMap currentMap;
    private List<FFASpawnpoint> spawns;

    private final Location PRACTICE_HOLOGRAM_LOCATION;

    // Tasks and listeners
    PracticePlayerTeleport playerTeleportTask;

    public PracticeManager (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;

        this.world = gameManager.getWorld();
        currentMap = null;

        PRACTICE_HOLOGRAM_LOCATION = new Location(world, -1069.5, 126.0, -1659.5);
    }

    public void enable(CBCMap map) {

        if (enabled) {
            return;
        }

        enabled = true;

        // Enable practice arena map
        setupMap(map);

        // Enable weapons
        combatManager.activateWeapons();

        players = new HashMap<>();

        // Send message
        world.sendMessage(
                Component.text("The practice arena has been opened on " + map.getName() + ".").color(NamedTextColor.GREEN)
        );

        // Remove glass barrier at portal
        for (int x = -1071; x <= -1069; x++) {
            for (int y = 125; y <= 127; y++) {
                world.getBlockAt(x, y, -1658).setType(Material.AIR);
            }
        }

        // Find area effect cloud nearby
        AreaEffectCloud hologram;
        Collection<AreaEffectCloud> entities = PRACTICE_HOLOGRAM_LOCATION
                .getNearbyEntitiesByType(AreaEffectCloud.class, 1);

        // If no area effect cloud exists, create one
        if (entities.isEmpty()) {
            hologram = (AreaEffectCloud) world.spawnEntity(PRACTICE_HOLOGRAM_LOCATION, EntityType.AREA_EFFECT_CLOUD);
            hologram.setCustomNameVisible(true);
            hologram.clearCustomEffects();
            hologram.setRadius(0);
            hologram.setDuration(300000000);
        }
        else {
            hologram = (AreaEffectCloud) entities.toArray()[0];
        }

        // Change display name of hologram
        if (hologram != null) {
            hologram.customName(Component.text("Practice - " + map.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        }

        // Start new player teleport task
        playerTeleportTask = new PracticePlayerTeleport(gameManager, this);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerTeleportTask, CBCPlugin.getPlugin());

        // Activate the new scoreboard manager
        gameManager.getCbcScoreboardManager().activate();
        gameManager.setAudience(new HashSet<>());
    }

    public void playerJoin(Player player) {

        PracticePlayer playerObj;
        if (players.containsKey(player.getUniqueId())) {
            playerObj = players.get(player.getUniqueId());
        } else {
            playerObj = createPlayer(player);
        }

        playerObj.playerSpawn();
        gameManager.setAudience(gameManager.getPlayerEntities());

    }

    public void playerLeave (Player playerEntity) {

        PracticePlayer player = getPlayer(playerEntity);

        if (player.isAlive() && player.getLastPlayerHitBy() != null) {
            combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.LEAVE_PRACTICE, false);
        }

        removePlayer(player);
        gameManager.setAudience(gameManager.getPlayerEntities());

    }

    @Override
    public PracticePlayer createPlayer(Player playerEntity) {
        return new PracticePlayer(gameManager, combatManager, this, playerEntity);
    }

    @Override
    public void addPlayer(PracticePlayer player) {
        players.put(player.getUUID(), player);
    }

    public void removePlayer (PracticePlayer player) {
        player.setAlive(false);
        player.setImmune(false);
        player.setRespawnTicks(0);
        if (player.isOnline()) {
            player.getPlayer().clearTitle();
            player.getPlayer().teleport(new Location(world, -1069.5, 126.0, -1668.5));
            player.getPlayer().setHealth(20);
            player.getPlayer().getInventory().clear();
            player.getPlayer().updateInventory();
            player.getPlayer().removeScoreboardTag("NVDisable");
            for (PotionEffect effect : player.getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) player.getPlayer().removePotionEffect(effect.getType());
            }
            player.getPlayer().setGameMode(GameMode.ADVENTURE);
            player.getPlayer().sendMessage(
                    Component.text("You have left the practice arena.").color(NamedTextColor.RED)
            );
        }

    }

    @Override
    public Collection<PracticePlayer> getPlayers() {
        return players.values();
    }

    @Override
    public Optional<PracticePlayer> getPlayerByUUID(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public void disable() {

        if (!enabled) {
            return;
        }

        enabled = false;
        combatManager.disableWeapons();

        // Teleport all players
        for (PracticePlayer p : List.copyOf(getPlayers())) {
            removePlayer(p);
        }

        players.clear();

        // Send message
        world.sendMessage(
                Component.text("The practice arena has been closed.").color(NamedTextColor.RED)
        );

        // Put glass barrier at portal
        for (int x = -1071; x <= -1069; x++) {
            for (int y = 125; y <= 127; y++) {
                world.getBlockAt(x, y, -1658).setType(Material.GLASS);
            }
        }

        // Find area effect cloud nearby
        for (Entity e : new Location(world, -1069.5, 126.0, -1659.5).getNearbyEntitiesByType(AreaEffectCloud.class, 1)) {
            e.customName(Component.text("Practice - Closed").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        }

        // Reset audience
        gameManager.setAudience(null);

        // Reset scoreboard manager if there is no lobby or game active current
        if (!gameManager.getLobby().isActive() && gameManager.getCurrentGame() == null) {
            gameManager.getCbcScoreboardManager().deactivate();
        }

        PlayerTeleportEvent.getHandlerList().unregister(playerTeleportTask);
    }

    public void changeMap (CBCMap map) {

        // Setup map
        setupMap(map);

        // Send message to all players
        world.sendMessage(Component.text("The practice arena's map has been changed to " + map.getName() + ".")
                .color(NamedTextColor.GREEN));

        // Change area effect cloud
        for (Entity e : new Location(world, -1069.5, 126.0, -1659.5).getNearbyEntitiesByType(AreaEffectCloud.class, 1)) {
            e.customName(Component.text("Practice - " + map.getName()).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
        }

        // Respawn every player
        for (CBCPlayer player : getPlayers()) {
            player.playerSpawn();
        }

    }

    public void setupMap (CBCMap map) {

        map.loadMapChunks(true);

        this.currentMap = map;
        this.spawns = new ArrayList<>();

        combatManager.setupMap(map);

        // Get current spawns
        this.spawns = map.getSpawns();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<FFASpawnpoint> getSpawns() {
        return spawns;
    }

    public CBCMap getMap() {
        return currentMap;
    }

    public void playerLeaveServer(Player player) {
        if (hasPlayer(player)) {
            this.playerLeave(player);
        }
    }
}
