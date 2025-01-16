package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;

public class PlayerStatObject {

    private final CBCPlayer player;
    private final int value;
    private int placement;
    private boolean placementTied;

    public PlayerStatObject(CBCPlayer player, int value) {
        this.player = player;
        this.value = value;
    }

    public void setPlacement (int placement, boolean tied) {
        this.placement = placement;
        this.placementTied = tied;
    }

    public int getValue() {
        return value;
    }

    public CBCPlayer getPlayer() {
        return player;
    }

    public int getPlacement () {
        return placement;
    }

    public boolean isPlacementTied() {
        return placementTied;
    }
}
