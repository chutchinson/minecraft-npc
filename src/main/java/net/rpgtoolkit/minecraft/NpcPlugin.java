package net.rpgtoolkit.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.persistence.PersistenceException;
import net.rpgtoolkit.minecraft.items.OwnedEntityItemFactory;
import net.rpgtoolkit.minecraft.persistence.OwnedEntityMetadataRecord;
import net.rpgtoolkit.minecraft.persistence.OwnedEntityRecord;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcPlugin extends JavaPlugin implements Listener {

    public static NpcPlugin INSTANCE = null;
    private OwnedEntityRepository repository;
    private OwnedEntityItemFactory creatorItemFactory;
    private OwnedEntityFactory factory;

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
        } catch (IOException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load saved NPCs!", ex);
        }

    }

    @Override
    public void onDisable() {

        // Ensure entities are unbound from all loaded
        // chunks in order to persist all stored metadata
        // and properties

        this.getLogger().log(Level.INFO, "Saving NPC metadata before shutdown...");

        for (final World world : this.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                this.repository.unbind(chunk);
            }
        }

        this.getLogger().log(Level.INFO, "Saved all metadata.");

        NpcPlugin.INSTANCE = null;

    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {

        // Bind owned entities to all entities loaded in
        // the loaded chunk

        if (!event.isNewChunk()) {
            this.repository.bind(event.getChunk());
        }

    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {

        // Ensure that all owned entities in the unloaded
        // chunk get unbound and persisted

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
                            item.clone());
                    return;
                }

                Location loc = player.getTargetBlock(null, 5).getLocation();
                loc.setY(loc.getY() + 1);

                final ItemMeta meta = item.getItemMeta();
                final OwnedEntity entity =
                        this.factory.spawn(player, loc, meta);

                if (entity != null) {
                    this.repository.add(entity);
                    player.sendMessage(
                            entity.getEntity().getCustomName() + " has arrived!");
                }

            }

        }

    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {

        // Prevent owned entities from combusting.

        OwnedEntity entity = this.repository.get(
                event.getEntity().getUniqueId().toString());

        if (entity != null) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        final Entity entity = event.getEntity();

        // Ensure entity entities

        String key = entity.getUniqueId().toString();
        OwnedEntity ownedEntity = this.repository.get(key);

        if (ownedEntity != null) {

            // Ensure owned entities drop no experience or loot.

            event.setDroppedExp(0);
            event.getDrops().clear();

            // Relay death to the companion's role so it can perform
            // any special tasks.

            ownedEntity.getRole().dead(event);

            // Notify server that a companion has died and remove the companion
            // from the repository.

            this.getServer().broadcastMessage(ChatColor.RED + String.format("%s %s",
                    ownedEntity.getName(), getCauseOfDeath(entity)));

            this.repository.remove(key);

        }

    }

    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event) {

        OwnedEntity entity = this.repository.get(
                event.getEntity().getUniqueId().toString());

        // Prevent owned entities from being damaged by certain events
        // and circumstances.

        if (entity != null) {
            switch (event.getCause()) {
                case MELTING:
                    event.setCancelled(true);
                    break;
            }
        }

    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player) {
            if (event.getEntity() instanceof LivingEntity) {
                OwnedEntity entity = this.repository.get(
                        event.getEntity().getUniqueId().toString());
                if (entity != null) {
                    entity.getRole().attack(event);
                }
            }
        }

    }

    @EventHandler
    public void onPlayerCloseInventory(InventoryCloseEvent event) {

        // Relay inventory close event to all entities
        // that are currently interacting with a player.

        Player player = (Player) event.getPlayer();

        for (OwnedEntity entity : this.repository.all()) {
            if (entity.getRole().interacting(player)) {
                entity.getRole().closeInventory(event);
                break;
            }
        }

    }

    @EventHandler
    public void onPlayerInteractInventory(InventoryClickEvent event) {

        for (OwnedEntity entity : this.repository.all()) {
            entity.getRole().iteractWithInventory(event);
        }

    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        OwnedEntity entity = this.repository.get(
                event.getRightClicked().getUniqueId().toString());

        if (entity != null) {
            entity.getRole().interact(event);
        }

    }

    @EventHandler
    public void onPlayerInteractBlock(PlayerInteractEvent event) {

        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack item = player.getItemInHand();

        if (item != null && item.getType().equals(Material.BLAZE_ROD)) {

            final OwnedEntityPlayerMetadata metadata =
                    new OwnedEntityPlayerMetadata(player);

            OwnedEntity entity = metadata.getSelectedEntity();
            if (entity != null) {
                entity.getRole().interactWithBlock(event);
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

        // Create entity NPC repository and create the items and recipes
        // necessary to control them.

        this.repository = new OwnedEntityRepository(this);
        this.factory = new OwnedEntityFactory();
        this.creatorItemFactory = new OwnedEntityItemFactory();

        // Register companion recipes.

        for (EntityType entityType : OwnedEntityFactory.getValidEntityTypes()) {
            final Recipe recipe = this.creatorItemFactory.getRecipe(entityType);
            if (recipe != null) {
                this.getServer().addRecipe(recipe);
            }
        }

        // Simple check to ensure database exists, and if not
        // creates the database(s) necessary.

        try {
            this.getDatabase().find(OwnedEntityRecord.class).findRowCount();
        } catch (PersistenceException ex) {
            this.installDDL();
        }

        // Register plugin for event handling

        this.getServer().getPluginManager().registerEvents(this, this);

        // Register commands

        this.getCommand("npc").setExecutor(new NpcPluginCommandExecutor(this));

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
                    return "was molested";
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
                    return "took an arrow to the knee";
                case STARVATION:
                    return "starved to death";
                case SUICIDE:
                    return "ended their own life";
                case VOID:
                    return "fell into the void";
                case WITHER:
                    return "withered away";
                case SUFFOCATION:
                    return "suffocated";
            }
        }

        return "vanished";

    }
}
