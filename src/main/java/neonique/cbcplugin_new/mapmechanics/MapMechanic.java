package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerSession;
import neonique.cbcplugin_new.core.PlayerStore;

public interface MapMechanic {

    void activate (CombatContext combatContext);

    void deactivate ();

}
