package neonique.cbcplugin_new.gameobjects;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class JumpPad extends Location {

    GameManager gameManager;
    CombatManager combatManager;

    public JumpPad(GameManager gameManager, CombatManager combatManager, Vector coordinates) {
        super(gameManager.getWorld(), coordinates.getX(), coordinates.getY(), coordinates.getZ());
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    public void jumpPadPressed (CBCPlayer player) {

        player.jumpPadPressed();

    }

}
