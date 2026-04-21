package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerRegistry {

    private PlayerSession<? extends CBCPlayer> activeSession;

    public void bind (PlayerSession<? extends CBCPlayer> activeSession) {
        this.activeSession = activeSession;
    }

    public void clear () {
        this.activeSession = null;
    }

    public Collection<? extends CBCPlayer> getPlayers () {
        return activeSession.getPlayers();
    }

    public Collection<Player> getPlayerEntities () {
        return getPlayers().stream()
                .map(CBCPlayer::getPlayer)
                .collect(Collectors.toSet());
    }

    public CBCPlayer getPlayerByUUID (UUID uuid) {
        return activeSession.getPlayerByUUID(uuid).orElse(null);
    }

    public CBCPlayer getPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId());
    }

    public boolean hasPlayer (Player player) {
        return getPlayerByUUID(player.getUniqueId()) != null;
    }

}
