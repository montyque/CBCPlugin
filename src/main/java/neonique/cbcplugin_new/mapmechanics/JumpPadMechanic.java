package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
/*
public class JumpPadMechanic implements MapMechanic {

    private final Collection<JumpPad> jumpPads;

    private CombatContext combatContext;

    private final Set<CBCPlayer> playersOnPad = new HashSet<>();

    public JumpPadMechanic (Collection<JumpPad> jumpPads) {
        this.jumpPads = jumpPads;
    }

    @Override
    public void activate (CombatContext combatContext) {
        this.combatContext = combatContext;
        playersOnPad.clear();

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 1);
    }

    @Override
    public void deactivate() {
        playersOnPad.clear();
        updateTask.cancel();
    }

    public void update () {

        Set<CBCPlayer> newOnPad = new HashSet<>();

        // Find all players who are on the pad
        for (JumpPad jumpPad : jumpPads) {
            jumpPad.playParticles();
            newOnPad.addAll(jumpPad.getPlayersOnPad(registry));
        }

        // Restore old jump boost effect if player is no longer on pad
        for (CBCPlayer player : playersOnPad) {
            if (!newOnPad.contains(player)) {
                playerOffPad(player);
            }
        }

        // Give players on jump pads the increased jump effect
        for (CBCPlayer player : newOnPad) {
            playerOnPad(player);
        }

        // Update the current list of players on jump pads
        playersOnPad.clear();
        playersOnPad.addAll(newOnPad);

    }

    private void playerOnPad (CBCPlayer player) {
        if (!player.isOnline()) return;
        Player entity = player.getPlayer();
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 11, false, false, true));
    }

    private void playerOffPad (CBCPlayer player) {
        if (!player.isOnline()) return;
        Player entity = player.getPlayer();
        entity.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.giveEffects();
    }

}
*/