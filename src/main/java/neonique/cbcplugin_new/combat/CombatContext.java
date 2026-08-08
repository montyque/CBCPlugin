package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;

public interface CombatContext {

    PlayerStore players();

    int timer();

    void playerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct);

}
