package net.rpgtoolkit.minecraft;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import net.rpgtoolkit.minecraft.persistence.OwnedEntityRecord;
import net.rpgtoolkit.minecraft.persistence.OwnedEntityMetadataRecord;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import com.avaje.ebean.EbeanServer;

public class OwnedEntityRepository {

	private Plugin plugin;
	private EbeanServer database;
	private Map<String, OwnedEntity> villagers;
	private Map<String, OwnedEntityRecord> records;
	
	public OwnedEntityRepository(Plugin plugin) {
		this.plugin = plugin;
		this.database = plugin.getDatabase();
		this.villagers = new HashMap<String, OwnedEntity>();
		this.records = new HashMap<String, OwnedEntityRecord>();
	}
	
	public Collection<OwnedEntity> all() {
		return this.villagers.values();
	}
	
	public void add(OwnedEntity villager) {
		if (villager != null) {
			this.villagers.put(villager.getId(), villager);
			
			OwnedEntityRecord entity = new OwnedEntityRecord(villager);

			this.records.put(villager.getId(), entity);
			this.database.save(entity);
		}
	}
	
	public void remove(String key) {
		
		this.villagers.remove(key);
		this.records.remove(key);
		
		// Remove all metadata for this entity and then remove
                // the entity itself.
		
		try {            
                        this.database.beginTransaction();
                        this.database.delete(this.database.find(OwnedEntityMetadataRecord.class).where()
                                .eq("reference", key).findList());
                        this.database.delete(this.database.find(OwnedEntityRecord.class).where()
                                .eq("reference", key).findList());
                        this.database.commitTransaction();
		}
		catch (PersistenceException ex) {
			this.plugin.getLogger().log(Level.SEVERE, 
					"Failed to delete all NPC data", ex);
		}

	}
	
	public void update(OwnedEntity villager) {
		
		try {
			OwnedEntityRecord entity = this.records.get(
					villager.getId());
			
			if (entity != null) {
				entity.setName(villager.getName());
				entity.setOwner(villager.getOwner());
				entity.setRole(villager.getRole().getTitle());
				
				Map<String, String> metadata = 
						villager.getRole().getMetadata();
				
				if (metadata.size() > 0) {
					
					// Delete all existing metadata for the current role
					
					this.database.delete(this.database.find(OwnedEntityMetadataRecord.class).where()
							.eq("reference", villager.getId())
							.eq("role", villager.getRole().getTitle()).findList());
					
					// Add updated / new metadata
					
					for (String key : metadata.keySet()) {
						
						OwnedEntityMetadataRecord record = 
								new OwnedEntityMetadataRecord();
						
						record.setKey(key);
						record.setValue(metadata.get(key));
						record.setRole(entity.getRole());
						record.setReference(entity.getReference());
						
						this.database.insert(record);
						
					}
				}
			}
			
			// Update existing NPC record
			
			this.database.update(entity);
			
			// Reset NPC dirty flag
			
			villager.getRole().setDirty(false);
			
		}
		catch (PersistenceException ex) {
			this.plugin.getLogger().log(Level.SEVERE,
					"Could not update NPC record", ex);
		}

	}
	
	public OwnedEntity get(String id) {
		if (this.villagers.containsKey(id)) {
			return this.villagers.get(id);
		}
		return null;
	}
	
	public void bind(Chunk chunk) {
		if (chunk != null) {
			for (Entity entity : chunk.getEntities()) {
				if (entity.getType() == EntityType.VILLAGER) {
					this.bind(entity.getUniqueId().toString(), (Villager) entity);
				}
			}
		}
	}
	
	public void bind(String key, Villager npc) {
		if (this.records.containsKey(key)) {
			OwnedEntityRecord entity = this.records.get(key);
			OwnedEntity villager = OwnedEntity.attach(npc, entity.getOwner());
			if (villager != null) {
				villager.setName(entity.getName());
				
				// Bind all metadata
				
				for (OwnedEntityMetadataRecord metadata : 
					this.getMetadata(key, villager.getRole().getTitle())) {
					villager.getRole().getMetadata().put(
							metadata.getKey(), metadata.getValue());
				}
				
				this.villagers.put(villager.getId(), villager);
			}
		}
	}
	
	public void unbind(Chunk chunk) {
		if (chunk != null) {
			for (Entity entity : chunk.getEntities()) {
				if (entity.getType() == EntityType.VILLAGER) {
					this.unbind(entity.getUniqueId().toString());
				}
			}
		}
	}
	
	public void unbind(String key) {
		this.villagers.remove(key);
	}
	
	public void load() throws IOException {
		
		if (this.database != null) {
			this.plugin.getLogger().info("Loading owned NPCs from database...");
			try {
				List<OwnedEntityRecord> entities = 
						this.database.find(OwnedEntityRecord.class).findList();
				for (OwnedEntityRecord entity : entities) {
					this.records.put(entity.getReference(), entity);
				}
			}
			catch (PersistenceException ex) {
				this.plugin.getLogger().log(Level.SEVERE, 
						"Failed to load owned NPCs", ex);
			}
		}
		
	}
	
	private List<OwnedEntityMetadataRecord> getMetadata(String key, String role) {
		return this.database.find(OwnedEntityMetadataRecord.class).where()
				.eq("reference", key)
				.eq("role",  role).findList();
	}
	
	private Player findPlayer(String name) {
		Player result = Bukkit.getPlayerExact(name);
		if (result == null) {
			return (Player) Bukkit.getOfflinePlayer(name);
		}
		return result;
	}

}
