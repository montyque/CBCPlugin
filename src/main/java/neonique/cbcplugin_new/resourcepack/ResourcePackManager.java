package neonique.cbcplugin_new.resourcepack;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCauses;
import neonique.cbcplugin_new.enums.ResourcePackFont;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class ResourcePackManager {

    private HashMap<UUID, PlayerHead> playerHeads = new HashMap<>();

    private final String worldName = "world";
    private final String zipName = "resources.zip";

    private final String slotSectionName = "PlayerUUIDSlots";
    private final String recentPlayersListName = "MostRecentPlayers";
    private final String headsNotUpdated = "HeadsNotUpdated";
    private final int playerMax = 64;

    private final float translucentHeadOpacity = 0.2627f;

    public void addPlayerHead (UUID playerUUID, String playerName, CBCPlugin plugin) {

        // Get config file
        File file = getPlayerHeadSlotsFile(plugin);
        if (file == null) {
            CBCPlugin.getPlugin().getLogger().warning("Could not add player " + playerName + " to player head slots file.");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Retrieve player UUID
        String stringUUID = playerUUID.toString();

        // Read section
        ConfigurationSection slotSection = config.getConfigurationSection(slotSectionName);
        List<String> stringList = config.getStringList(recentPlayersListName);
        List<String> newHeadsList = config.getStringList(headsNotUpdated);

        assert slotSection != null;

        boolean wasNotInPack = false;

        // Check if player is in the list
        int playerSlot;
        if (stringList.contains(stringUUID)) {
            // Move player up to top of list
            stringList.remove(stringUUID);
            stringList.add(0, stringUUID);

            playerSlot = slotSection.getInt(stringUUID, -1);
        }
        // If not, check if list is maxed out at 64
        else {

            String removedUUID = "";
            wasNotInPack = true;
            if (stringList.size() == playerMax) {
                // Remove last player
                removedUUID = stringList.remove(63);
                slotSection.set(removedUUID, null);
            }

            // Give player a slot
            Set<Integer> slots = new HashSet<>();
            for (int i = 0; i < playerMax; i++) {
                slots.add(i);
            }

            for (String uuid : slotSection.getKeys(true)) {
                if (Objects.equals(uuid, removedUUID)) continue;

                int slotForPlayer = slotSection.getInt(uuid, -1);
                slots.remove(slotForPlayer);
            }

            playerSlot = Collections.min(slots);

            // Add new player
            stringList.add(0, stringUUID);
        }

        if (playerSlot == -1) {
            // Give player a slot
            Set<Integer> slots = new HashSet<>();
            for (int i = 0; i < playerMax; i++) {
                slots.add(i);
            }

            for (String uuid : slotSection.getKeys(true)) {
                int slotForPlayer = slotSection.getInt(uuid, -1);
                slots.remove(slotForPlayer);
            }

            playerSlot = Collections.min(slots);
        }

        boolean newHead = savePlayerHead(playerUUID, playerName, playerSlot);

        boolean oneCharacterHead = true;

        if (newHeadsList.contains(stringUUID) || newHead || wasNotInPack) {
            oneCharacterHead = false;
            if (!newHeadsList.contains(stringUUID)) {
                newHeadsList.add(stringUUID);
            }

            // Send messages to console about the player
            if (newHead) {
                CBCPlugin.getPlugin().getLogger().info(
                        "Player " + playerName + "'s head has changed. Rewriting head in resource pack..."
                );
            }

            CBCPlugin.getPlugin().getLogger().info(
                    "Player " + playerName + "'s head has not been updated on the resource pack, so will use pixel created head instead."
            );
        }
        else {
            CBCPlugin.getPlugin().getLogger().info(
                    "Player " + playerName + "'s head is saved on the resource pack, so will use singular unicode character."
            );
        }


        config.set(recentPlayersListName, stringList);
        slotSection.set(stringUUID, playerSlot);
        config.set(headsNotUpdated, newHeadsList);

        // Create player head object
        int finalPlayerSlot = playerSlot;
        createPlayerHeadComponent(playerUUID, oneCharacterHead, finalPlayerSlot);

        // Save to file
        try {
            config.save(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlayerHeadNew (BufferedImage source, BufferedImage head, int slot) {

        BufferedImage oldHead = source.getSubimage((slot % 8) * 8, (slot / 8) * 8, 8, 8);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (head.getRGB(x, y) != oldHead.getRGB(x, y)) {
                    return true;
                }
            }
        }

        return false;

    }

    @SuppressWarnings("deprecated")
    public boolean savePlayerHead (UUID playerUUID, String playerName, int slot) {

        String uuidString = playerUUID.toString();
        URL playerHeadUrl;

        // Remove dashes from string -- sometimes non dash version is wrong
        uuidString = uuidString.replace("-", "");

        try {
            playerHeadUrl = new URL("https://crafatar.com/avatars/" + uuidString + "?overlay");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HashMap<String, String> env = new HashMap<>();
        env.put("create", "true");

        File zipFile = getZipFile();
        if (zipFile == null) {
            CBCPlugin.getPlugin().getLogger().warning("Resource pack zip file not found.");
            return true;
        }
        Path path = Paths.get(zipFile.getPath());

        URI uri = URI.create("jar:" + path.toUri());
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {

            // Create a BufferedImage object
            BufferedImage playerHeadImg;

            try (InputStream in = playerHeadUrl.openStream()){
                playerHeadImg = ImageIO.read(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Resize the image to be 8 by 8
            BufferedImage playerHeadImg8x8 = resizePlayerHead(playerHeadImg, 8);

            // Get transparent version of head
            BufferedImage translucentPlayerHeadImg = changePlayerHeadOpacity(playerHeadImg8x8, translucentHeadOpacity);

            // Get the 64x64 image with the player heads
            boolean isNewHead = false;

            Path plrHeadsPath = fs.getPath("assets", "minecraft", "textures", "font", "playerheads.png");
            BufferedImage playerHeadsImg = loadImage(plrHeadsPath);
            if (playerHeadsImg != null) {

                // Check if same
                isNewHead = isPlayerHeadNew(playerHeadsImg, playerHeadImg8x8, slot);

                // Paste both opaque and translucent heads onto image
                BufferedImage newPlayerHeadsImg = pastePlayerHead(playerHeadsImg, playerHeadImg8x8, (slot % 8) * 8, (slot / 8) * 8);
                if (newPlayerHeadsImg != null) {
                    newPlayerHeadsImg = pastePlayerHead(newPlayerHeadsImg, translucentPlayerHeadImg, (slot % 8) * 8, (slot / 8) * 8 + playerMax);
                }
                // Save image
                if (newPlayerHeadsImg != null) {
                    saveImage(newPlayerHeadsImg, plrHeadsPath);
                }

            }

            // Get the 128x128 image with the raised player heads
            Path plrHeadsRaisedPath = fs.getPath("assets", "minecraft", "textures", "font", "playerheads_24up.png");
            BufferedImage playerRaisedHeadsImg = loadImage(plrHeadsRaisedPath);
            if (playerHeadsImg != null) {
                BufferedImage newPlayerRaisedHeadsImg = pastePlayerHead(playerRaisedHeadsImg, playerHeadImg8x8, (slot % 16) * 8, (slot / 16) * 32);
                if (newPlayerRaisedHeadsImg != null) {
                    newPlayerRaisedHeadsImg = pastePlayerHead(newPlayerRaisedHeadsImg, translucentPlayerHeadImg, (slot % 16) * 8, (slot / 16) * 32 + playerMax * 2);
                }
                // Save image
                if (newPlayerRaisedHeadsImg != null) {
                    saveImage(newPlayerRaisedHeadsImg, plrHeadsRaisedPath);
                }
            }

            return isNewHead;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getPlayerHeadSlotsFile (CBCPlugin plugin) {

        File dataFolder = CBCPlugin.getPlugin().getDataFolder();

        File playerHeadSlotsFile = new File(dataFolder, "playerheadslots.yaml");
        if (!playerHeadSlotsFile.exists()) {
            // Create the file
            try{
                boolean works = playerHeadSlotsFile.createNewFile(); // and here
                if (!works) {
                    CBCPlugin.getPlugin().getLogger().warning("Failed to generate playerheadslots.yaml file");
                } else {
                    CBCPlugin.getPlugin().getLogger().info("Successfully generated playerheadslots.yaml file");

                    // Add configuration section
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(playerHeadSlotsFile);

                    config.createSection(slotSectionName); // The numbered slots of each player are stored here

                    config.set(recentPlayersListName, new ArrayList<String>()); // The most 32 recent players to log on
                                                                                // the server are here -- by UUID
                    config.save(playerHeadSlotsFile);

                    return playerHeadSlotsFile;
                }
            } catch(SecurityException | IOException e) {
                CBCPlugin.getPlugin().getLogger().warning("Failed to generate playerheadslots.yaml file");
            }
            return null;
        }

        // Return configuration section
        return playerHeadSlotsFile;
    }

    private File getWorldFolder() {
        File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            return worldFolder;
        } else {
            return null;
        }
    }

    private File getZipFile() {

        File worldFolder = getWorldFolder();
        if (worldFolder == null) {
            CBCPlugin.getPlugin().getLogger().warning("[!] World folder not found.");
            return null;
        }

        File zipFile = new File(worldFolder, zipName);
        if (zipFile.exists()) {
            return zipFile;
        } else {
            return null;
        }
    }

    // Used to resize the player head
    private static BufferedImage resizePlayerHead(BufferedImage plrHead, int size) {
        Image tmp = plrHead.getScaledInstance(size, size, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(tmp, 0, 0, size, size,null);
        g2d.dispose();

        return dimg;
    }

    private static BufferedImage changePlayerHeadOpacity (BufferedImage plrHead, float opacity) {
        BufferedImage dimg = new BufferedImage(plrHead.getWidth(), plrHead.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = dimg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.drawImage(plrHead, 0, 0, plrHead.getWidth(), plrHead.getHeight(),null);
        g2d.dispose();
        return dimg;
    }

    private static BufferedImage pastePlayerHead(BufferedImage bg, BufferedImage plrHead, int x, int y) {
        try
        {

            for (int xz = x; xz < x + plrHead.getWidth(); xz++) {
                for (int yz = y; yz < y + plrHead.getHeight(); yz++) {
                    bg.setRGB(xz, yz, 0);
                }
            }

            Graphics g = bg.getGraphics();
            g.drawImage(plrHead, x, y, null);
            g.dispose();
            return bg;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static void saveImage(BufferedImage image, Path path) {
        try {
            Path tempFile = Files.createTempFile(null, "png");
            ImageIO.write(image, "png", tempFile.toFile());
            Files.copy(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            CBCPlugin.getPlugin().getLogger().warning("Error while attempting to save image.");
            e.printStackTrace();
        }
    }

    private static BufferedImage loadImage(Path path) {
        BufferedImage img;
        try {
            img = ImageIO.read(Files.newInputStream(path));
            return img;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createPlayerHeadComponent (UUID playerUUID, boolean oneCharacterHead, int slot) {

        String uuidString = playerUUID.toString();
        URL playerHeadUrl;

        PlayerHead newPlayerHead = new PlayerHead(playerUUID);

        if (oneCharacterHead) {
            newPlayerHead.setHeadOneCharacter(slot);
        }
        else {
            try {
                playerHeadUrl = new URL("https://mc-heads.net/avatar/" + uuidString);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            // Create a BufferedImage object
            BufferedImage playerHeadImg;
            try (InputStream in = playerHeadUrl.openStream()){
                playerHeadImg = ImageIO.read(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            BufferedImage resizedHead = resizePlayerHead(playerHeadImg, 8);

            newPlayerHead.setHead(resizedHead);
        }

        playerHeads.put(playerUUID, newPlayerHead);

    }

    public Component getPlayerHeadComponent (PlayerHeadType headType, OfflinePlayer player) {

        UUID playerUUID = player.getUniqueId();

        PlayerHead playerHeadObject = playerHeads.getOrDefault(playerUUID, null);

        if (playerHeadObject == null) {
            return normalText(String.valueOf((char) (headType.getStartingUnicodeId() + 63)));
        }

        return playerHeadObject.getPlayerHead(headType);
    }

    public static Component setFont (Component target, ResourcePackFont font) {
        return target.style(Style.style().font(font.getFontKey()));
    }

    public static Component setTextFont (String target, ResourcePackFont font) {
        return Component.text(target).style(Style.style().font(font.getFontKey()));
    }

    public static Component normalText (String target) {
        return setTextFont(target, ResourcePackFont.DEFAULT);
    }

    public static Component smallText (String target) {
        return setTextFont(target, ResourcePackFont.SMALL_5X5);
    }

    public static Component smallRaisedText (String target) {
        return setTextFont(target, ResourcePackFont.SMALL_5X5_RAISED);
    }

    public static Component noShadowText (Component target) {
        return target.color(TextColor.color(78, 92, 36));
    }

    public static Component getReloadBarComponent (WeaponType weaponType, float progress) {

        int charNum = (int) Math.ceil(progress * 60);

        if (weaponType == WeaponType.CREEPER) {
            charNum += 57344;
            return Component.text(String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars")));
        }
        else if (weaponType == WeaponType.FLAME) {
            charNum += 57600;
            return Component.text(String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars")));
        }
        else if (weaponType == WeaponType.XBOW) {
            charNum += 57856;
            return Component.text(String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars")));
        }
        return null;
    }

    public static Component getHotbarIcon (CBCTeam team, boolean showingReloadBars) {

        String hotbarIconChar = "\uE219";
        if (team != null) {
            NamedTextColor teamColor = team.getColor();
            if (teamColor == NamedTextColor.RED) hotbarIconChar = "\uE210";
            else if (teamColor == NamedTextColor.BLUE) hotbarIconChar = "\uE211";
            else if (teamColor == NamedTextColor.GREEN) hotbarIconChar = "\uE212";
            else if (teamColor == NamedTextColor.YELLOW) hotbarIconChar = "\uE213";
            else if (teamColor == NamedTextColor.AQUA) hotbarIconChar = "\uE214";
            else if (teamColor == NamedTextColor.GOLD) hotbarIconChar = "\uE215";
            else if (teamColor == NamedTextColor.LIGHT_PURPLE) hotbarIconChar = "\uE216";
            else if (teamColor == NamedTextColor.DARK_PURPLE) hotbarIconChar = "\uE217";
        }

        if (showingReloadBars) {
            return setFont(Component.text("\uF80B\uF80A\uF804" + hotbarIconChar + "\uF82B\uF829\uF823"), ResourcePackFont.DEFAULT);
        }
        else {
            return setFont(Component.text(hotbarIconChar), ResourcePackFont.DEFAULT);
        }
    }

    public static Component getDeathCauseIcon (DeathCauses cause, boolean isKiller, NamedTextColor color) {

        if (color == null) {
            color = NamedTextColor.WHITE;
        }

        if (cause == DeathCauses.CREEPER) {
            return setFont(Component.text("\uE400"), ResourcePackFont.DEFAULT).color(NamedTextColor.WHITE);
        }
        else if (cause == DeathCauses.FLAMEZONE) {
            return setFont(Component.text("\uE401"), ResourcePackFont.DEFAULT).color(NamedTextColor.WHITE);
        }
        else if (cause == DeathCauses.XBOW) {
            return setFont(Component.text("\uE402"), ResourcePackFont.DEFAULT).color(NamedTextColor.WHITE);
        }
        else if (cause == DeathCauses.MELEE) {
            return setFont(Component.text("\uE403"), ResourcePackFont.DEFAULT).color(NamedTextColor.WHITE);
        }
        else if (cause == DeathCauses.VOID) {
            return setFont(Component.text("\uE404"), ResourcePackFont.DEFAULT).color(color);
        }
        else {
            if (isKiller) {
                return setFont(Component.text("\uE405"), ResourcePackFont.DEFAULT).color(color);
            } else {
                return setFont(Component.text("\uE406"), ResourcePackFont.DEFAULT).color(color);
            }
        }
    }
}
