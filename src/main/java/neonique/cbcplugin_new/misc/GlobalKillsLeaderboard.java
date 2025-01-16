package neonique.cbcplugin_new.misc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.InvocationTargetException;
import java.util.*;


import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static org.bukkit.Bukkit.getLogger;

public class GlobalKillsLeaderboard implements Listener {

    private final GameManager gameManager;
    private Scoreboard mainScoreboard;
    private Objective globalKillsObjective;

    private final HashMap<Integer, UUID> holograms;
    private UUID leftArrowArmorStand;
    private UUID rightArrowArmorStand;

    private ArrayList<Score> globalKillCounts;
    private ArrayList<String> playerNameRankings;

    private HashMap<UUID, Integer> playerPages;
    private int maxPages = 0;

    public GlobalKillsLeaderboard (GameManager gameManager) {

        holograms = new HashMap<>();

        this.gameManager = gameManager;

        mainScoreboard = CBCPlugin.getPlugin().getServer().getScoreboardManager().getMainScoreboard();
        globalKillsObjective = mainScoreboard.getObjective("globalKills");

        if (globalKillsObjective == null) {
            mainScoreboard.registerNewObjective("globalKills", "dummy", Component.text("Global Kills"));
            globalKillsObjective = mainScoreboard.getObjective("globalKills");
        }

        globalKillCounts = new ArrayList<>();
        playerNameRankings = new ArrayList<>();
        playerPages = new HashMap<>();

        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(this, CBCPlugin.getPlugin());

        // Setting up protocol manager
        PacketAdapter packetSpawnListener = new PacketAdapter(CBCPlugin.getPlugin(), PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                tryUpdateNametag(event);
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetSpawnListener);

        PacketAdapter packetMetadataListener = new PacketAdapter(CBCPlugin.getPlugin(), PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                tryUpdateNametag(event);
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(packetMetadataListener);

    }

    public void setupHolograms () {
        // Clear hologram hashmap
        holograms.clear();

        final double hologramX = -1064.5;
        final double hologramZ = -1673.5;
        final double topHologramY = 128.9;

        final World world = gameManager.getWorld();

        Chunk chunk = world.getChunkAt(new Location(world, hologramX, topHologramY, hologramZ));

        // Create the first 11 holograms, showing the title and current page
        for (int i = 0; i < 14; i++) {

            Location hologramLocation = new Location(world, hologramX, topHologramY - (i * 0.3), hologramZ);

            // Check if there is an existing area effect cloud at this place
            Collection<AreaEffectCloud> nearbyClouds = hologramLocation.getNearbyEntitiesByType(AreaEffectCloud.class, 0.01);
            UUID hologramUUID;
            AreaEffectCloud hologram;

            for (AreaEffectCloud nearbyCloud : nearbyClouds) {
                if (!holograms.containsValue(nearbyCloud.getUniqueId())) {
                    nearbyCloud.remove();
                }
            }

            // Create new area effect cloud
            hologram = (AreaEffectCloud) gameManager.getWorld().spawnEntity(hologramLocation, EntityType.AREA_EFFECT_CLOUD);
            hologram.clearCustomEffects();
            hologram.setRadius(0);
            hologram.setDuration(30000000);
            hologram.setCustomNameVisible(true);

            hologramUUID = hologram.getUniqueId();

            // Put hologram in hashmap
            holograms.put(i, hologramUUID);

            // If hologram is first hologram, set its custom name
            if (i == 0) {
                hologram.customName(Component.text("Global Kills Leaderboard").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
            }
        }

        // Create armor stands, which will be used as arrow buttons
        ArmorStand leftArmorStand = getArmorStandAtLocation(new Location(world, -1065.5, 124.5, -1674.5));
        ArmorStand rightArmorStand = getArmorStandAtLocation(new Location(world, -1063.5, 124.5, -1672.5));

        if (leftArmorStand != null) {
            leftArrowArmorStand = leftArmorStand.getUniqueId();
        }

        if (rightArmorStand != null) {
            rightArrowArmorStand = rightArmorStand.getUniqueId();
        }
    }

    public ArmorStand getArmorStandAtLocation (Location loc) {

        // Check if there is an existing area effect cloud at this place
        Collection<ArmorStand> nearbyArmorStands = loc.getNearbyEntitiesByType(ArmorStand.class, 0.01);
        ArmorStand armorStand;
        if (nearbyArmorStands.size() == 0) {
            // Create new armor stand
            armorStand = (ArmorStand) gameManager.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setAI(false);
            armorStand.setGravity(false);
        }
        else {
            // Get UUID of current cloud
            armorStand = new ArrayList<>(nearbyArmorStands).get(0);
        }
        return armorStand;
    }

    public void updateGlobalKills () {

        globalKillCounts.clear();
        playerNameRankings.clear();

        for (String entry : mainScoreboard.getEntries()) {
            Score score = globalKillsObjective.getScore(entry);
            if (score.isScoreSet()) {
                globalKillCounts.add(score);
            }
        }

        globalKillCounts.sort(Comparator.comparingInt(Score::getScore).reversed().thenComparing(Score::getEntry));

        for (Score score : globalKillCounts) {
            playerNameRankings.add(score.getEntry());
        }

        maxPages = globalKillCounts.size() / 10;
        if (globalKillCounts.size() % 10 > 0) {
            maxPages++;
        }
        if (maxPages == 0) {
            maxPages = 1;
        }
    }

    public Component getHologramComponent (int placement, String name, int kills) {

        return Component.text(placement + ". ").color(NamedTextColor.YELLOW).append(
                Component.text(name).color(NamedTextColor.GREEN)).append(
                Component.text(" \uE449 ").color(NamedTextColor.WHITE)).append(
                Component.text(kills).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));

    }

    public void update() {

        // Update holograms
        setupHolograms();

        // Update global kill counts
        updateGlobalKills();

        final World world = gameManager.getWorld();

        // Reset all pages
        playerPages.clear();

        // Display the top 10 players
        for (int i = 0; i < 10; i++) {

            UUID hologramUUID = holograms.getOrDefault(i + 1, null);

            if (hologramUUID == null) {
                continue;
            }

            Entity hologram = world.getEntity(hologramUUID);

            if (hologram == null) {
                continue;
            }

            Score score;
            try {
                score = globalKillCounts.get(i);
            }
            catch (IndexOutOfBoundsException e) {
                hologram.customName(blankComponent());
                continue;
            }

            String name = score.getEntry();
            int globalKillAmount = score.getScore();

            Component hologramComponent = getHologramComponent(i + 1, name, globalKillAmount);
            hologram.customName(hologramComponent);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Set 12th hologram to show personal player stats
            PacketContainer packet = updatePersonalHologram(player);
            if (packet != null) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }
        }
    }

