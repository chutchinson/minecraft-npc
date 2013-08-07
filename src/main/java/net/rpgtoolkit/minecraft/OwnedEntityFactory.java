/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import net.rpgtoolkit.minecraft.roles.ShopkeeperRole;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/**
 *
 * @author Chris Hutchinson
 */
public class OwnedEntityFactory {
    
    private OwnedEntityRepository repository;
    
    public OwnedEntityFactory(OwnedEntityRepository repository) {
        this.repository = repository;
    }
    
    public OwnedEntity<?> spawn(Player owner, Location location, String name) {
        
        LivingEntity entity = 
                (LivingEntity) owner.getWorld().spawnEntity(location, EntityType.VILLAGER);
        
        if (entity != null) {
            OwnedEntity<?> ownedEntity = OwnedEntityFactory.attach(
                    owner.getName(), entity);
            if (ownedEntity != null) {
                this.repository.add(ownedEntity);
            }
            return ownedEntity;
        }
        
        return null;
        
    }
    
    public static OwnedEntity<?> attach(String owner, LivingEntity entity) {
        
        if (!OwnedEntityFactory.validate(entity.getType())) {
            return null;
        }
        
        OwnedEntity<?> result = null;
        
        switch (entity.getType()) {
            case VILLAGER:
                result = new OwnedEntity<Villager>(entity, owner);
                break;
            case CREEPER:
                result = new OwnedEntity<Creeper>(entity, owner);
                break;
        }
        
        if (result != null) {
            result.setRole(new ShopkeeperRole(result));
        }
        
        return result;
        
    }
    
    private static boolean validate(EntityType type) {
        switch (type) {
            case VILLAGER:
            case CREEPER:
            case ZOMBIE:
            case SKELETON:
                return true;
            default:
                return false;
        }
    }
    
}
