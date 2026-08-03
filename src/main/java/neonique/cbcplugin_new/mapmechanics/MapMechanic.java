package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerSession;
import neonique.cbcplugin_new.core.PlayerStore;

public interface MapMechanic {

    void activate (PlayerStore players);

    void deactivate ();

}
