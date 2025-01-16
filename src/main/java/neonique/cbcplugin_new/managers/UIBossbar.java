package neonique.cbcplugin_new.managers;

import net.kyori.adventure.bossbar.BossBar;

import java.util.HashMap;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class UIBossbar {

    private Component serverText;
    private HashMap<UUID, Component> clientText;

    public UIBossbar () {
        // Set server text and client text
        serverText = Component.text("");
        clientText = new HashMap<>();
    }

    public void clearAllClientText () {
        clientText.clear();
    }

    public void setClientText(Player player, Component component) {
        if (component != null) {
            clientText.put(player.getUniqueId(), component);
        }
        else {
            clientText.remove(player.getUniqueId());
        }
    }

    public void setServerText(Component component) {
        serverText = component;
    }

    public Component getText(UUID playerUUID) {

        if (clientText.containsKey(playerUUID)) {
            return clientText.getOrDefault(playerUUID, serverText);
        }
        else {
            return serverText;
        }

    }

}
