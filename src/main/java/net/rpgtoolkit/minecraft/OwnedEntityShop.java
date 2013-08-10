package net.rpgtoolkit.minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rpgtoolkit.minecraft.items.Items;
import net.rpgtoolkit.minecraft.util.Pluralize;

import org.apache.commons.lang.StringUtils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OwnedEntityShop {

	private OwnedEntity owner;
	private Chest stockChest;
	private Chest pricesChest;
        private Chest profitChest;
        private boolean isInventoryFinite;

        private static final class ItemShopInformation {
            
            private ItemStack item;
            private ItemStack[] prices;
            
            public ItemShopInformation(ItemStack item) {
                this.item = item.clone();
            }
            
            public void prices(ItemStack... prices) {
                this.prices = prices;
            }
            
            public ItemStack[] prices() {
                return this.prices;
            }
            
            public boolean purchasable() {
                return (this.prices != null && this.prices.length > 0);
            }
            
            public List<String> getPurchaseText() {
                
                List<String> lines = new ArrayList<>();
                
                if (this.prices != null && this.prices.length > 0) {
                    for (int i = 0; i < prices.length; i++) {
                        int amount = prices[i].getAmount();     
                        String name = Items.itemByStack(prices[i]).getName();
                        String text = String.format(
                              ChatColor.GREEN + "%s %s", amount, 
                                Pluralize.apply(name, amount));
                        if (i == 0) {
                            text = ChatColor.GRAY + "Trade for " + text;
                        }
                        if (i < prices.length - 1) {
                            text += ChatColor.GRAY + " or";
                        }
                        lines.add(text);
                    }
                }
                else {
                    lines.add(ChatColor.RED + "Not for sale");
                }
                
                return lines;
                                
            }
                        
        }
        
	public OwnedEntityShop(OwnedEntity owner) {
            
		this.owner = owner;
                this.isInventoryFinite = true;
                
		OwnedEntityRole role = this.owner.getRole();
		
		if (role.getMetadata().size() > 0) {
                    this.setInventoryChest(this.getChestFromMetadata("shop.stock"));
                    this.setPricesInventoryChest(this.getChestFromMetadata("shop.prices"));
                    this.setProfitInventoryChest(this.getChestFromMetadata("shop.profit"));
                    
                    final Map<String, String> metadata = this.owner.getRole().getMetadata();
                    if (metadata.containsKey("shop.finite")) {
                        this.setHasFiniteInventory(Boolean.parseBoolean(
                                metadata.get("shop.finite")));
                    }
		}
                
	}
        
        /**
         * Prepares the shop for vending items to a player.
         */
        public void prepare() {
                        
            // Set item price metadata

            for (ItemStack item : this.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    
                    ItemShopInformation info = this.getItemInformation(item);
                    
                    if (!item.hasItemMeta()) {
                        item.setItemMeta(Bukkit.getItemFactory().getItemMeta(item.getType()));
                    }
                    
                    ItemMeta meta = item.getItemMeta();
                    
                    meta.setLore(
                            info.getPurchaseText()
                    );
                    
                    item.setItemMeta(meta);
                    
                }
            }
        }
        
	private ItemShopInformation getItemInformation(final ItemStack item) {
            
            List<ItemStack> prices = new ArrayList<>();
            
            if (this.pricesChest == null) {
                    return null;
            }
            else {

                Inventory inventory = this.getPricesInventory();
                
                int rows = 1;
                if (inventory.getHolder() instanceof DoubleChest) {
                    rows = 2;
                }
                
                for (int row = 0; row < rows; row++) {
                    for (int i = 0; i < 9; i++) {
                        int slot = (i + (row * 27));
                        ItemStack price = inventory.getItem(slot);
                        if (price != null && price.getType() != Material.AIR) {
                            if (price.getType() == item.getType() && price.getAmount() == item.getAmount()) {
                                ItemStack cost = inventory.getItem(slot + 9);
                                if (cost != null && cost.getType() != Material.AIR) {
                                    prices.add(cost.clone());
                                }
                            }
                        }
                    }
                }

            }
            
            ItemShopInformation info = new ItemShopInformation(item);
            
            info.prices(prices.toArray(new ItemStack[0]));
            
            return info;
                
	}
	
	public boolean affordable(Inventory inventory, ItemStack price) {
		
            Map<Integer, ? extends ItemStack> items =
                    this.getItemsMatching(inventory, price);
            
            for (ItemStack item : items.values()) {
                if (item.getAmount() >= price.getAmount()) {
                    return true;
                }
            }

            return false;
		
	}

	public boolean purchase(final Player player, final int slot) {

            final ItemStack item = this.getInventory().getItem(slot);
            final ItemStack cloned = item.clone();
            
            cloned.setAmount(item.getAmount());
            cloned.setDurability(item.getDurability());
            cloned.setItemMeta(item.getItemMeta());
            
            // Ensure player has room.
            
            if (this.isFull(player.getInventory(), item)) {
                this.owner.say(player, ChatColor.RED + "You don't have room for that item!");
                return false;
            }
            
            // Ensure the item is for sale.

            final ItemShopInformation info = this.getItemInformation(item);
            
            if (!info.purchasable()) {
                this.owner.say(player, ChatColor.RED + "That item is not for sale.");
                return false;
            }

            //  Determine what price, if any, the player can afford.

            ItemStack priceAvailable = null;
            
            for (ItemStack e : info.prices) {
                if (affordable(player.getInventory(), e)) {
                    priceAvailable = e;
                    break;
                }
            }
            
            final ItemStack price = priceAvailable;
            
            // If the shop still has the item then sell it to
            // the player.
            
            if (price != null && affordable(this.getInventory(), item)) {

                // Strip the pricing information and remove metadata
                // for non-specific blocks.

                OwnedEntityShop.removePricingInformation(item);

                // Transfer price items to inventory

                Bukkit.getScheduler().runTask(NpcPlugin.INSTANCE, new Runnable() {

                    @Override
                    public void run() {

                        // Deduct price from player's inventory.

                        OwnedEntityShop.this.deduct(player.getInventory(), price);

                        // Transfer price to shop inventory / profit inventory.
                        // Reset inventory slot if the shop is configured to
                        // be infinite. Infinite shops do not collect profit.

                        if (!OwnedEntityShop.this.hasFiniteInventory()) {
                            OwnedEntityShop.this.getInventory().setItem(slot, cloned);
                        }
                        else {
                            if (OwnedEntityShop.this.getProfitChest() != null) {
                                OwnedEntityShop.this.getProfitInventory().addItem(price);
                            }
                            else{                                                                
                                OwnedEntityShop.this.getInventory().addItem(price);
                            }
                        }

                        player.updateInventory();

                    }

                });

                return true;
            }

            this.owner.say(player, ChatColor.RED + "You can't afford that.");
            
            return false;

	}
        
        public final void setHasFiniteInventory(boolean value) {
            this.isInventoryFinite = value;
            if (this.owner != null) {
                this.owner.getRole().getMetadata().put("shop.finite", String.valueOf(value));
            }
        }
        
        public boolean hasFiniteInventory() {
            return this.isInventoryFinite;
        }
	
	public final void setOwner(OwnedEntity owner) {
            this.owner = owner;
	}
	
	public Chest getInventoryChest() {
            return this.stockChest;
	}
	
	public final void setInventoryChest(Chest value) {
            
            this.stockChest = value;
            
            if (value != null) {
                    this.owner.set("shop.stock", getLocationString(value.getLocation()));
            }
            else {
                this.owner.unset("shop.stock");
            }
            
	}
	
	public Inventory getInventory() {
            if (this.stockChest == null)
                    return null;
            return this.stockChest.getInventory();
	}
	
	public Chest getPricesInventoryChest() {
            return this.pricesChest;
	}
	
	public final void setPricesInventoryChest(Chest value) {
            
            this.pricesChest = value;
            
            if (value != null) {
                    this.owner.set("shop.prices", getLocationString(value.getLocation()));
            }
            else {
                this.owner.unset("shop.prices");
            }
            
	}
        
        public Chest getProfitChest() {
            return this.profitChest;
        }
        
        public Inventory getProfitInventory() {
            if (this.profitChest == null) 
                return null;
            return this.profitChest.getInventory();
        }
        
        public final void setProfitInventoryChest(Chest value) {
            
            this.profitChest = value;
            
            if (value != null) {
                this.owner.set("shop.profit", getLocationString(value.getLocation()));
            }
            else {
                this.owner.unset("shop.profit");
            }
            
        }
	
	public Inventory getPricesInventory() {
            if (this.pricesChest == null)
                    return null;
            return this.pricesChest.getInventory();
	}
	
        /***
         * Determines if the shop is vending the specified player.
         * 
         * @param player player to check
         * @return true if vending the player, false otherwise
         */
	public boolean isVending(final Player player) {
		
            boolean result = false;

            if (this.stockChest != null) {
                    InventoryHolder holder = this.stockChest.getInventory().getHolder();
                    if (holder instanceof DoubleChest) {
                        Chest left = (Chest)((DoubleChest) holder).getLeftSide();
                        Chest right = (Chest)((DoubleChest) holder).getRightSide();
                        result = left.getBlockInventory().getViewers().contains(player) ||
                                        right.getBlockInventory().getViewers().contains(player);
                    }
                    else {
                        result = this.stockChest.getInventory().getViewers().contains(player);
                    }
            }

            return result;
		
	}
        
        public boolean hasStock() {
            
            final Inventory stock = this.getInventory();
            final Inventory prices = this.getPricesInventory();
            
            if (stock != null && prices != null) {
                for (ItemStack price : prices.getContents()) {
                    if (price != null && stock.contains(price.getType(), price.getAmount())) {
                        return true;
                    }
                }
            }
            
            return false;
            
        }
        
        public boolean isEmpty(Chest chest) {
         
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null)
                    return false;
            }
            
            return true;
            
        }
        
        public boolean isFull(Inventory inventory, ItemStack itemToCheck) {
            
            return inventory.firstEmpty() < 0;
            
//            for (ItemStack item : inventory.getContents()) {
//                if (item == null)
//                    return false;
//                else if (itemToCheck != null) {
//                    if (item.getType() == itemToCheck.getType() &&
//                            item.getAmount() != item.getMaxStackSize()) {
//                        return false;
//                    }
//                }
//            }
//                
//            return true;
            
        }
        
	private void deduct(Inventory inventory, ItemStack item) {
            
            Map<Integer, ? extends ItemStack> items = 
                   this.getItemsMatching(inventory, item);
            
            for (ItemStack stack : items.values()) {
                if (stack.getAmount() >= item.getAmount()) {
                    int difference = (stack.getAmount() - item.getAmount());
                    if (difference > 0) {
                        stack.setAmount(difference);
                    }
                    else {
                        inventory.removeItem(stack);
                    }
                    break;
                }
            }
            
	}
        
        private String getLocationString(Location location) {
            
            return StringUtils.join(new Object[] { 
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ() }, ",");
            
        }

        private Map<Integer, ? extends ItemStack> getItemsMatching(Inventory inventory, ItemStack itemToMatch) {

            Map<Integer, ItemStack> results =  new HashMap<>();
            Map<Integer, ? extends ItemStack> items = 
                    inventory.all(itemToMatch.getType());
            
            for (Integer slot : items.keySet()) {
                ItemStack item = (ItemStack) items.get(slot);
                if (item.isSimilar(itemToMatch)) {
                    results.put(slot, item);
                }
            }

            return results;

        }
        
        private Chest getChestFromMetadata(String key) {
            
            Map<String, String> metadata = this.owner.getRole().getMetadata();
            
            if (metadata.containsKey(key)) {
                String[] parts = metadata.get(key).split(",");
                if (parts.length == 3) {
                    Block block = this.owner.getEntity().getWorld().getBlockAt(
                        Integer.parseInt(parts[0]), 
                        Integer.parseInt(parts[1]), 
                        Integer.parseInt(parts[2]));
                    if (block != null && block.getType() == Material.CHEST) {
                        return (Chest) block.getState();
                    }
                }
            }
            
            return null;
            
        }
        
        public static void removePricingInformation(ItemStack item) {
            
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName() || meta.hasEnchants()) {
                meta.setLore(null);
            }
            else {
                meta = null;
            }

            item.setItemMeta(meta);
            
        }
	
}
