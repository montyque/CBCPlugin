package neonique.cbcplugin_new.core;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerStore {

    Collection<? extends CBCPlayer> getPlayers ();

    Optional<? extends CBCPlayer> getPlayerByUUID (UUID uuid);

    default CBCPlayer getPlayer (Entity entity) {
        return getPlayerByUUID(entity.getUniqueId()).orElse(null);
    }

    default CBCPlayer getPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId()).orElse(null);
    }

    default boolean hasPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId()).isPresent();
    }

}
