package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.resourcepack.PlayerHeadType;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GameBossBarManager {

    private final ResourcePackManager resourcePackManager;

    private final HashMap<UUID, List<BossBar>> bossbars;

    private final List<UIBossbar> uiBossbars;

    private final int rows;

    public GameBossBarManager (int rows) {

        resourcePackManager = CBCPlugin.getResourcePackManager();
        bossbars = new HashMap<>();
        uiBossbars = new ArrayList<>();
        this.rows = rows;

        // Create boss bars
        for (int row = 0; row < rows; row++) {
            // UIBossbar uiBossbar = BossBar.bossBar(Component.text(""), 0, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            UIBossbar uiBossbar = new UIBossbar();
            uiBossbars.add(uiBossbar);
        }
    }

    public void setAllBossbarsText (Component t0, Component t1, Component t2, Component t3, Component t4, Component t5, Component t6) {

        if (t0 != null) {setServerText(0, t0);} else {setServerText(0, Component.text(""));}
        if (t1 != null) {setServerText(1, t1);} else {setServerText(1, Component.text(""));}
        if (t2 != null) {setServerText(2, t2);} else {setServerText(2, Component.text(""));}
        if (t3 != null) {setServerText(3, t3);} else {setServerText(3, Component.text(""));}
        if (t4 != null) {setServerText(4, t4);} else {setServerText(4, Component.text(""));}
        if (t5 != null) {setServerText(5, t5);} else {setServerText(5, Component.text(""));}
        if (t6 != null) {setServerText(6, t6);} else {setServerText(6, Component.text(""));}

    }

    public void setServerText (int row, Component text) {

        // Check if bossbar manager has enough rows
        if (uiBossbars.size() - 1 < row) {
            // Not enough rows
            return;
        }

        // Get bossbar
        uiBossbars.get(row).setServerText(text);

    }

    public void setClientText (Player player, int row, Component text) {

        // Check if bossbar manager has enough rows
        if (uiBossbars.size() - 1 < row) {
            // Not enough rows
            return;
        }

        // Get bossbar
        uiBossbars.get(row).setClientText(player, text);

    }

    public void update () {}

    public void updateClientBars () {
        for (UUID playerUUID : bossbars.keySet()) {
            // Get bossbar list
            List<BossBar> bossBarList = bossbars.get(playerUUID);
            int i = 0;
            for (BossBar bossBar : bossBarList) {
                if (i < uiBossbars.size()) {
                    UIBossbar uiBossbar = uiBossbars.get(i);
                    bossBar.name(uiBossbar.getText(playerUUID));
                }
                i++;
            }
        }
    }

    public void addPlayer (Player player) {
        UUID playerUUID = player.getUniqueId();
        if (!bossbars.containsKey(playerUUID)) {
            // Create bossbars
            bossbars.put(playerUUID, new ArrayList<>());
            for (int row = 0; row < rows; row++) {
                BossBar bossBar = BossBar.bossBar(Component.text(""), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
                bossbars.get(playerUUID).add(bossBar);
            }
        }

        // Bossbar list
        List<BossBar> bars = bossbars.get(playerUUID);

        for (BossBar bossbar : bars) {
            // Show boss bar to player
            player.showBossBar(bossbar);
        }
    }

    public void hidePlayerBossbars (Player player) {
        UUID playerUUID = player.getUniqueId();
        if (bossbars.containsKey(playerUUID)) {
            for (BossBar bossBar : bossbars.get(playerUUID)) {
                player.hideBossBar(bossBar);
            }
        }
    }

    public Component getPlayerHeadDisplay (CBCPlayer player, double health, boolean alive, TextColor barColor) {

        OfflinePlayer offlinePlayer = player.getOfflinePlayer();

        Component headCharacter;
        if (alive) {
            headCharacter = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.DOWN_24_NORMAL, offlinePlayer);
        }
        else {
            headCharacter = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.DOWN_24_TRANSPARENT, offlinePlayer);
        }

        // Get health bar character depending on health
        int healthInt = (int) Math.ceil(health * 8);

        if (healthInt > 8) {
            healthInt = 8;
        }

        if (healthInt < 0) {
            healthInt = 0;
        }

        if (!alive) {
            healthInt = 0;
        }

        int healthCharacterInt = 58127 - healthInt;
        String healthCharacter = Character.toString((char) healthCharacterInt);

        // Get bar character
        String barCharacter = Character.toString((char) 58112);

        // Create component
        Component headHealthComponent = headCharacter.append(Component.text( "\uF808\uF801" + healthCharacter));

        if (barColor != null) {
            return headHealthComponent.append(Component.text("\uF808\uF801" + barCharacter).color(barColor));
        }
        else {
            return headHealthComponent;
        }
    }

    public void setAllBossbarsText (Component t0) {
        setAllBossbarsText(t0, null, null, null, null, null, null);
    }
    public void setAllBossbarsText (Component t0, Component t1) {
        setAllBossbarsText(t0, t1, null, null, null, null, null);
    }
    public void setAllBossbarsText (Component t0, Component t1, Component t2) {
        setAllBossbarsText(t0, t1, t2, null, null, null, null);
    }
    public void setAllBossbarsText (Component t0, Component t1, Component t2, Component t3) {
        setAllBossbarsText(t0, t1, t2, t3, null, null, null);
    }
    public void setAllBossbarsText (Component t0, Component t1, Component t2, Component t3, Component t4) {
        setAllBossbarsText(t0, t1, t2, t3, t4, null, null);
    }
    public void setAllBossbarsText (Component t0, Component t1, Component t2, Component t3, Component t4, Component t5) {
        setAllBossbarsText(t0, t1, t2, t3, t4, t5, null);
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
}
