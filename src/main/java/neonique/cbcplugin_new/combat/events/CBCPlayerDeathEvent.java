package neonique.cbcplugin_new.combat.events;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class CBCPlayerDeathEvent extends Event {

    private final CBCPlayer victim;
    private final CBCPlayer killer;
    private final DeathCause cause;
    private final boolean direct;

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    public CBCPlayerDeathEvent (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {
        this.victim = victim;
        this.killer = killer;
        this.cause = cause;
        this.direct = direct;
    }

    public CBCPlayer victim () {
        return victim;
    }

    public CBCPlayer killer () {
        return killer;
    }

    public DeathCause cause () {
        return cause;
    }

    public boolean direct () {
        return direct;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

}
