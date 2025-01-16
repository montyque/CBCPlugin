package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.misc.Tuple;
import neonique.cbcplugin_new.misc.imagemaps.ImageMap;
import neonique.cbcplugin_new.misc.imagemaps.LobbyImageMapRenderer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;

public class LobbyImageMaps {

    private Lobby lobby;

    public static int MAP_WIDTH = 128;
    public static int MAP_HEIGHT = 128;

    private static final String IMAGES_DIR = "mapdisplays";

    private HashMap<String, BufferedImage> imageCache = new HashMap<>();
    private HashMap<ImageMap, Integer> maps = new HashMap<>();

    private ArrayList<UUID> itemFrames = new ArrayList<>();

    public LobbyImageMaps(Lobby lobby) {
        this.lobby = lobby;
    }

    public void createItemFrames() {

        double startingX = -1072.5;
        double startingY = 128.5;
        double z = -1678.96875;

        for (int x = 0; x < 7; x++) {
            for (int y = 0; y > -4; y--) {

                if (y == -3) {
                    lobby.world.getBlockAt(new Location(lobby.world, startingX + (double) x - 0.5, startingY + (double) y, z)).setType(Material.AIR);
                }

                ItemFrame frame = lobby.world.spawn(
                        new Location(lobby.world, startingX + (double) x, startingY + (double) y, z),
                        ItemFrame.class);

                // Create item frames
                frame.setFacingDirection(BlockFace.SOUTH);
                frame.setItem(null);
                frame.setVisible(false);
                frame.setFixed(true);

                itemFrames.add(frame.getUniqueId());
            }
        }

        for (int x = 0; x < 7; x++) {
            lobby.world.getBlockAt(new Location(lobby.world, -1073 + (double) x, 125, z)).setType(Material.SPRUCE_SLAB);
        }
    }

    public void deleteItemFrames() {
        for (UUID itemFrameUUID : itemFrames) {
            Entity itemFrame = getServer().getEntity(itemFrameUUID);
            if (itemFrame != null) {
                itemFrame.remove();
            }
        }

        // Remove any item frames 4 blocks nearby
        for (Entity entity : new Location(lobby.world, -1070, 127, -1680).getNearbyEntitiesByType(ItemFrame.class, 4.0)) {
            if (entity.getType() == EntityType.ITEM_FRAME) {
                entity.remove();
            }
        }

        itemFrames.clear();
    }

    public void setImage(String filename) {

        clearImage();

        if (filename == null) {
            System.out.println("No file name");
            return;
        };

        BufferedImage image = getImage(filename);
        if (image == null) {
            System.out.println("No image found");
            return;
        };

        int index = 0;
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 4; y++) {

                if (itemFrames.size() <= index) {
                    continue;
                }

                UUID itemFrameUUID = itemFrames.get(index);
                Entity entity = getServer().getEntity(itemFrameUUID);
                if (entity == null) continue;
                if (entity.getType() == EntityType.ITEM_FRAME) {
                    ItemFrame itemFrame = (ItemFrame) entity;
                    itemFrame.setItem(getMapItem(image, x, y, filename, new Tuple<>(7, 4)));
                }

                index++;
            }
        }

    }

    public void clearImage() {
        for (UUID itemFrameUUID : itemFrames) {
            Entity entity = getServer().getEntity(itemFrameUUID);
            if (entity == null) continue;
            if (entity.getType() == EntityType.ITEM_FRAME) {
                ItemFrame itemFrame = (ItemFrame) entity;
                itemFrame.setItem(null);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack getMapItem(BufferedImage image, int x, int y, String filename, Tuple<Integer, Integer> size) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);

        ImageMap imageMap = new ImageMap(filename, x, y, getScale(image, size));
        if (maps.containsKey(imageMap)) {
            MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapId(maps.get(imageMap));
            item.setItemMeta(meta);
            return item;
        }

        MapView map = getServer().createMap(getServer().getWorlds().get(0));
        map.getRenderers().forEach(map::removeRenderer);
        map.addRenderer(new LobbyImageMapRenderer(CBCPlugin.getPlugin(), image, x, y, getScale(image, size)));
        map.setTrackingPosition(false);

        MapMeta meta = ((MapMeta) item.getItemMeta());
        meta.setMapView(map);
        item.setItemMeta(meta);
        maps.put(imageMap, map.getId());

        return item;
    }

    public BufferedImage getImage(String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            CBCPlugin.getPlugin().getLogger().warning("Someone tried to get image with illegal characters in file name.");
            return null;
        }

        if (imageCache.containsKey(filename.toLowerCase()))
            return imageCache.get(filename.toLowerCase());

        File file = new File(CBCPlugin.getPlugin().getDataFolder(), IMAGES_DIR + File.separatorChar + filename);
        BufferedImage image = null;

        if (!file.exists())
            return null;

        try {
            image = ImageIO.read(file);
            imageCache.put(filename.toLowerCase(), image);
        }
        catch (IOException e) {
            CBCPlugin.getPlugin().getLogger().log(Level.SEVERE, String.format("Error while trying to read image %s.", file.getName()), e);
        }

        if (image == null)
            CBCPlugin.getPlugin().getLogger().log(Level.WARNING, () -> String.format("Failed to read file as image %s.", file.getName()));

        return image;
    }

    public double getScale(String filename, Tuple<Integer, Integer> size) {
        return getScale(getImage(filename), size);
    }

    public double getScale(BufferedImage image, Tuple<Integer, Integer> size) {
        if (image == null)
            return 1.0;

        int baseX = image.getWidth();
        int baseY = image.getHeight();

        double finalScale = 1D;

        if (size != null) {
            int targetX = size.getKey() * MAP_WIDTH;
            int targetY = size.getValue() * MAP_HEIGHT;

            double scaleX = size.getKey() > 0 ? (double) targetX / baseX : Double.MAX_VALUE;
            double scaleY = size.getValue() > 0 ? (double) targetY / baseY : Double.MAX_VALUE;

            finalScale = Math.min(scaleX, scaleY);
            if (finalScale >= Double.MAX_VALUE)
                finalScale = 1D;
        }

        return finalScale;
    }

}
