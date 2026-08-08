package neonique.cbcplugin_new.practice;

import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;
import org.bukkit.entity.Player;

public class PracticePlayer extends CBCPlayer {
    public PracticePlayer(Player player, CombatContext combatContext) {
        super(player, combatContext);
    }
}