    public void onPlayerJoin (Player player) {
        playerPages.remove(player.getUniqueId());
    }

    public PacketContainer updatePersonalHologram (Player player) {

        final World world = gameManager.getWorld();
        UUID hologramUUID = holograms.getOrDefault(12, null);

        if (hologramUUID != null) {
            Entity hologram = world.getEntity(hologramUUID);
            if (hologram != null) {

                Score score = globalKillsObjective.getScore(player.getName());

                if (score.isScoreSet()) {

                    int globalKillAmount = score.getScore();

                    Component hologramComponent;
                    if (playerNameRankings.contains(player.getName())) {
                        hologramComponent = getHologramComponent(playerNameRankings.indexOf(player.getName()) + 1, player.getName(), globalKillAmount);
                    } else {
                        hologramComponent = getHologramComponent(0, player.getName(), globalKillAmount);
                    }

                    return updateNametag(player, hologram, componentToString(hologramComponent));
                }
            }
        }

        return null;
    }

    public void leftArmorStandClicked (Player player) {

        // Get player's current page
        int playerPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (playerPage == 0) {
            // Loop around to the last page
            playerPage = maxPages - 1;
        }
        else {
            playerPage--;
        }

        changePage(player, playerPage);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 300, 1);
    }

    public void rightArmorStandClicked (Player player) {

        // Get player's current page
        int playerPage = playerPages.getOrDefault(player.getUniqueId(), 0);

        if (playerPage == (maxPages - 1)) {
            // Loop around to the first page
            playerPage = 0;
        }
        else {
            playerPage++;
        }

        changePage(player, playerPage);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 300, 1);
    }

    public void changePage (Player player, int page) {

        playerPages.put(player.getUniqueId(), page);

        // Display the players in that page on the holograms
        World world = gameManager.getWorld();

        for (int i = 0; i < 10; i++) {

            UUID hologramUUID = holograms.getOrDefault(i + 1, null);

            if (hologramUUID == null) {
                continue;
            }

            Entity hologram = world.getEntity(hologramUUID);

            if (hologram == null) {
                continue;
            }

            Component hologramComponent = getHologramComponentForPage(i, page);
            PacketContainer packet = updateNametag(player, hologram, componentToString(hologramComponent));

            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }

        // Get thirteenth hologram
        UUID hologramUUID = holograms.getOrDefault(13, null);

        if (hologramUUID != null) {
            Entity hologram = world.getEntity(hologramUUID);
            if (hologram != null) {
                PacketContainer packet = updateNametag(player, hologram, componentToString(getPageComponent(page)));

                ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
            }
        }
    }

    public Component getPageComponent (int page) {
        return Component.text("◀\uF829").color(NamedTextColor.YELLOW).append(
                Component.text("Page ").color(NamedTextColor.GREEN)).append(
                Component.text(page + 1).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)).append(
                Component.text(" of ").color(NamedTextColor.GREEN)).append(
                Component.text(maxPages).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)).append(
                Component.text("\uF829▶").color(NamedTextColor.YELLOW));
    }

    public Component getHologramComponentForPage (int row, int page) {

        int placement = page * 10 + (row + 1);

        Score score;
        try {
            score = globalKillCounts.get(placement - 1);
        }
        catch (IndexOutOfBoundsException e) {
            return blankComponent();
        }

        String name = score.getEntry();
        int globalKillAmount = score.getScore();

        return getHologramComponent(placement, name, globalKillAmount);
    }

    @EventHandler
    public void onPlayerDamageEntity (EntityDamageByEntityEvent e) {

        Entity damager = e.getDamager();
        Entity damaged = e.getEntity();

        if (damager.getType() != EntityType.PLAYER) return;

        Player damagerPlayer = (Player) damager;

        if (damaged.getUniqueId() == leftArrowArmorStand) {
            leftArmorStandClicked(damagerPlayer);
            e.setCancelled(true);
        }

        if (damaged.getUniqueId() == rightArrowArmorStand) {
            rightArmorStandClicked(damagerPlayer);
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void onPlayerInteractEntity (PlayerInteractAtEntityEvent e) {

        Player player = e.getPlayer();
        Entity entityRightClicked = e.getRightClicked();

        if (entityRightClicked.getUniqueId() == leftArrowArmorStand) {
            leftArmorStandClicked(player);
            e.setCancelled(true);
        }

        if (entityRightClicked.getUniqueId() == rightArrowArmorStand) {
            rightArmorStandClicked(player);
            e.setCancelled(true);
        }
    }

    public String componentToString (Component component) {
        return LegacyComponentSerializer.legacy(LegacyComponentSerializer.SECTION_CHAR).serialize(component);
    }

    public void tryUpdateNametag (PacketEvent packetEvent) {

        PacketContainer packet = packetEvent.getPacket().deepClone();
        Player client = packetEvent.getPlayer();

        World world = gameManager.getWorld();

        for (int hologramId : holograms.keySet()) {
            UUID hologramUUID = holograms.get(hologramId);
            Entity hologram = world.getEntity(hologramUUID);
            if (hologram != null) {
                if (hologram.getEntityId() == packet.getIntegers().read(0)) {
                    updateHologramDependingOnId(packetEvent, client, hologramId, hologram);
                }
            }
        }
    }

    public void updateHologramDependingOnId (PacketEvent packetEvent, Player player, int id, Entity hologram) {

        PacketContainer packet = null;

        if (id == 12) {
            packet = updatePersonalHologram(player);
        }

        int playerPage = playerPages.getOrDefault(player.getUniqueId(), 0);
        if ((id > 0 && id <= 10)) {
            // Get specific row component for page specified if the page is not 0
            Component hologramComponent = getHologramComponentForPage(id - 1, playerPage);
            packet = updateNametag(player, hologram, componentToString(hologramComponent));
        }

        if (id == 13) {
            Component pageComponent = getPageComponent(playerPage);
            packet = updateNametag(player, hologram, componentToString(pageComponent));
        }

        if (packet != null) {
            if (packetEvent.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
                packetEvent.setPacket(packet);
            }
            else if (packetEvent.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                } catch (NullPointerException ignored) {}
            }
        }
    }

    public PacketContainer updateNametag (Player player, Entity entity, String nametag) {

        WrappedDataWatcher dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone();
        WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
        WrappedDataWatcher.WrappedDataWatcherObject watcherObject = new WrappedDataWatcher.WrappedDataWatcherObject(2, chatSerializer);
        Optional<Object> optional = Optional.of(WrappedChatComponent.fromChatMessage(nametag)[0].getHandle());
        dataWatcher.setObject(watcherObject, optional);

        if (Objects.equals(nametag, "")) {
            dataWatcher.setObject(3, false);
        }
        else {
            dataWatcher.setObject(3, true);
        }

        PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getWatchableCollectionModifier().write(0, dataWatcher.getWatchableObjects());
        packet.getIntegers().write(0, entity.getEntityId());

        return packet;
    }

    public void removeHolograms () {

        final double hologramX = -1064.5;
        final double hologramZ = -1673.5;
        final double topHologramY = 128.9;

        final World world = gameManager.getWorld();

        Chunk chunk = world.getChunkAt(new Location(world, hologramX, topHologramY, hologramZ));

        for (UUID uuid : holograms.values()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }
}
