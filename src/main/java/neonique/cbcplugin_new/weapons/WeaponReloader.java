package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.combat.CombatManager;

public class WeaponReloader {

    // Reloading variables
    private boolean loaded;
    private int reloadProgress;
    private int reloadTime;

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public void setReloadProgress(int reloadProgress) {
        this.reloadProgress = Math.max(reloadProgress, 0);
    }

    public void startReload () {
        this.reloadProgress = -1;
        loaded = false;
        updateReload();
    }

    public void updateReload () {
        if (!loaded) {
            reloadProgress++;
            if (reloadProgress >= reloadTime) {
                reloadProgress = reloadTime;
                loaded = true;
            }
        }
    }

    /**
     Gets the proportion of which the reload process has completed.
     <p>If the time it takes the reload to weapon is 0, then this will always return 1.0.
     @return The proportion of which the weapon has reloaded
     */
    public float getReloadPercentage () {
        if (reloadTime > 0) {
            return ((float) reloadProgress / (float) reloadTime);
        } else {
            return 1;
        }
    }

    public boolean isLoaded () {
        return loaded;
    }

    public void setReloadBySecond(double seconds) {
        setReloadProgress(reloadTime - (int) Math.round(seconds * CombatManager.RELOAD_TASK_FREQUENCY));
    }
}
