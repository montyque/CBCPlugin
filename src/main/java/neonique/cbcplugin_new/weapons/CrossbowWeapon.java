package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import java.util.ArrayList;

public abstract class CrossbowWeapon {

    // Player holding weapon
    private CBCPlayer player;
    private CombatManager combatManager;
    private WeaponManager weaponManager;

    // Reloading variables
    private boolean loaded;
    private int reloadProgress;
    private int reloadTime;

    // Custom model data states
    private int modelLoadedState;
    private ArrayList<Integer> unloadedStates;

    public CrossbowWeapon () {

    }

    public float getReloadPercentage () {
        return (float) (reloadProgress / reloadTime);
    }

    public void updateReload () {
        if (loaded) {

        }
        else {
            // Add to reload progress
            reloadProgress++;
            // Check if weapon is fully loaded
            if (reloadTime != reloadProgress) {
                loaded = true;
            }
        }
    }

    public abstract void getWeaponItem ();

    public abstract void weaponFired ();



}
