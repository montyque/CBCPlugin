package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerItemListener implements Listener {

    private final PlayerStore players;

    private final Set<Material> DISALLOWED_ITEM_DROPS = new HashSet<>();

    public PlayerItemListener (PlayerStore players) {

        this.players = players;

        // All items below are disallowed to be dropped
        DISALLOWED_ITEM_DROPS.add(Material.CROSSBOW);
        DISALLOWED_ITEM_DROPS.add(Material.NETHERITE_CHESTPLATE);
        DISALLOWED_ITEM_DROPS.add(Material.RED_STAINED_GLASS_PANE);
        DISALLOWED_ITEM_DROPS.add(Material.BLUE_STAINED_GLASS_PANE);
        DISALLOWED_ITEM_DROPS.add(Material.LIME_STAINED_GLASS_PANE);
        DISALLOWED_ITEM_DROPS.add(Material.YELLOW_STAINED_GLASS_PANE);
        DISALLOWED_ITEM_DROPS.add(Material.COMPASS);
    }

    @EventHandler
    public void changeHand(PlayerItemHeldEvent e) {

        Player playerEntity = e.getPlayer();
        CBCPlayer player = players.getPlayer(playerEntity);
        if (player == null) return;
        player.updateActionBarDisplay(true);

    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {

        Player playerEntity = e.getPlayer();
        CBCPlayer player = players.getPlayer(playerEntity);
        if (player == null) return;

        if (DISALLOWED_ITEM_DROPS.contains(e.getItemDrop().getItemStack().getType())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void pickupItem(EntityPickupItemEvent e) {

        // Verify that the entity is a player
        Entity entity = e.getEntity();
        if (!(entity instanceof Player playerEntity)) {
            return;
        }

        CBCPlayer player = players.getPlayer(playerEntity);
        if (player == null) return;

        if (DISALLOWED_ITEM_DROPS.contains(e.getItem().getItemStack().getType())) {
            e.setCancelled(true);
        }

    }

    @EventHandler
    public void swapItem(PlayerSwapHandItemsEvent e) {

        CBCPlayer player = players.getPlayer(e.getPlayer());
        if (player == null) return;

        if (DISALLOWED_ITEM_DROPS.contains(e.getMainHandItem().getType())) {
            // Cancel event
            e.setCancelled(true);
            return;
        }

        if (DISALLOWED_ITEM_DROPS.contains(e.getOffHandItem().getType())) {
            // Cancel event
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if(e.getWhoClicked() instanceof Player && e.getClickedInventory() != null) {
            List<ItemStack> items = new ArrayList<>();
            items.add(e.getCurrentItem());
            items.add(e.getCursor());
            items.add((e.getClick() == org.bukkit.event.inventory.ClickType.NUMBER_KEY) ? e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) : e.getCurrentItem());
            for(ItemStack item : items) {
                if(item != null && item.hasItemMeta()) {
                    if(DISALLOWED_ITEM_DROPS.contains(item.getType())) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
}