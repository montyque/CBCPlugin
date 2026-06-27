package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.setTextFont;

public interface PlayerLike {

    UUID uuid ();

    default Player getPlayer () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid());
        if (player.getPlayer() != null) {
            return player.getPlayer();
        } else {
            return null;
        }
    }

    default String name () {
        return getOfflinePlayer().getName();
    }

    default OfflinePlayer getOfflinePlayer () {
        return Bukkit.getOfflinePlayer(uuid());
    }

    default boolean isOnline () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid());
        return player.isOnline();
    }

    default TextColor nameColor () {
        return NamedTextColor.WHITE;
    }

    default Component nameComponent () {
        return Component.text(name()).color(nameColor());
    }

    default Component nameComponent(ResourcePackFont font) {
        return setTextFont(name(), font).color(nameColor());
    }

}
