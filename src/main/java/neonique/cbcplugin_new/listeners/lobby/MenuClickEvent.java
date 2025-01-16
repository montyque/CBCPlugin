package neonique.cbcplugin_new.listeners.lobby;

import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gameobjects.GamemodeOptions;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MenuClickEvent implements Listener {

    private final Lobby lobby;
    private final GameManager gameManager;

    public MenuClickEvent (Lobby lobby, GameManager gameManager) {
        this.lobby = lobby;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {

        Player playerClicked = (Player) e.getWhoClicked();

        if (!(e.getView().title() instanceof TextComponent)) {
            return;
        }

        TextComponent text = (TextComponent) e.getView().title();

        if (text.content().equals("Post Game Statistics")) {
            e.setCancelled(true);
            return;
        }

        // Gamemode selection gui
        if (text.content().equals("Select Gamemode")) {

            e.setCancelled(true);
            if (checkIfNotValidItemClicked(e)) {
                return;
            }

            ItemStack clickedItem = e.getCurrentItem();

            assert clickedItem != null;
            if (clickedItem.getType() != Material.LIGHT_GRAY_DYE) return;

            ItemMeta itemMeta = clickedItem.getItemMeta();
            if (!itemMeta.hasCustomModelData()) return;

            int customModelData = itemMeta.getCustomModelData();
            if (!gameManager.getGamemodeToIntegerList().containsKey(customModelData)) return;

            CBCGamemode gamemode = gameManager.getGamemodeToIntegerList().get(customModelData);

            lobby.openMapMenu(playerClicked, gamemode, gameManager.getGamemodes().get(gamemode));

        } else if (text.content().startsWith("Select Map")) {

            // Map selection gui
            e.setCancelled(true);
            if (checkIfNotValidItemClicked(e)) {
                return;
            }

            // Find gamemode
            LinkedHashMap<CBCGamemode, GamemodeOptions> gamemodeList = gameManager.getGamemodes();
            CBCGamemode gamemodeSelected = null;
            for (CBCGamemode gamemode : gamemodeList.keySet()) {
                GamemodeOptions gamemodeVariables = gamemodeList.get(gamemode);
                if (text.content().endsWith(gamemodeVariables.getGamemodeName())) {
                    gamemodeSelected = gamemode;
                    break;
                }
            }

            if (gamemodeSelected == null) {
                System.out.println("In Select Map GUI, could not find gamemode from title");
                return;
            }

            // Find map
            List<CBCMap> mapList = gameManager.getGamemodeAndMapList().get(gamemodeSelected);

            if (mapList == null) {
                System.out.println("In Select Map GUI, could not find map from gamemode " + gamemodeSelected.name());
                return;
            }

            // Iterate through all maps in map list to find map selected
            CBCMap selectedMap = null;
            for (CBCMap map : mapList) {
                if (map.getBlockSymbol() == e.getCurrentItem().getType()) {
                    selectedMap = map;
                    break;
                }
            }

            // If selected map found
            if (selectedMap == null) {
                System.out.println("In Select Map GUI, could not find map to material");
                return;
            }

            e.getView().close(); // Close the inventory
            lobby.setGamemodeAndMapSelected(gamemodeSelected, selectedMap);

        } else if (text.content().startsWith("Randomize Teams")) {

            // Team randomizer gui
            e.setCancelled(true);
            if (checkIfNotValidItemClicked(e)) {
                return;
            }

            Inventory inventory = e.getClickedInventory();
            if (inventory != e.getView().getTopInventory()) {
                return;
            }

            // Make a hashmap of all team objects with the keys being their respective blocks
            Collection<LobbyTeam> teamSet = lobby.getTeamsSet();
            HashMap<Integer, LobbyTeam> numberToTeam = new HashMap<>();
            for (LobbyTeam team : teamSet) {
                numberToTeam.put(team.getColorNumber() + 1, team);
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

    public boolean checkIfNotValidItemClicked (InventoryClickEvent e) {
        if (e.getCurrentItem() == null) {
            return true;
        }

        e.setCancelled(true);
        if (e.getCurrentItem() == null) {
            System.out.println("In Select Map GUI, current item not found");
            return true;
        }

        if (e.getCurrentItem().getItemMeta().displayName() == null) {
            System.out.println("In Select Map GUI, current item with display name not found");
            return true;
        }

        if (!Objects.requireNonNull(e.getCurrentItem().getItemMeta().displayName()).hasDecoration(TextDecoration.BOLD)) {
            System.out.println("In Select Map GUI, current item with bolded display name not found");
            return true;
        }

        return false;
    }
}