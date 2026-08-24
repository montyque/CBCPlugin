package neonique.cbcplugin_new.misc.imagemaps;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.lobby_old.LobbyImageMaps;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageMapRenderer extends MapRenderer {

    private final CBCPlugin plugin;
    private BufferedImage image = null;
    private boolean first = true;

    private final int x;
    private final int y;
    private final double scale;

    public ImageMapRenderer(CBCPlugin plugin, BufferedImage image, int x, int y, double scale) {
        this.plugin = plugin;
        this.x = x;
        this.y = y;
        this.scale = scale;
        recalculateInput(image);
    }

    public void recalculateInput(BufferedImage input) {
        if ((long) x * LobbyImageMaps.MAP_WIDTH > Math.round(input.getWidth() * scale)
                || (long) y * LobbyImageMaps.MAP_HEIGHT > Math.round(input.getHeight() * scale))
            return;

        int x1 = (int) Math.floor(x * LobbyImageMaps.MAP_WIDTH / scale);
        int y1 = (int) Math.floor(y * LobbyImageMaps.MAP_HEIGHT / scale);

        int x2 = (int) Math.ceil(Math.min(input.getWidth(), ((x + 1) * LobbyImageMaps.MAP_WIDTH / scale)));
        int y2 = (int) Math.ceil(Math.min(input.getHeight(), ((y + 1) * LobbyImageMaps.MAP_HEIGHT / scale)));

        if (x2 - x1 <= 0 || y2 - y1 <= 0)
            return;

        this.image = input.getSubimage(x1, y1, x2 - x1, y2 - y1);

        if (scale != 1D) {

            BufferedImage resized = new BufferedImage(LobbyImageMaps.MAP_WIDTH, LobbyImageMaps.MAP_HEIGHT,
                    input.getType() == 0 ? image.getType() : input.getType());
            AffineTransform at = new AffineTransform();

            at.scale(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            this.image = scaleOp.filter(this.image, resized);

        }

        first = true;
    }

    @Override
    public void render(@NotNull MapView view, @NotNull MapCanvas canvas, @NotNull Player player) {

        if (image != null && first) {
            new BukkitRunnable() {
                @Override
                public void run () {
                    canvas.drawImage(0, 0, image);
                }
            }.runTaskLater(plugin, System.nanoTime() % 60);

            first = false;
        }
    }
}
