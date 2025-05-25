package neonique.cbcplugin_new.cbcevents;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class CBCEventImageGenerator {

    private final static HashMap<Integer, Color> redToGoldConversion;

    static {
        redToGoldConversion = new HashMap<>();
        redToGoldConversion.put(new Color(255, 66, 66).getRGB(), new Color(249, 219, 86, 255));
        redToGoldConversion.put(new Color(88, 0, 1).getRGB(), new Color(144, 56, 32, 255));
        redToGoldConversion.put(new Color(188, 0, 0).getRGB(), new Color(247, 133, 46, 255));
        redToGoldConversion.put(new Color(255, 109, 109).getRGB(), new Color(246, 255, 189, 255));
    }

    private final File pluginDataFolder;

    public CBCEventImageGenerator () {
        pluginDataFolder = CBCPlugin.getPlugin().getDataFolder();
    }

    public BufferedImage generateImage (int gameNumber, CBCGamemode gamemode, String mapName, CBCEventTeam winningTeam) {

        File assetsFolder = new File(pluginDataFolder, "assets");
        if (!assetsFolder.exists()) {
            CBCPlugin.getPlugin().getLogger().warning("'assets' folder not found in CBCPlugin folder");
            return null;
        }

        File cbcEventImagesFolder = new File(assetsFolder, "cbceventimages");
        if (!cbcEventImagesFolder.exists()) {
            CBCPlugin.getPlugin().getLogger().warning("'cbceventimages' folder not found in CBCPlugin/assets folder");
            return null;
        }

        File gamemodeIconsFolder = new File(assetsFolder, "gamemodeicons");
        if (!gamemodeIconsFolder.exists()) {
            CBCPlugin.getPlugin().getLogger().warning("'gamemodeicons' folder not found in CBCPlugin/assets folder");
            return null;
        }

        File backgroundFile;
        File gameNumberFile;

        switch (gameNumber) {
            case 1:
                backgroundFile = new File(cbcEventImagesFolder, "game1map.png");
                gameNumberFile = new File(cbcEventImagesFolder, "game1number.png");
                break;
            case 2:
                backgroundFile = new File(cbcEventImagesFolder, "game2map.png");
                gameNumberFile = new File(cbcEventImagesFolder, "game2number.png");
                break;
            case 3:
                backgroundFile = new File(cbcEventImagesFolder, "game3map.png");
                gameNumberFile = new File(cbcEventImagesFolder, "game3number.png");
                break;
            case 4:
                backgroundFile = new File(cbcEventImagesFolder, "finalmap.png");
                gameNumberFile = new File(cbcEventImagesFolder, "finalnumber.png");
                break;
            default:
                CBCPlugin.getPlugin().getLogger().warning("Invalid input for game number");
                return null;
        }

        BufferedImage mainImage = convertToARGB(getImageFromFile(backgroundFile));
        if (mainImage == null) {
            return null;
        }

        Graphics2D g = mainImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        BufferedImage gameNumberImage = convertToARGB(getImageFromFile(gameNumberFile));
        if (gameNumberImage == null) {
            return null;
        }

        // Paste the game number onto the background image in the right position
        g.drawImage(gameNumberImage, 28, 12, null);

        File gamemodeIconImageFile = new File(gamemodeIconsFolder, gamemode.name().toLowerCase() + ".png");

        BufferedImage gamemodeIconImage = convertToARGB(getImageFromFile(gamemodeIconImageFile));
        gamemodeIconImage = convertToGold(gamemodeIconImage);

        Image scaledImage = gamemodeIconImage.getScaledInstance(96, 96, Image.SCALE_SMOOTH);
        g.drawImage(scaledImage, 145, 16, 96, 96, null);

        String gamemodeName = gamemode.getGamemodeName();

        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, new File(assetsFolder, "non.ttf"));
            font = font.deriveFont(14.0f * (4f / 3f));
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }

        // Due to the weird nature of the font, spacing is weird
        // Use two spaces for every space in gamemode name and map name
        String gamemodeNameDisplay = gamemodeName.replaceAll(" ", "   ").toUpperCase();
        String mapNameDisplay = mapName.replaceAll(" ", "   ").toUpperCase();

        // Draw text with outline and stripes clips
        BufferedImage glistenClip = convertToARGB(getImageFromFile(new File(cbcEventImagesFolder, "textclip.png")));
        drawOutlinedText(g, gamemodeNameDisplay, 264, 32, font, new Color(249, 219, 86), new Color(144, 56, 32), glistenClip);
        drawOutlinedText(g, mapNameDisplay, 264, 51, font, new Color(249, 219, 86), new Color(144, 56, 32), glistenClip);

        // Draw Game MVP text
        int width = g.getFontMetrics().stringWidth("GAME  MVP");
        int gameMvpX = Math.round(640 + 64 - (width / 2f));

        drawOutlinedText(g, "GAME  MVP", gameMvpX, 20, font, new Color(249, 219, 86), new Color(144, 56, 32), glistenClip);

        String flagFileName = "normalflag.png";
        if (winningTeam != null) {
            flagFileName = winningTeam.getTeamId().toLowerCase() + "flag.png";
        }

        BufferedImage flagImage = convertToARGB(getImageFromFile(new File(cbcEventImagesFolder, flagFileName)));
        if (flagImage == null) {
            return null;
        }

        // Paste the colored flag onto the background image in the right position
        g.drawImage(flagImage, 481, 14, null);


        return mainImage;

    }

    public BufferedImage getImageFromFile (File imageFile) {

        if (!imageFile.exists()) {
            System.out.println("[ERROR] File '" + imageFile.getName() + "' does not exist.");
            return null;
        }

        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            System.out.println("[ERROR] Could not open file '" + imageFile.getName() + ": " + e);
            return null;
        }

    }

    public BufferedImage convertToGold (BufferedImage image) {

        BufferedImage newImage = convertToARGB(image);
        for (int x = 0; x < newImage.getWidth(); x++) {
            for (int y = 0; y < newImage.getHeight(); y++) {
                Color pixelColor = new Color(newImage.getRGB(x, y));
                Color newColor = redToGoldConversion.getOrDefault(pixelColor.getRGB(), null);
                if (newColor == null) continue;
                newImage.setRGB(x, y, newColor.getRGB());
            }
        }
        return newImage;

    }

    public BufferedImage convertToARGB (BufferedImage image) {

        if (image == null) return null;

        BufferedImage newImage = new BufferedImage(
                image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return newImage;
    }

    public void drawOutlinedText (Graphics2D g, String text, int x, int y, Font font,
                                  Color fillColor, Color outlineColor, Image textClip) {

        g.setFont(font);

        GlyphVector glyphVector = font.createGlyphVector(g.getFontRenderContext(), text);
        Shape textShape = glyphVector.getOutline();

        //
        g.setColor(outlineColor);
        g.setStroke(new BasicStroke(4.0f));

        // Add extra thickness at the bottom of the text
        g.translate(x, y + 1);
        g.draw(textShape);

        // Draw normal outline
        g.translate(0, -1);
        g.draw(textShape);

        // Fill in text
        g.setColor(fillColor);
        g.fill(textShape);

        if (textClip != null) {
            g.setClip(textShape);
            g.drawImage(textClip, -x, -y, null);
            g.setClip(null);
        }

        // Reset translation to reset the origin back to what it was
        g.translate(-x, -y);

    }

}
