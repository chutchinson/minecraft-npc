package net.rpgtoolkit.minecraft.roles;

import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityRole;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CompanionRole extends OwnedEntityRole {

    public CompanionRole(OwnedEntity owner) {
        super(owner, "Companion");
    }

    @Override
    protected void onAttack(EntityDamageByEntityEvent event) {
    }

    @Override
    protected void onInventoryInteraction(InventoryClickEvent event) {
    }

    @Override
    protected void onInteraction(PlayerInteractEntityEvent event) {
    }

    @Override
    protected void onBlockInteraction(PlayerInteractEvent event) {
    }

    @Override
    protected void onDeath(EntityDeathEvent event) {
    }

    @Override
    protected void onInventoryClosed(InventoryCloseEvent event) {
    }

    @Override
    protected void update() {
    }
}
