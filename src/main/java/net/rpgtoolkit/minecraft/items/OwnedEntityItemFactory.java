package net.rpgtoolkit.minecraft.items;

import java.util.Arrays;
import java.util.List;

import net.rpgtoolkit.minecraft.OwnedEntityRepository;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
	private OwnedEntityRepository repository;
	
	public OwnedEntityItemFactory(OwnedEntityRepository repository) {
            
            this.name = DEFAULT_NAME;
            this.lore = DEFAULT_LORE;
            this.material = DEFAULT_MATERIAL;
            this.repository = repository;
            
	}
	
	public ItemStack getItem() {
	
            ItemStack item = new ItemStack(this.material, 1, (short) 16384);
            ItemMeta meta = 
                            Bukkit.getServer().getItemFactory().getItemMeta(this.material);

            meta.setLore(Arrays.asList(this.lore));
            meta.setDisplayName(this.name);

            item.setItemMeta(meta);

            return item;
		
	}
	
	public Recipe getRecipe() {
            
		ShapedRecipe recipe = new ShapedRecipe(this.getItem());
                
		recipe.shape("fbf", "ece", "ada");
		recipe.setIngredient('a', Material.CHEST);
		recipe.setIngredient('b', new MaterialData(Material.SKULL_ITEM, (byte) 0));
                recipe.setIngredient('c', Material.LEATHER_CHESTPLATE);
                recipe.setIngredient('d', Material.LEATHER_LEGGINGS);
                recipe.setIngredient('e', Material.BONE);
                recipe.setIngredient('f', Material.ENDER_PEARL);
                
		return recipe;
                
	}
	
	public boolean isItemValid(ItemStack item) {
            
		String name = (item.hasItemMeta() ? item.getItemMeta().getDisplayName() : null);
                
		if (name == null)
			return false;
		if (name.trim().length() == 0)
			return false;
                if (name.equalsIgnoreCase(this.name))
                    return false;
                
		return true;
                
	}

	
	public boolean isCreatorItem(ItemStack item) {
            
		if (item == null)
			return false;
		if (item.getType() == this.material && item.hasItemMeta()) {
			List<String> lore = item.getItemMeta().getLore();
			if (lore != null && lore.size() > 0) {
				return lore.get(0).equals(this.lore);
			}
		}
                
		return false;
                
	}
	
}
