package net.rpgtoolkit.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import net.rpgtoolkit.minecraft.items.OwnedEntityItemFactory;
import net.rpgtoolkit.minecraft.persistence.OwnedEntityRecord;
import net.rpgtoolkit.minecraft.persistence.OwnedEntityMetadataRecord;
import org.bukkit.ChatColor;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcPlugin extends JavaPlugin implements Listener {

	public static NpcPlugin INSTANCE = null;

	private OwnedEntityRepository repository;
	private OwnedEntityItemFactory creatorItemFactory;
	
	@Override
	public void onEnable() {
		
                NpcPlugin.INSTANCE = this;
                
		this.setup();
		
                // Load saved NPC data
                
		try {
			this.repository.load();
			
			for (final World world : this.getServer().getWorlds()) {
				for (final Chunk chunk : world.getLoadedChunks()) {
					this.repository.bind(chunk);
				}
			}
		}
		catch (IOException ex) {
			this.getLogger().log(Level.SEVERE, "Failed to load saved NPCs!", ex);
		}
	}
	
	@Override
	public void onDisable() {
           	
		for (final World world : this.getServer().getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				this.repository.unbind(chunk);
			}
		}
              
                NpcPlugin.INSTANCE = null;
                
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
				
		this.repository.bind(event.getChunk());
		
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		
		this.repository.unbind(event.getChunk());
		
        }

        @EventHandler
        public void onPotionSplash(PotionSplashEvent event) {
            
            if (event.getEntity().getShooter() instanceof Player) {
                
                final Player player = (Player) event.getEntity().getShooter();
                final ItemStack item = event.getEntity().getItem();
                
                if (item.hasItemMeta() && this.creatorItemFactory.isCreatorItem(item)) {
                
                    if (!this.creatorItemFactory.isItemValid(item)) {
                        player.sendMessage(ChatColor.RED + "You must name your companion!");
                        player.getWorld().dropItem(event.getEntity().getLocation(),
                                this.creatorItemFactory.getItem());
                        return;
                    }
                    
                    Location loc = player.getTargetBlock(null, 5).getLocation();
                    loc.setY(loc.getY() + 1);
                    
                    final ItemMeta meta = item.getItemMeta();
                    final String itemName = meta.getDisplayName().trim();
                    final OwnedEntity villager = this.createOwnedVillager(loc, player, itemName);
                    
                    if (villager != null) {	
                        player.sendMessage(
                                villager.getEntity().getCustomName() + " has arrived!");
                    }

                }
                
            }
            
        }

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		
		final Entity entity = event.getEntity();
		
                // Ensure entity is removed from the 
                // repository
                
		if (entity.getType() == EntityType.VILLAGER) {
                    
                    String key = entity.getUniqueId().toString();
                    OwnedEntity villager = this.repository.get(key);
                    
                    if (villager != null) {
                        villager.getRole().dead(event);
                        this.getServer().broadcastMessage(ChatColor.RED + String.format("%s %s",
                                villager.getDisplayName(), getCauseOfDeath(entity)));
                        this.repository.remove(key);
                    }
                    
		}
		
		// Ensure selection is reset if their selected entity
		// has died.
		
		for (Player player : this.getServer().getOnlinePlayers()) {
                    if (player.hasMetadata(OwnedEntityMetadata.SELECTED)) {
                        if (player.getMetadata(OwnedEntityMetadata.SELECTED).get(0).asString().equals(
                                        entity.getUniqueId().toString())) {
                                player.removeMetadata(OwnedEntityMetadata.SELECTED, this);
                        }
                    }
		}
		
	}
	
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent event) {

		if (event.getDamager() instanceof Player) {
			if (event.getEntity() instanceof Villager) {
				OwnedEntity villager = 
						this.repository.get(event.getEntity().getUniqueId().toString());
				if (villager != null) {
					villager.getRole().attack(event);
					if (villager.getRole().dirty()) {
						this.repository.update(villager);
					}
				}
			}
		}
		
	}
	
	@EventHandler
	public void onPlayerCloseInventory(InventoryCloseEvent event) {
		
            Player player = (Player) event.getPlayer();
            for (OwnedEntity villager : this.repository.all()) {
                if (villager.getRole().interacting(player)) {
                    villager.getRole().closeInventory(event);
                    break;
                }
            }
		
	}
	
	@EventHandler
	public void onPlayerInteractInventory(InventoryClickEvent event) {

            for (OwnedEntity villager : this.repository.all()) {
                villager.getRole().iteractWithInventory(event);
            }
		
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
				
		if (event.getRightClicked() instanceof Villager) {
			OwnedEntity owned = this.repository.get(
					event.getRightClicked().getUniqueId().toString());
			if (owned != null) {
				owned.getRole().interact(event);
				if (owned.getRole().dirty()) {
					this.repository.update(owned);
				}
			}
		}
		
	}
	
	@EventHandler
	public void onPlayerInteractBlock(PlayerInteractEvent event) {

            if (event.getAction() != Action.LEFT_CLICK_BLOCK)
                    return;

            final Player player = event.getPlayer();
            final ItemStack item = player.getItemInHand();

            if (item != null && item.getType().equals(Material.BLAZE_ROD)) {

                if (player.hasMetadata(OwnedEntityMetadata.SELECTED)) {
                        List<MetadataValue> selected = player.getMetadata(OwnedEntityMetadata.SELECTED);

                        OwnedEntity villager = this.repository.get(selected.get(0).asString());
                        if (villager != null) {
                                villager.getRole().interactWithBlock(event);
                                if (villager.getRole().dirty()) {
                                        this.repository.update(villager);
                                }
                        }
                }


            }
		
	}
	
	@Override
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> classes = new ArrayList<>();
		classes.add(OwnedEntityRecord.class);
		classes.add(OwnedEntityMetadataRecord.class);
		return classes;
	}
        
        public OwnedEntityRepository getRepository() {
            return this.repository;
        }
	
	private void setup() {

            // Create owned NPC repository and create the items and recipes
            // necessary to control them.
            
            this.repository = new OwnedEntityRepository(this);
            this.creatorItemFactory = new OwnedEntityItemFactory(this.repository);
            this.getServer().addRecipe(
                            this.creatorItemFactory.getRecipe());

            // Simple check to ensure database exists, and if not
            // creates the database(s) necessary.
            
            try {
                    this.getDatabase().find(OwnedEntityRecord.class).findRowCount();
            }
            catch (PersistenceException ex) {
                    this.installDDL();
            }
            
            // Register plugin for event handling
            
            this.getServer().getPluginManager().registerEvents(this, this);
            
            // Register commands
            
            this.getCommand("npc").setExecutor(new NpcPluginCommandExecutor(this));
            
	}
	
	private OwnedEntity createOwnedVillager(Location location, Player player, String name) {
		
		// Spawn effect
		
		player.getWorld().createExplosion(location, 0f);
		
		// Add villager to repository
		
		OwnedEntity owned = OwnedEntity.create(location, player, name);
		if (owned != null) {
			this.repository.add(owned);
		}
		
		return owned;
		
	}
        
        private String getCauseOfDeath(Entity entity) {
            
            EntityDamageEvent lastDamageEvent = entity.getLastDamageCause();
            
            if (lastDamageEvent != null) {
                DamageCause cause = lastDamageEvent.getCause();
                switch (cause) {
                    case BLOCK_EXPLOSION:
                        return "died from an explosion";
                    case CONTACT:
                        return "died from contact with a block";
                    case DROWNING:
                        return "has drown";
                    case ENTITY_ATTACK:
                        return "died from blunt force";
                    case ENTITY_EXPLOSION:
                        return "has been molested by a Creeper";
                    case FALL:
                        return "fell to their death";
                    case FALLING_BLOCK:
                        return "died from a falling block";
                    case FIRE:
                        return "burst into flames";
                    case FIRE_TICK:
                        return "died from severe burns";
                    case LAVA:
                        return "fell into lava";
                    case LIGHTNING:
                        return "died from a lightning strike";
                    case MAGIC:
                        return "sparkled out of existence";
                    case MELTING:
                        return "melted away";
                    case POISON:
                        return "died from poison";
                    case PROJECTILE:
                        return "died by a projectile volley";
                    case STARVATION:
                        return "starved to death";
                    case SUICIDE:
                        return "ended their own life";
                    case VOID:
                        return "fell into the void";
                    case WITHER:
                        return "withered out of existence";
                }
            }
  
            return "has died!";
            
        }

	
}
