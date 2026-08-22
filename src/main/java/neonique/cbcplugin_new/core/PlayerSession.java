package neonique.cbcplugin_new.core;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface PlayerSession<P extends CBCPlayer> extends PlayerStore {

    P createPlayer (Player playerEntity);

    void addPlayer (P player);

    void removePlayer (P player);

    Collection<P> players();

    Optional<P> getPlayerByUUID (UUID uuid);

    default P getPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId()).orElse(null);
    }

    default boolean hasPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId()).isPresent();
    }

    default void removePlayerByBase (CBCPlayer player) {
        P typedPlayer = getPlayerByUUID(player.getUUID()).orElseThrow(IllegalArgumentException::new);
        removePlayer(typedPlayer);
    }

    default P createAndAddPlayer (Player playerEntity) {
        P typedPlayer = createPlayer(playerEntity);
        addPlayer(typedPlayer);
        return typedPlayer;
    }

    default Collection<P> playersWithinRadius (Location loc, double radius) {

        double r2 = radius * radius;
        return players().stream()
                .filter(CBCPlayer::isOnline)
                .filter(p -> p.getPlayer().getLocation().distanceSquared(loc) <= r2)
                .collect(Collectors.toUnmodifiableSet());

    }

    default List<String> playerNames () {
        return players().stream()
                .map(CBCPlayer::name)
                .toList();
    }

}
