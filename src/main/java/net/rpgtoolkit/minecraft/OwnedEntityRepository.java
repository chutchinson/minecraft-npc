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

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import com.avaje.ebean.EbeanServer;
import org.bukkit.entity.LivingEntity;

public class OwnedEntityRepository {

    private Plugin plugin;
    private EbeanServer database;
    private Map<String, OwnedEntity> npcs;
    private Map<String, OwnedEntityRecord> records;

    public OwnedEntityRepository(Plugin plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
        this.npcs = new HashMap<>();
        this.records = new HashMap<>();
    }

    public Collection<OwnedEntity> all() {
        return this.npcs.values();
    }

    public void add(OwnedEntity entity) {
        if (entity != null) {
            this.npcs.put(entity.getId(), entity);

            OwnedEntityRecord record = new OwnedEntityRecord(entity);

            this.records.put(entity.getId(), record);
            this.database.save(record);
        }
    }

    public void remove(String key) {

        this.npcs.remove(key);
        this.records.remove(key);

        // Remove all metadata for this record and then remove
        // the record itself.

        try {
            this.database.beginTransaction();
            this.database.delete(this.database.find(OwnedEntityMetadataRecord.class).where()
                    .eq("reference", key).findList());
            this.database.delete(this.database.find(OwnedEntityRecord.class).where()
                    .eq("reference", key).findList());
            this.database.commitTransaction();
        } catch (PersistenceException ex) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Failed to delete all NPC data", ex);
        }

    }

    public void update(OwnedEntity entity) {

        try {
            OwnedEntityRecord record = this.records.get(entity.getId());

            if (record != null) {
                record.setName(entity.getName());
                record.setOwner(entity.getOwner());
                record.setRole(entity.getRole().getTitle());

                Map<String, String> metadata =
                        entity.getRole().getMetadata();

                if (metadata.size() > 0) {

                    // Delete all existing metadata for the current role.

                    this.database.delete(this.database.find(OwnedEntityMetadataRecord.class).where()
                            .eq("reference", entity.getId())
                            .eq("role", entity.getRole().getTitle()).findList());

                    // Add updated / new metadata.

                    for (String key : metadata.keySet()) {

                        OwnedEntityMetadataRecord metadataRecord =
                                new OwnedEntityMetadataRecord();

                        metadataRecord.setKey(key);
                        metadataRecord.setValue(metadata.get(key));
                        metadataRecord.setRole(record.getRole());
                        metadataRecord.setReference(record.getReference());

                        this.database.insert(metadataRecord);

                    }
                }
            }

            // Update existing NPC metadataRecord

            this.database.update(record);

            // Reset NPC dirty flag

            entity.getRole().setDirty(false);

        } catch (PersistenceException ex) {
            this.plugin.getLogger().log(Level.SEVERE,
                    "Could not update NPC record", ex);
        }

    }

    public OwnedEntity get(String id) {
        if (this.npcs.containsKey(id)) {
            return this.npcs.get(id);
        }
        return null;
    }

    public void bind(Chunk chunk) {
        if (chunk != null) {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof LivingEntity) {
                    this.bind(entity.getUniqueId().toString(), (LivingEntity) entity);
                }
            }
        }
    }

    public void bind(String key, LivingEntity npc) {

        if (this.records.containsKey(key)) {

            OwnedEntityRecord record = this.records.get(key);
            OwnedEntity entity = OwnedEntityFactory.attach(
                    record.getOwner(), record.getRole(), npc);

            if (entity != null) {

                this.npcs.put(entity.getId(), entity);

                // Bind persisted data

                entity.setName(record.getName());

                // Bind all metadata

                for (OwnedEntityMetadataRecord metadata
                        : this.getMetadata(key, record.getRole())) {
                    entity.getRole().getMetadata().put(
                            metadata.getKey(), metadata.getValue());
                }

                entity.getRole().update();

            }
        }

    }

    public void unbind(Chunk chunk) {
        if (chunk != null) {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof LivingEntity) {
                    this.unbind(entity.getUniqueId().toString());
                }
            }
        }
    }

    public void unbind(String key) {
        OwnedEntity entity = this.npcs.get(key);
        if (entity != null) {
            this.update(entity);
        }
        this.npcs.remove(key);
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
            } catch (PersistenceException ex) {
                this.plugin.getLogger().log(Level.SEVERE,
                        "Failed to load owned NPCs", ex);
            }
        }

    }

    private List<OwnedEntityMetadataRecord> getMetadata(String key, String role) {
        return this.database.find(OwnedEntityMetadataRecord.class).where()
                .eq("reference", key)
                .eq("role", role).findList();
    }
}
