/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 *
 * @author Chris Hutchinson
 */
public class OwnedEntityFactory {
    
    public OwnedEntity<?> create(Player owner, EntityType entityType) {
        
        return null;
        
    }
    
    private static boolean validate(EntityType type) {
        switch (type) {
            case CREEPER:
                return true;
        }
        return false;
    }
    
    private static LivingEntity spawn(EntityType type, World world, Location location) {

        return (LivingEntity) world.spawnEntity(location, type);
        
    }
    
}
