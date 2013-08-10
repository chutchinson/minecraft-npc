package net.rpgtoolkit.minecraft.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.io.Serializable;

import net.rpgtoolkit.minecraft.OwnedEntity;

@Entity()
@Table(name = "OwnedVillagers")
public class OwnedEntityRecord implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @NotNull
    @NotEmpty
    private String reference;
    @NotNull
    @NotEmpty
    private String name;
    @NotNull
    @NotEmpty
    private String owner;
    @NotNull
    @NotEmpty
    private String role;

    public OwnedEntityRecord() {
    }

    public OwnedEntityRecord(OwnedEntity villager) {
        if (villager == null) {
            throw new NullPointerException();
        }
        this.setReference(villager.getId());
        this.setName(villager.getName());
        this.setOwner(villager.getOwner());
        this.setRole(villager.getRole().getTitle());
    }

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
    }

    public String getReference() {
        return this.reference;
    }

    public void setReference(String value) {
        this.reference = value;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String value) {
        this.owner = value;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String value) {
        this.role = value;
    }
}
