package neonique.cbcplugin_new.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

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

}
