package net.rpgtoolkit.minecraft.roles;

import java.util.Map;
import net.rpgtoolkit.minecraft.NpcPlugin;
import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityRole;
import net.rpgtoolkit.minecraft.OwnedEntityShop;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import org.yi.acru.bukkit.Lockette.Lockette;

public class ShopkeeperRole extends OwnedEntityRole {

    private OwnedEntityShop shop;
    private Configuration config;

    private enum ConfigurationEntry {

        MESSAGE_WELCOME("npc.shop.msg.welcome"),
        MESSAGE_GOODBYE("npc.shop.msg.goodbye"),
        MESSAGE_PURCHASE("npc.shop.msg.purchase"),
        MESSAGE_OUTOFSTOCK("npc.shop.msg.outofstock"),
        MESSAGE_NOTAFFORDABLE("npc.shop.msg.notaffordable");
        
        private String key;

        ConfigurationEntry(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }

    private final class Configuration {

        private Map<String, String> metadata;

        public Configuration() {
            this.metadata = ShopkeeperRole.this.getMetadata();
        }

        /**
         * Retrieves and parses configuration from book metadata and applies it
         * to the role's metadata for persistence and use within the role.
         *
         * @param book book metadata
         */
        public void apply(BookMeta book) {

            int pages = book.getPageCount();
            int page = 0;

            // Maps each page of the book to a configuration entry
            // in the order the configuration entries are defined. If there
            // is not a page for the configuration entry then the entry
            // is removed so that the default value is used.

            for (ConfigurationEntry entry : ConfigurationEntry.values()) {
                page++;
                String contents = null;
                if (page <= pages) {
                    contents = book.getPage(page);
                }
                this.set(entry, contents);
            }

        }

        private String get(Object key, String defaultValue) {
            String entry = key.toString();
            if (this.metadata.containsKey(entry)) {
                return this.metadata.get(entry);
            } else {
                return defaultValue;
            }
        }

        private void set(Object key, String value) {
            String entry = key.toString();
            if (value != null && value.trim().length() > 0) {
                this.metadata.put(entry, value);
            } else {
                this.metadata.remove(entry);
            }
        }
    }

    public ShopkeeperRole(OwnedEntity owner) {
        super(owner, "Shopkeeper");
        this.config = new Configuration();
    }

    public OwnedEntityShop getShop() {
        if (this.shop == null) {
            this.shop = new OwnedEntityShop(this.entity);
        }
        return this.shop;
    }

    @Override
    protected void onAttack(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player) {

            Player player = (Player) event.getDamager();

            if (this.entity.isOwner(player)) {
                final ItemStack item = player.getItemInHand();

                if (item.getType() == Material.BOOK_AND_QUILL
                        || item.getType() == Material.WRITTEN_BOOK) {
                    BookMeta meta = (BookMeta) player.getItemInHand().getItemMeta();
                    if (meta != null) {
                        this.config.apply(meta);
                        this.entity.say(player, "I will now say the things in that book.");
                    }
                }
            }
        }

    }

    @Override
    protected void onInventoryInteraction(InventoryClickEvent event) {

        final ItemStack item = event.getCurrentItem();
        final Player player = (Player) event.getWhoClicked();

        OwnedEntityShop shop = this.getShop();

        // Remove pricing information if we're not performing
        // a shop transaction with the player, but we're within
        // their configured chest and moving items out of the
        // shop inventory.

        if (!this.interacting(player)) {
            if (item != null && item.getType() != Material.AIR) {
                if (shop != null && shop.isVending(player)) {
                    if (!this.isPlayerItem(event.getInventory(), event.getRawSlot())) {
                        OwnedEntityShop.removePricingInformation(event.getCurrentItem());
                    }
                }
            }
            return;
        }

        // NOTE: At this point we're within a shop transaction.
        // Do not accept any requests that are not an item move request.

        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
            return;
        }

        // If the player is interacting with their own
        // inventory then cancel the request.

        if (this.isPlayerItem(event.getInventory(), event.getRawSlot())) {
            event.setCancelled(true);
            return;
        }

        // If clicked an empty slot or no slot at all then cancel.

        if (item == null || item.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        // Start shop interaction

        if (event.isShiftClick()) {

            // Ensure player has room.

            if (this.isInventoryFull(player.getInventory())) {
                player.sendMessage(
                        ChatColor.RED + "Your inventory is full!");
                event.setCancelled(true);
                return;
            }

            // Purchase item and if successful notify the player
            // and allow player to grab item from the inventory.

            // TODO: Consider returning the "purchase result"
            // in order to supply better messages
            
            if (shop.purchase(player, event.getRawSlot())) {
                this.entity.say(player, this.config.get(
                        ConfigurationEntry.MESSAGE_PURCHASE,
                        "Thank you!"));
                return;
            }
            else {
                this.entity.say(player, ChatColor.RED + this.config.get(
                        ConfigurationEntry.MESSAGE_NOTAFFORDABLE,
                        "You cannot afford that."));
            }

        }

        // Cancel the event because at this stage we've reached a point
        // where either the action is not valid or the shop issued
        // a notification to the player.

        event.setCancelled(true);

    }

