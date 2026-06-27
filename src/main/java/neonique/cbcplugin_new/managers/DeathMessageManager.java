package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.getDeathCauseIcon;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class DeathMessageManager {

    // List of possible death messages
    private HashMap<DeathCause, DeathMessageGenerator> defaultDeathMessages;

    // If a map overrides certain death messages, use these instead
    private HashMap<DeathCause, DeathMessageGenerator> overrideDeathMessages;

    public DeathMessageManager () {
        defaultDeathMessages = new HashMap<>();
        overrideDeathMessages = new HashMap<>();
    }

    public boolean loadDeathMessages () {

        final String deathMessagesFileName = "deathmessages.yml";
        defaultDeathMessages = new HashMap<>();

        // Attempt to find weapons folder
        File weaponsFolderFile = new File(CBCPlugin.getPlugin().getDataFolder(), "weapons");

        // Attempt to make this a directory
        if (!weaponsFolderFile.exists()) {
            boolean folderMade = weaponsFolderFile.mkdir();
            if (!folderMade) {
                return false;
            }
        }

        // Get file that has all death messages
        File file = new File(weaponsFolderFile, deathMessagesFileName);
        if (!file.exists()) return false;

        YamlConfiguration deathMessagesFile = YamlConfiguration.loadConfiguration(file);

        defaultDeathMessages = DeathMessageGenerator.loadDeathMessageGenerators(deathMessagesFile);

        return true;

    }


    public Component getDeathMessage(CBCPlayer playerKilled, CBCPlayer playerKiller, DeathCause cause, boolean direct) {

        // Retrieve death message via cause and circumstances of kill
        DeathMessageGenerator dmGenerator;
        if (overrideDeathMessages.containsKey(cause)) {
            dmGenerator = overrideDeathMessages.get(cause);
        }
        else {
            dmGenerator = defaultDeathMessages.get(cause);
        }

        if (dmGenerator == null) return null;
        Component deathMessage = dmGenerator.getDeathMessageComponent(playerKilled, playerKiller, direct);

        // Add multi kill counter
        if (playerKiller != null) {
            playerKiller.updateMultiKill();
            String multiText = "";
            if (playerKiller.getMultiKill() + 1 == 2) {
                multiText = " DOUBLE KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 3) {
                multiText = " TRIPLE KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 4) {
                multiText = " QUADRA KILL!";
            } else if (playerKiller.getMultiKill() + 1 == 5) {
                multiText = " PENTAKILL!";
            }
            deathMessage = deathMessage.append(smallText(multiText).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        }

        NamedTextColor color = NamedTextColor.WHITE;
        if (playerKilled.team() != null) {
            color = playerKilled.team().textColor();
        }

        Component deathIcon = getDeathCauseIcon(cause, playerKiller != null, color).append(Component.space());

        return deathIcon.append(deathMessage);
    }

    public Component getKillStreakMessage(CBCPlayer playerKiller) {

        Component playerComponent = playerKiller.nameComponent().decorate(TextDecoration.BOLD);

        if (playerKiller.getKillStreak() == 5) {
            return playerComponent.append(Component.text(" is on a killing spree!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 10) {
            return playerComponent.append(Component.text(" is unstoppable!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 15) {
            return playerComponent.append(Component.text(" is dominating!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 20) {
            return playerComponent.append(Component.text(" is legendary!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);
        }
        return null;
    }

    public Component getKillStreakEndedMessage(CBCPlayer playerKilled, CBCPlayer playerKiller) {

        int killStreak = playerKilled.getKillStreak();

        Component playerKilledComponent = playerKilled.nameComponent();

        if (playerKiller == null) {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has ended!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        } else {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has been ended by ").color(NamedTextColor.WHITE))
                    .append(playerKiller.nameComponent())
                    .append(Component.text("!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        }
    }

    public void setOverrides (HashMap<DeathCause, DeathMessageGenerator> overrides) {
        overrideDeathMessages = overrides;
    }
}
