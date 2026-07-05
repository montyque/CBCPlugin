package neonique.cbcplugin_new.gamemodes;

import neonique.cbcplugin_new.mapconfig.GamemodeMapData;

import java.util.Map;

public interface GameContext {

    GamemodeMapData mapData ();
    GameSettings gameSettings ();

}
