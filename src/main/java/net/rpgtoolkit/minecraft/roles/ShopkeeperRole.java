package net.rpgtoolkit.minecraft.roles;

import net.rpgtoolkit.minecraft.NpcPlugin;
import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityRole;
import net.rpgtoolkit.minecraft.OwnedEntityShop;

import org.bukkit.Bukkit;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import org.yi.acru.bukkit.Lockette.Lockette;

public class ShopkeeperRole extends OwnedEntityRole {

    protected OwnedEntityShop shop;

    public ShopkeeperRole(OwnedEntity owner) {
            super(owner, "Shopkeeper");
    }

    public OwnedEntityShop getShop() {
            if (this.shop == null) {
                    this.shop = new OwnedEntityShop(this.entity);
            }
            return this.shop;
    }

    @Override
    protected void onAttack(EntityDamageByEntityEvent event) {


    }

    @Override
    protected void onInventoryInteraction(InventoryClickEvent event) {

        final Inventory inventory = event.getInventory();
        final InventoryHolder holder = inventory.getHolder();
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

            // Purchase item and if successful notify the player
            // and allow player to grab item from the inventory.

            if (shop.purchase(player, event.getRawSlot())) {
                this.entity.say(player, "Thank you!");
                return;
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

        if (this.interacting(player)) {
            return;
        }
        
        // Open the shop if it's configured
        
        OwnedEntityShop shop = this.getShop();
        
        if (shop.getInventory() != null && shop.getPricesInventory() != null) {
            
            // Chest distance check
        
            Chest stock = shop.getInventoryChest();
            if (stock != null && stock.getLocation().distance(player.getLocation()) > 16) {
                this.entity.say(player, "My shop is too far away.");
                return;
            }
            
            // Check if there are items to vend or the profit chest is full
            
            if ((shop.getProfitChest() != null && shop.isFull(shop.getProfitInventory(), null)) || !shop.hasStock()) {
                this.entity.say(player, "Sorry, I am out of stock.");
                return;
            }
            
            // Prepare for vending
            
            shop.prepare();
            
            // Start vending
            
            this.startInteracting(player);
            this.entity.say(player, "Welcome! Use shift-click to buy.");

            player.openInventory(shop.getInventory());  
        }
        else {
            this.entity.say(event.getPlayer(), 
                            "Sorry, I don't have a shop setup yet.");
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

                        if (Lockette.isProtected(block) && !Lockette.isOwner(block, event.getPlayer().getName())) {
                            this.entity.say(player, "That chest is owned by somebody else.");
                            return;
                        }

                        if (chest.equals(shop.getInventoryChest())) {
                            shop.setInventoryChest(null);
                            this.entity.say(player, "I will stop using that chest for my shop inventory.");
                            return;
                        }
                        else if (chest.equals(shop.getPricesInventoryChest())) {
                            shop.setPricesInventoryChest(null);
                            this.entity.say(player, "I will stop using that chest for my shop pricing.");
                            return;
                        }
                        else if (chest.equals(shop.getProfitChest())) {
                            shop.setProfitInventoryChest(null);
                            this.entity.say(player, "I will stop using that chest for my profit.");
                            return;
                        }

                        if (shop.getInventoryChest() == null) {
                            shop.setInventoryChest(chest);
                            this.entity.say(player, "I will use that chest for my shop inventory.");
                        }
                        else if (shop.getPricesInventoryChest() == null) {
                            shop.setPricesInventoryChest(chest);
                            this.entity.say(player, "I will use that chest for my shop pricing.");
                        }
                        else if (shop.getProfitChest() == null) {
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
                public void run()  {
                    player.closeInventory();
                }

            });
        }
            
    }

    @Override
    protected void onInventoryClosed(InventoryCloseEvent event) {

        Player player = (Player) event.getPlayer();

        if (this.interacting(player)) {
                this.shop.unprepare();
                this.entity.say(player, "Thanks for shopping!");
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
                }
                else {
                        return null;
                }
        }
        return null;
    }

}