    @Override
    protected void onInteraction(PlayerInteractEntityEvent event) {

        event.setCancelled(true);

        final Player player = event.getPlayer();

        // If player is already interacting with the shopkeeper
        // then cancel the request

        if (this.interacting(player)) {
            return;
        }

        // Open the shop if it's configured

        OwnedEntityShop shop = this.getShop();

        if (shop.getInventory() != null && shop.getPricesInventory() != null) {

            // Chest distance check

            Chest stock = shop.getInventoryChest();
            if (stock != null && stock.getLocation().distance(player.getLocation()) > 16) {
                return;
            }

            // Check if there are items to vend or the profit chest is full

            if ((shop.getProfitChest() != null
                    && this.isInventoryFull(shop.getProfitInventory()))
                    || !shop.hasStock()) {
                this.entity.say(player, this.config.get(
                        ConfigurationEntry.MESSAGE_OUTOFSTOCK,
                        "Sorry, I am out of stock!"));
                return;
            }

            // Prepare for vending

            shop.prepare();

            // Start vending

            this.startInteracting(player);

            this.entity.say(player, this.config.get(
                    ConfigurationEntry.MESSAGE_WELCOME,
                    "Welcome! Use shift-click to buy."));

            player.openInventory(shop.getInventory());

        }

    }

    @Override
    protected void onBlockInteraction(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        Block block = event.getClickedBlock();
        OwnedEntityShop shop = this.getShop();

        switch (block.getType()) {
            case CHEST:
                Chest chest = (Chest) event.getClickedBlock().getState();
                if (chest != null) {

                    // Check if chest is protected.

                    if (Lockette.isProtected(block) && !Lockette.isOwner(block, event.getPlayer().getName())) {
                        this.entity.say(player, "That chest is owned by somebody else.");
                        return;
                    }

                    // If the chest is any of the configured chests then unconfigure it.

                    if (chest.equals(shop.getInventoryChest())) {
                        shop.setInventoryChest(null);
                        this.entity.say(player, "I will stop using that chest for my shop inventory.");
                        return;
                    } else if (chest.equals(shop.getPricesInventoryChest())) {
                        shop.setPricesInventoryChest(null);
                        this.entity.say(player, "I will stop using that chest for my shop pricing.");
                        return;
                    } else if (chest.equals(shop.getProfitChest())) {
                        shop.setProfitInventoryChest(null);
                        this.entity.say(player, "I will stop using that chest for my profit.");
                        return;
                    }

                    // Configure one of the unconfigured chests.

                    if (shop.getInventoryChest() == null) {
                        shop.setInventoryChest(chest);
                        this.entity.say(player, "I will use that chest for my shop inventory.");
                    } else if (shop.getPricesInventoryChest() == null) {
                        shop.setPricesInventoryChest(chest);
                        this.entity.say(player, "I will use that chest for my shop pricing.");
                    } else if (shop.getProfitChest() == null) {
                        shop.setProfitInventoryChest(chest);
                        this.entity.say(player, "I will use that chest for my profit.");
                    }

                }
                break;
            default:
                break;
        }


    }

    @Override
    protected void onDeath(EntityDeathEvent event) {

        this.shop = null;

        // Close all open inventories.

        for (final Player player : this.playersInteracting) {
            Bukkit.getScheduler().runTask(NpcPlugin.INSTANCE, new Runnable() {
                @Override
                public void run() {
                    player.closeInventory();
                }
            });
        }

    }

    @Override
    protected void onInventoryClosed(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (this.interacting(player)) {
            this.entity.say(player, this.config.get(
                    ConfigurationEntry.MESSAGE_GOODBYE,
                    "Thanks for shopping!"));
            this.stopInteracting(player);
        }

    }

    @Override
    protected void update() {

        OwnedEntityShop shop = this.getShop();

        shop.setOwner(this.entity);
        shop.setInventoryChest(this.getCurrentChest(
                shop.getInventoryChest()));
        shop.setPricesInventoryChest(this.getCurrentChest(
                shop.getPricesInventoryChest()));
        shop.setProfitInventoryChest(this.getCurrentChest(
                shop.getProfitChest()));

    }

    public boolean isInventoryFull(Inventory inventory) {
        return inventory.firstEmpty() < 0;
    }

    private boolean isPlayerItem(Inventory inventory, int slot) {
        if (inventory instanceof DoubleChestInventory) {
            return (slot > 26 * 2);
        }
        return (slot > 26);
    }

    private Chest getCurrentChest(Chest chest) {
        if (chest != null) {
            Block block = this.entity.getEntity().getWorld()
                    .getBlockAt(chest.getLocation());
            if (block.getType() == Material.CHEST) {
                return (Chest) block.getState();
            } else {
                return null;
            }
        }
        return null;
    }
}
