/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import net.rpgtoolkit.minecraft.roles.ShopkeeperRole;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Chris Hutchinson
 */
public class OwnedEntityFactory {

    private static final String defaultRole = "Shopkeeper";
    private static final Collection<EntityType> validEntityTypes =
            Arrays.asList(new EntityType[]{
        EntityType.VILLAGER,
        EntityType.CREEPER,
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.IRON_GOLEM,
        EntityType.SNOWMAN,
        EntityType.WITCH
    });

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
                    owner.getName(), defaultRole, entity);

            if (ownedEntity != null) {

                ownedEntity.setName(name);

                // Set various properties for entities based on the entity type.
                // For example, pick a random profession for a villager.

                switch (entity.getType()) {
                    case VILLAGER:
                        ((Villager) entity).setProfession(getRandomProfession());
                        break;
                }

            }

            return ownedEntity;
        }

        return null;

    }

    public static OwnedEntity<?> attach(String owner, String role, LivingEntity entity) {

        if (!validEntityTypes.contains(entity.getType())) {
            return null;
        }

        // Wrap owned entity around the created living entity
        // and set the default role.

        OwnedEntity<?> result = new OwnedEntity<>(entity, owner);

        switch (role) {
            case "Shopkeeper":
                result.setRole(new ShopkeeperRole(result));
        }

        return result;

    }

    public static Collection<EntityType> getValidEntityTypes() {

        return validEntityTypes;

    }

    private static Profession getRandomProfession() {
        Random ran = new Random(System.currentTimeMillis());
        return Profession.getProfession(ran.nextInt(
                Profession.values().length));
    }
}
