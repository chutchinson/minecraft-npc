/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import java.util.Arrays;
import java.util.Collection;

import net.rpgtoolkit.minecraft.roles.ShopkeeperRole;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Chris Hutchinson
 */
public class OwnedEntityFactory {
    
    private static final Collection<EntityType> validEntityTypes = 
            Arrays.asList(new EntityType[] {
                EntityType.VILLAGER,
                EntityType.CREEPER,
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.IRON_GOLEM,
                EntityType.SNOWMAN,
                EntityType.WITCH
            });
    
    private OwnedEntityRepository repository;
    
    public OwnedEntityFactory(OwnedEntityRepository repository) {
        this.repository = repository;
    }
    
    public OwnedEntity<?> spawn(Player owner, Location location, ItemMeta meta) {
        
        final World world = owner.getWorld();
        
        String name = meta.getDisplayName().trim();
        String itemEntityType = meta.getLore().get(1);
     
        // Determine the entity type based on the
        // item meta data.
        
        EntityType entityType = EntityType.fromName(itemEntityType);
                
        // Spawn effects.
        
        world.createExplosion(location, 0f);

        // Spawn a new entity based on the item details.
        
        LivingEntity entity = 
                (LivingEntity) world.spawnEntity(location, entityType);
        
        if (entity != null) {
            OwnedEntity<?> ownedEntity = OwnedEntityFactory.attach(
                    owner.getName(), entity);
            if (ownedEntity != null) {
                ownedEntity.setName(name);
                this.repository.add(ownedEntity);
            }
            return ownedEntity;
        }
        
        return null;
        
    }
    
    public static OwnedEntity<?> attach(String owner, LivingEntity entity) {
        
        if (!validEntityTypes.contains(entity.getType())) {
            return null;
        }
        
        // Set default properties for most living entities.
        
        entity.setRemoveWhenFarAway(false);
        entity.setCanPickupItems(true);
        
        OwnedEntity<?> result = null;
        
        switch (entity.getType()) {
            case VILLAGER:
                result = new OwnedEntity<Villager>(entity, owner);
                break;
            case CREEPER:
                result = new OwnedEntity<Creeper>(entity, owner);
                break;
            case SKELETON:
                result = new OwnedEntity<Skeleton>(entity, owner);
                break;
            case ZOMBIE:
                result = new OwnedEntity<Zombie>(entity, owner);
                break;
            case IRON_GOLEM:
                result = new OwnedEntity<IronGolem>(entity, owner);
                break;
            case SNOWMAN:
                result = new OwnedEntity<Snowman>(entity, owner);
                break;
            case WITCH:
                result = new OwnedEntity<Witch>(entity, owner);
                break;
        }
        
        if (result != null) {
            result.setRole(new ShopkeeperRole(result));
        }
        
        return result;
        
    }
    
    public static Collection<EntityType> getValidEntityTypes() {
        
      return validEntityTypes;
        
    }    
    
}
