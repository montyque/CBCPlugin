package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.gamemodes._base.GlowManager;
import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AssassinGlowManager extends GlowManager {

    private final AssassinGame game;

    private final HashMap<Player, Player> glowingPlayers;

    public AssassinGlowManager (World world, AssassinGame game) {
        super(world);
        this.game = game;
        glowingPlayers = new HashMap<>();
    }

    public void updateGlowingPlayer (Player client, Player newPlayer) {

        Player oldPlayer = glowingPlayers.getOrDefault(client, null);
        // Check if added player is not the same as old player
        if (oldPlayer != newPlayer) {
            if (oldPlayer != null) {
                playerImmediateGlow(client, oldPlayer, false);
            }
            if (newPlayer != null) {
                playerImmediateGlow(client, newPlayer, true);
            }
        }

        glowingPlayers.put(client, newPlayer);
    }

    @Override
    public boolean isGlowing(Player client, Player player) {
        if (player == null) return false;
        return glowingPlayers.getOrDefault(client, null) == player;
    }
}
