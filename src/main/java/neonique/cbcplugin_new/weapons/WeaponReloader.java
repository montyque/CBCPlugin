package neonique.cbcplugin_new.weapons;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
     Gets the percentage
     @return The proportion of which the weapon has reloaded
     */
    public float getReloadPercentage () {
        return ((float) reloadProgress / (float) reloadTime);
    }

    public boolean isLoaded () {
        return loaded;
    }

    public void setReloadBySecond(double seconds) {
        setReloadProgress(reloadTime - (int) Math.round(seconds * 10));
    }
}
