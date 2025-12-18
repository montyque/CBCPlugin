package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlagRushPlayer extends CTFPlayer {

    public FlagRushPlayer(CTFGame game, GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        super(game, gameManager, combatManager, player, playerId);
    }

    @Override
    public void playerCaptureFlag () {

        if (!isOnline()) return;

        clearPlayerListSuffixes();
        getGameManager().playSound(getPlayer().getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 17, 1);

        // Display firework
        Component flagCaptureComponent = Component.newline().append(Component.text("FLAG CAPTURED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(getNameComponent())
                .append(Component.text(" has captured the ").color(NamedTextColor.WHITE))
                .append(Component.text("⚑ " + teamWithFlagPickedUp.getTeamName() + " Flag").color(teamWithFlagPickedUp.getColor()).decorate(TextDecoration.BOLD))
                .append(Component.text("!").color(NamedTextColor.WHITE));

        // Send message
        getGameManager().sendGlobalMessage(
                flagCaptureComponent.append(Component.newline())
        );

        FlagRushTeam frTeam = (FlagRushTeam) getTeam();
        frTeam.flagCapturedByTeam(teamWithFlagPickedUp);

        // Increment stats
        flagsCaptured++;

        // Reset player's health and effects
        getPlayer().setHealth(20);
        for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
        }

        // Set player helmet back to normal
        getInventory().setHelmetOverride(null);

    }
}
