package net.rpgtoolkit.minecraft.items;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

public final class OwnedEntityItemFactory {

    private static final String DEFAULT_NAME = "Companion Potion";
    private static final String DEFAULT_LORE = "Creates a new companion.";
    private static final Material DEFAULT_MATERIAL = Material.POTION;
    private String name;
    private String lore;
    private Material material;

    public OwnedEntityItemFactory() {

        this.name = DEFAULT_NAME;
        this.lore = DEFAULT_LORE;
        this.material = DEFAULT_MATERIAL;

    }

    public ItemStack getItem(EntityType entityType) {

        ItemStack item = new ItemStack(this.material, 1, (short) 16384);
        ItemMeta meta =
                Bukkit.getServer().getItemFactory().getItemMeta(this.material);

        meta.setLore(Arrays.asList(this.lore, entityType.getName()));
        meta.setDisplayName(this.name);

        item.setItemMeta(meta);

        return item;

    }

    public Recipe getRecipe(EntityType entityType) {

        ShapedRecipe recipe = new ShapedRecipe(this.getItem(entityType));

        recipe.shape("fbf", "ece", "ada");

        switch (entityType) {
            case SKELETON:
                recipe.setIngredient('b',
                        new MaterialData(Material.SKULL_ITEM, (byte) 0));
                break;
            case VILLAGER:
                recipe.setIngredient('b',
                        new MaterialData(Material.SKULL_ITEM, (byte) 3));
                break;
            case CREEPER:
                recipe.setIngredient('b',
                        new MaterialData(Material.SKULL_ITEM, (byte) 4));
                break;
            case ZOMBIE:
                recipe.setIngredient('b',
                        new MaterialData(Material.SKULL_ITEM, (byte) 2));
                break;
            case IRON_GOLEM:
                recipe.setIngredient('b', Material.IRON_BLOCK);
                break;
            case WITCH:
                recipe.setIngredient('b', Material.CAULDRON_ITEM);
                break;
            case SNOWMAN:
                recipe.setIngredient('b', Material.JACK_O_LANTERN);
                break;
        }

        recipe.setIngredient('a', Material.CHEST);
        recipe.setIngredient('c', Material.LEATHER_CHESTPLATE);
        recipe.setIngredient('d', Material.LEATHER_LEGGINGS);
        recipe.setIngredient('e', Material.BONE);
        recipe.setIngredient('f', Material.ENDER_PEARL);

        return recipe;

    }

    public boolean isItemValid(ItemStack item) {

        String itemName = (item.hasItemMeta() ? item.getItemMeta().getDisplayName() : null);

        if (itemName == null) {
            return false;
        }
        if (itemName.trim().length() == 0) {
            return false;
        }
        if (itemName.equalsIgnoreCase(this.name)) {
            return false;
        }

        return true;

    }

    public boolean isCreatorItem(ItemStack item) {

        if (item == null) {
            return false;
        }
        if (item.getType() == this.material && item.hasItemMeta()) {
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null && lore.size() > 0) {
                return lore.get(0).equals(this.lore);
            }
        }

        return false;

    }
}
