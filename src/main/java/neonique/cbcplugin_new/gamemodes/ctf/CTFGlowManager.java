package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.gamemodes._base.GlowManager;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CTFGlowManager extends GlowManager {

    private CTFGame game;
    private final HashMap<Player, Set<Player>> glowingPlayers;

    public CTFGlowManager(World world, CTFGame game) {
        super(world);
        this.game = game;

        glowingPlayers = new HashMap<>();
    }

    public void updateGlowingList (Player client, Set<Player> players) {

        Set<Player> addedPlayers = new HashSet<>();

        Set<Player> oldList;
        if (glowingPlayers.containsKey(client)) {
            oldList = glowingPlayers.get(client);
        } else {
            glowingPlayers.put(client, new HashSet<>());
            oldList = new HashSet<>();
        }

        // Go through all players in new list
        Set<Player> removedPlayers = new HashSet<>(oldList);
        for (Player player : players) {
            if (!oldList.contains(player)) {
                addedPlayers.add(player);
            } else {
                removedPlayers.remove(player);
            }
        }

        for (Player player : addedPlayers) {
            glowingPlayers.get(client).add(player);
            playerImmediateGlow(client, player, true);
        }

        for (Player player : removedPlayers) {
            glowingPlayers.get(client).remove(player);
            playerImmediateGlow(client, player, false);
        }
    }


    @Override
    public boolean isGlowing(Player client, Player player) {
        if (glowingPlayers.containsKey(client)) {
            Set<Player> glowingPlayerSet = glowingPlayers.get(client);
            return glowingPlayerSet.contains(player);
        }
        return false;
    }
}
