package net.rpgtoolkit.minecraft;

import de.ntcomputer.minecraft.controllablemobs.api.ControllableMob;
import de.ntcomputer.minecraft.controllablemobs.api.ControllableMobs;
import de.ntcomputer.minecraft.controllablemobs.api.ai.behaviors.AILookAtEntity;
import org.bukkit.ChatColor;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class OwnedEntity<T extends LivingEntity> {

    private String id;
    private String name;
    private String owner;
    private LivingEntity entity;
    private ControllableMob<T> controller;
    private OwnedEntityRole role;
    private boolean selected;

    public OwnedEntity(LivingEntity entity, String owner) {

        if (entity == null) {
            throw new NullPointerException("entity");
        }

        if (owner == null) {
            throw new NullPointerException("owner");
        }

        this.id = entity.getUniqueId().toString();
        this.setOwner(owner);

        this.update(entity);

    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.entity.getCustomName();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
        this.entity.setCustomName(this.getName());
    }

    public OwnedEntityRole getRole() {
        return this.role;
    }

    public void setRole(OwnedEntityRole role) {
        if (role == null) {
            throw new NullPointerException();
        }
        this.role = role;
        // this.entity.setProfession(this.role.getProfession());
    }

    public boolean selected() {
        return this.selected;
        // return this.entity.getProfession() == Profession.PRIEST;
    }

    public void select(Player player, boolean value) {

        if (value) {
            this.selected = true;
            this.entity.setCustomName(ChatColor.LIGHT_PURPLE + this.getName());
            if (player != null) {
                player.setMetadata("npc.selected",
                        new FixedMetadataValue(NpcPlugin.INSTANCE, this.id));
            }
        } else {
            this.selected = false;
            this.entity.setCustomName(this.getName());
            if (player != null) {
                player.removeMetadata("npc.selected", NpcPlugin.INSTANCE);
            }
        }

    }

    public final LivingEntity getEntity() {
        return this.entity;
    }

    public final String getOwner() {
        return this.owner;
    }

    public final void setOwner(Player owner) {
        this.owner = owner.getName();
    }

    public final void setOwner(String owner) {
        this.owner = owner;
    }

    public final ControllableMob<T> getController() {
        return this.controller;
    }

    public void remove() {

        if (this.role != null) {
            this.role.dead(null);
        }

        this.entity.remove();

    }

    public void say(Player player, String message) {

        this.say(player, message, 'f');

    }

    public void say(Player player, String message, char color) {

        if (player != null && message != null) {
            player.sendMessage(String.format("\u00a7f<\u00a72%s\u00a7f> \u00a7%s%s",
                    this.getName(), color, message));
        }

    }

    public void set(String key, String value) {

        if (this.role != null) {
            this.role.getMetadata().put(key, value);
            this.role.setDirty(true);
        }

    }

    public void unset(String key) {

        if (this.role != null) {
            if (this.role.getMetadata().containsKey(key)) {
                this.role.getMetadata().remove(key);
            }
        }

    }

    public final void update(LivingEntity entity) {

        this.entity = entity;

        if (this.entity instanceof Ageable) {
            ((Ageable) this.entity).setBreed(false);
        }

        this.controller = (ControllableMob<T>) ControllableMobs.getOrAssign(this.entity);
        this.controller.getAI().clear();
        this.controller.getAI().addBehavior(
                new AILookAtEntity(EntityType.PLAYER));

        if (this.getRole() != null) {
            this.getRole().update();
        }

    }
}
