package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;

public record DeathInfo(CBCPlayer victim,
                        CBCPlayer killer,
                        DeathCause cause,
                        boolean direct,
                        int time) {



}
