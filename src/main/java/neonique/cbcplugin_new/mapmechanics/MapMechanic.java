package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;

public interface MapMechanic {

    void activate (PlayerRegistry registry, CombatManager combatManager);

    void deactivate ();

}
