package neonique.cbcplugin_new.mapmechanics;

public class SwimTimer {

    private final int max;
    private int current;

    private final int damageTicksMax;
    private int damageTicks;

    public SwimTimer (int max, int damageTicksMax) {
        this.max = max;
        this.current = max;
        this.damageTicksMax = damageTicksMax;
        this.damageTicks = 0;
    }

    public float getFraction () {
        return (float) current / max;
    }

    public void decrement () {
        current = Math.max(current - 1, 0);
    }

    public void increment () {
        current = Math.min(current + 1, max);
    }

    public boolean full () {
        return max >= current;
    }

    public boolean empty () {
        return current == 0;
    }

    public void decrementDamage () {
        damageTicks--;
    }

    public boolean damageEmpty () {
        return damageTicks == 0;
    }

    public void resetDamage () {
        damageTicks = damageTicksMax;
    }

    public void resetDamageZero () {
        damageTicks = 0;
    }

}
