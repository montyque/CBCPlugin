package neonique.cbcplugin_new.lobby.listeners;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MenuClickEvent implements Listener {

    public static final NamespacedKey MENU_ITEM_ID_KEY = new NamespacedKey(CBCPlugin.getPlugin(), "menu-item-id");

    private final Lobby lobby;
    private final GameManager gameManager;

    public MenuClickEvent (Lobby lobby, GameManager gameManager) {
        this.lobby = lobby;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {

        Player playerClicked = (Player) e.getWhoClicked();

        if (!(e.getView().title() instanceof TextComponent text)) {
            return;
        }

        if (text.content().equals("Post Game Statistics")) {
            e.setCancelled(true);
            return;
        }

        // Gamemode selection gui
        if (text.content().equals("Select Gamemode")) {

            e.setCancelled(true);
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;

            // Get the item menu id
            String itemId = clickedItem.getItemMeta().getPersistentDataContainer().get(
                    MENU_ITEM_ID_KEY,
                    PersistentDataType.STRING
            );
            if (itemId == null) return;

            // Find the gamemode clicked on
            try {
                CBCGamemode gamemode = CBCGamemode.valueOf(itemId);
                lobby.openMapMenu(playerClicked, gamemode);
            } catch (IllegalArgumentException ignored) {}

        } else if (text.content().startsWith("Select Map")) {

            e.setCancelled(true);
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;

            // Use title of UI to figure out which gamemode the player selected
            CBCGamemode gamemodeSelected = null;
            for (CBCGamemode gamemode : gameManager.getLoadedGamemodes()) {
                if (text.content().endsWith(gamemode.getGamemodeName())) {
                    gamemodeSelected = gamemode;
                    break;
                }
            }

            if (gamemodeSelected == null) {
                return;
            }

            // Retrieve map from map id
            String itemId = clickedItem.getItemMeta().getPersistentDataContainer().get(
                    MENU_ITEM_ID_KEY,
                    PersistentDataType.STRING
            );
            if (itemId == null) return;

            Optional<GamemodeMapData> mapSelected = gameManager.getGamemodeMapWithId(gamemodeSelected, itemId);
            if (mapSelected.isEmpty()) return;
            lobby.setGamemodeAndMapSelected(gamemodeSelected, mapSelected.get());

        } else if (text.content().startsWith("Randomize Teams")) {

            // Team randomizer gui
            e.setCancelled(true);
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;

            Inventory inventory = e.getClickedInventory();
            if (inventory != e.getView().getTopInventory()) {
                return;
            }

            // Make a hashmap of all team objects with the keys being their respective blocks
            Collection<LobbyTeam> teamSet = lobby.getTeamsSet();
            HashMap<Integer, LobbyTeam> numberToTeam = new HashMap<>();
            for (LobbyTeam team : teamSet) {
                numberToTeam.put(team.textColor().value() + 1, team);
            }

            Set<LobbyTeam> teamsSelected = new HashSet<>();
            // Find the teams currently selected
            for (ItemStack item : inventory.getContents()) {
                if (item == null) continue;
                // Check if item has enchantment which marks it as selected
                if (item.containsEnchantment(Enchantment.THORNS)) {
                    // Check if item has custom model data
                    if (item.getItemMeta().hasCustomModelData()) {
                        if (numberToTeam.containsKey(item.getItemMeta().getCustomModelData())) {
                            teamsSelected.add(numberToTeam.get(item.getItemMeta().getCustomModelData()));
                        }
                    }
                }
            }

            // Check if item clicked's slot is between 0 and 7 - the team objects only appear from these slots
            assert e.getCurrentItem() != null;
            if (e.getSlot() >= 0 && e.getSlot() <= 7) {
                int slot = e.getSlot();
                // Toggle that item
                e.getCurrentItem();

                // Create item
                ItemStack item = e.getCurrentItem();

                int teamsSelectedNum = teamsSelected.size();

                assert item != null;
                if (item.containsEnchantment(Enchantment.THORNS)) {
                    // Set randomising to false
                    item.removeEnchantment(Enchantment.THORNS);
                    ItemMeta itemMeta = item.getItemMeta();
                    Component itemLore;
                    itemLore = Component.text("Click to add to randomisation").color(NamedTextColor.DARK_RED)
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    List<Component> loreList = new ArrayList<>();
                    loreList.add(itemLore);

                    int newCustomModelData = (itemMeta.getCustomModelData() % 8) + 8;
                    if (itemMeta.getCustomModelData() == 8) {
                        newCustomModelData = 16;
                    }
                    itemMeta.setCustomModelData(newCustomModelData);


                    itemMeta.lore(loreList);
                    item.setItemMeta(itemMeta);
                    teamsSelectedNum--;
                } else {
                    // Set randomising to false
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.addEnchant(Enchantment.THORNS, 1, false);
                    Component itemLore;
                    itemLore = Component.text("Click to remove from randomisation").color(NamedTextColor.DARK_GREEN)
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    List<Component> loreList = new ArrayList<>();
                    loreList.add(itemLore);
                    itemMeta.lore(loreList);

                    int newCustomModelData = (itemMeta.getCustomModelData() % 8);
                    if (itemMeta.getCustomModelData() == 16) {
                        newCustomModelData = 8;
                    }

                    itemMeta.setCustomModelData(newCustomModelData);
                    item.setItemMeta(itemMeta);
                    teamsSelectedNum++;
                }
                inventory.setItem(slot, item);



                // Check that there are enough/too many teams in randomisation
                ItemStack goItem;

                // Check if there is a max
                int max = 8;
                if (lobby.getMapSelected() != null) {
                    if (lobby.getMapSelected().getMaxTeams() != null) {
                        max = lobby.getMapSelected().getMaxTeams();
                    }
                }

                if (teamsSelectedNum < 2) {
                    // Create magenta glazed terracotta to press when randomising
                    goItem = new ItemStack(Material.RED_GLAZED_TERRACOTTA);
                    ItemMeta itemMeta = goItem.getItemMeta();
                    // Set item title
                    Component itemTitle = Component.text("You need to select at least 2 teams!").color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    itemMeta.displayName(itemTitle);
                    goItem.setItemMeta(itemMeta);
                } else if (teamsSelectedNum > max) {
                    // Create magenta glazed terracotta to press when randomising
                    goItem = new ItemStack(Material.RED_GLAZED_TERRACOTTA);
                    ItemMeta itemMeta = goItem.getItemMeta();
                    // Set item title
                    Component itemTitle = Component.text("You can only select up to " + max + " teams!").color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    itemMeta.displayName(itemTitle);
                    goItem.setItemMeta(itemMeta);
                } else {
                    // Create magenta glazed terracotta to press when randomising
                    goItem = new ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA);
                    ItemMeta itemMeta = goItem.getItemMeta();
                    // Set item title
                    Component itemTitle = Component.text("Randomise players into " + teamsSelectedNum + " teams").color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
                    itemMeta.displayName(itemTitle);
                    goItem.setItemMeta(itemMeta);
                }
                inventory.setItem(26, goItem);

                playerClicked.openInventory(inventory);
            } else if (e.getCurrentItem().getType() == Material.MAGENTA_GLAZED_TERRACOTTA && e.getSlot() == 26) {
                // Player wants to randomise teams
                inventory.close();
                lobby.randomizeTeams(teamsSelected);
            }
        }
    }

}