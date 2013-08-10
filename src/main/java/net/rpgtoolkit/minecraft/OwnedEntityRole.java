package net.rpgtoolkit.minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rpgtoolkit.minecraft.items.OwnedEntityItemFactory;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class OwnedEntityRole {

	protected OwnedEntity entity;
	protected String title;
	protected Map<String, String> metadata;
        protected List<Player> playersInteracting;
	protected boolean dirty;
	
	public OwnedEntityRole(OwnedEntity owner, String title) {
		if (owner == null) {
			throw new NullPointerException();
		}
		this.entity = owner;
		this.title = title;
		this.metadata = new HashMap<>();
                this.playersInteracting = new ArrayList<>();
		this.dirty = false;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public boolean dirty() {
		return this.dirty;
	}
	
	public void setDirty(boolean value) {
		this.dirty = value;
	}
	
	public boolean interacting(final Player player) {
		return this.playersInteracting.contains(player);
	}
        
        public void startInteracting(final Player player) {
            if (!this.playersInteracting.contains(player)) {
                this.playersInteracting.add(player);
            }
        }
        
        public void stopInteracting(final Player player) {
            this.playersInteracting.remove(player);
        }
	
	protected abstract void update();
	
	public void attack(EntityDamageByEntityEvent event) {
		
		Player player = (Player) event.getDamager();
                	
		event.setCancelled(true);
		
		if (player.hasMetadata("npc.selected")) {
                    String key = player.getMetadata("npc.selected").get(0).asString();
                    if (!this.entity.getId().equals(key)) {
                            this.entity.say(player, "You can only interact with one of us.");
                            return;
                    }
		}
		
		if (player.getItemInHand().getTypeId() == Material.BLAZE_ROD.getId()) {
			
                    if (player.getName().equals(this.entity.getOwner()) || player.isOp()) {
                        this.entity.update((LivingEntity) event.getEntity());
                        this.entity.select(player, !this.entity.selected());
                    }
                    else {
                        this.entity.say(player, "I don't recognize you.");
                    }

		}
		
		this.onAttack(event);
		
	}
	
	public void interact(PlayerInteractEntityEvent event) {
                this.update();
                
                final Player player = event.getPlayer();
                
                // Remove entity from the world and transform back into
                // a potion.
                
                if (player != null && player.getItemInHand().getTypeId() == Material.BLAZE_ROD.getId()) {
                    if (this.entity.selected() && this.entity.getOwner().equals(player.getName())) {
                        
                        // Kill the entity.
                        
                        this.entity.getEntity().damage(
                                this.entity.getEntity().getHealth() + 1);
                        
                        // TODO: Decouple this and consider removing from the role
                        // to the main plugin interface.
                        
                        OwnedEntityItemFactory factory = new OwnedEntityItemFactory(
                                NpcPlugin.INSTANCE.getRepository());

                        ItemStack item = 
                                factory.getItem(this.entity.getEntity().getType());
                        ItemMeta meta = item.getItemMeta();

                        meta.setDisplayName(this.entity.getName());
                        item.setItemMeta(meta);

                        player.getWorld().createExplosion(player.getLocation(), 0f);
                        player.getWorld().dropItem(player.getLocation(), item);

                        event.setCancelled(true);
                        return;
                    }
                }
                
		this.onInteraction(event);
	}
	
	public void iteractWithInventory(InventoryClickEvent event) {
		this.onInventoryInteraction(event);
	}
	
	public void closeInventory(InventoryCloseEvent event) {
		this.onInventoryClosed(event);
	}
	
	public void interactWithBlock(PlayerInteractEvent event) {
		
            Player player = (Player) event.getPlayer();

            if (this.isInteractableBlock(event.getClickedBlock())) {
                if (player.getGameMode() == GameMode.CREATIVE) {
                    event.setCancelled(true);
                }
                this.onBlockInteraction(event);
            }
            else {
                if (this.entity.getEntity().getLocation().distance(player.getLocation()) <= 32) {
                        this.entity.getController().getActions().moveTo(
                                        event.getClickedBlock().getLocation());
                }
                else {
                        this.entity.say(player, "Sorry, that's too far away.");
                }
            }
		
	}
	
	public void dead(EntityDeathEvent event) {
            
            this.onDeath(event);
            
            // Find the owner and remove any NPC-specific metadata.
            
            final Player owner = Bukkit.getPlayer(this.entity.getOwner());
            
            if (owner != null && owner.hasMetadata(OwnedEntityMetadata.SELECTED)) {
                String selection = owner.getMetadata(OwnedEntityMetadata.SELECTED).get(0).asString();
                if (selection.equals(this.entity.getId())) {
                    owner.removeMetadata(OwnedEntityMetadata.SELECTED, NpcPlugin.INSTANCE);
                }
            }

            // Remove interactions.
            
            this.playersInteracting.clear();
                
	}
	
	public Map<String, String> getMetadata() {
		return this.metadata;
	}
	
	protected boolean isInteractableBlock(Block block) {
		switch (block.getType()) {
			case CHEST:
				return true;
			default:
				return false;
		}
	}

	protected abstract void onAttack(EntityDamageByEntityEvent event);
	protected abstract void onInventoryInteraction(InventoryClickEvent event);
	protected abstract void onInventoryClosed(InventoryCloseEvent event);
	protected abstract void onInteraction(PlayerInteractEntityEvent event);
	protected abstract void onBlockInteraction(PlayerInteractEvent event);
	protected abstract void onDeath(EntityDeathEvent event);
	
}
