package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;
import org.bukkit.plugin.Plugin;

public interface CombatContext {

    PlayerStore players();

    int timer();

    void playerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct);

    Plugin plugin ();

    default void playerDeath (CBCPlayer victim, DeathCause cause) {
        playerDeath(victim, victim.getLastPlayerHitBy(), cause, false);
    }

}
