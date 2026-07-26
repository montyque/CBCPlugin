package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerSession;

public interface MapMechanic {

    void activate (PlayerSession<? extends CBCPlayer> players);

    void deactivate ();

}
