package neonique.cbcplugin_new.gamemodes;

import neonique.cbcplugin_new.mapconfig.GamemodeMapData;

import java.util.Map;

public interface GameContext {

    GamemodeMapData mapData ();

    Map<String, Boolean> boolVars ();
    Map<String, Integer> intVars ();
    Map<String, String> stringVars ();

}
