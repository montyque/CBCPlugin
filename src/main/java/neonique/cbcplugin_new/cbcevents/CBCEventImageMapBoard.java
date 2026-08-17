package neonique.cbcplugin_new.cbcevents;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.misc.imagemaps.ImageMapRenderer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.image.BufferedImage;
import java.util.Collection;

public class CBCEventImageMapBoard {

    private final static int mapStartingId = 69509;
    private final static int[][] mapIds = {
            {69509, 69510, 69511, 69512, 69513, 69514},
            {69655, 69656, 69657, 69658, 69659, 69660},
            {69661, 69662, 69663, 69664, 69665, 69666},
            {69667, 69668, 69669, 69670, 69671, 69672},
            {69673, 69674, 69675, 69676, 69677, 69678},
    };

    private final int[] size;

    private final CBCEventManager eventManager;

    private final BlockFace directionFacing;
    private final Location topLeft;

    private final CBCEventImageGenerator imageGenerator;

    public CBCEventImageMapBoard (CBCEventManager eventManager, BlockFace directionFacing, Location topLeftBlock, int rows) {

        this.eventManager = eventManager;
        this.directionFacing = directionFacing;
        this.topLeft = topLeftBlock;

        this.imageGenerator = new CBCEventImageGenerator();

        size = new int[]{6, rows};

    }

    public void placeItemFrames () {

        clearAllMVPHeads();

        Block locationBlock = topLeft.getBlock();
        Block b = locationBlock.getRelative(directionFacing);
        BlockFace widthDirection = calculateWidthDirection(directionFacing);

        // Check for any item frames or paintings are currently placed near the location, and remove them if so
        for (int x = 0; x < size[0]; x++) {
            for (int y = 0; y < size[1]; y++) {
                Block frameBlock = b.getRelative(widthDirection, x).getRelative(BlockFace.DOWN, y);
                Collection<Entity> hangingEntitiesNearby = b.getWorld().getNearbyEntities(
                        frameBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5,
                        Hanging.class::isInstance);

                for (Entity entity : hangingEntitiesNearby) {
                    entity.remove();
                }
            }
        }

        // Create the new item frames
        for (int x = 0; x < size[0]; x++) {
            for (int y = 0; y < size[1]; y++) {
                ItemFrame frame = locationBlock.getWorld().spawn(
                        b.getRelative(widthDirection, x).getRelative(BlockFace.DOWN, y).getLocation(), ItemFrame.class);

                frame.setFacingDirection(directionFacing);
                frame.setItem(getMapItem(x, y));

                frame.setInvisible(true);
                frame.setFixed(true);

            }
        }
    }

    public void reloadAllRows () {
        for (int i = 0; i < mapIds.length; i++) {
            reloadRow(i);
        }
    }

    public void reloadRow (int rowY) {

        int gameNumber = rowY + 1;

        CBCGamemode gamemode = eventManager.getGamemodeNameForGame(gameNumber);
        String mapName = eventManager.getMapNameForGame(gameNumber);
        CBCEventTeam winningTeam = eventManager.getGameWinner(gameNumber);

        BufferedImage image = imageGenerator.generateImage(gameNumber, gamemode, mapName, winningTeam);

        int[] rowMapIds = mapIds[rowY];
        int i = 0;

        for (int id : rowMapIds) {
            MapView map = Bukkit.getMap(id);
            if (map != null) {
                map.getRenderers().forEach(map::removeRenderer);
                map.addRenderer(new ImageMapRenderer(CBCPlugin.getPlugin(), image, i, 0, 1.0));
                map.setTrackingPosition(false);
            }
            i++;
        }

    }

    public void clearAllMVPHeads () {
        for (int y = 0; y < size[1]; y++) {
            setMVPHead(null, y);
        }
    }

    public void setMVPHead (OfflinePlayer owningPlayer, int rowY) {

        Block locationBlock = topLeft.getBlock();
        BlockFace widthDirection = calculateWidthDirection(directionFacing);

        Block headBlock = locationBlock.getRelative(directionFacing).getRelative(widthDirection, size[0] - 1)
                .getRelative(BlockFace.DOWN, rowY);

        if (owningPlayer == null) {
            headBlock.setType(Material.AIR);
            return;
        }

        headBlock.setType(Material.PLAYER_WALL_HEAD);

        // Set the skin of the player wall head
        Skull head = (Skull) headBlock.getState();
        head.setOwningPlayer(owningPlayer);
        head.update();

        new BukkitRunnable() {
            @Override
            public void run() {
                Directional headData = (Directional) headBlock.getBlockData();
                headData.setFacing(directionFacing);
                headBlock.setBlockData(headData);
            }
        }.runTaskLater(CBCPlugin.getPlugin(), 1);

    }

    private ItemStack getMapItem (int x, int y) {

        ItemStack item = new ItemStack(Material.FILLED_MAP);

        MapMeta meta = (MapMeta) item.getItemMeta();
        MapView mapView = Bukkit.getServer().getMap(mapIds[y][x]);

        if (mapView == null) return null;

        meta.setMapView(mapView);
        item.setItemMeta(meta);

        return item;

    }

    private static BlockFace calculateWidthDirection(BlockFace face) {

        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case WEST -> BlockFace.SOUTH;
            default -> throw new RuntimeException();
        };

    }

}
