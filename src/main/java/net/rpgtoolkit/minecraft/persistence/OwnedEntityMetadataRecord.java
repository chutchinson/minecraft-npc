package net.rpgtoolkit.minecraft.persistence;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "OwnedVillagersMetadata")
public class OwnedEntityMetadataRecord implements Serializable {

    @Id
    @GeneratedValue
    private int id;
    @NotNull
    @NotEmpty
    private String reference;
    @NotNull
    @NotEmpty
    private String role;
    @NotNull
    @NotEmpty
    private String key;
    @NotNull
    private String value;

    public int getId() {
        return this.id;
    }

    public void setId(int value) {
        this.id = value;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String value) {
        this.role = value;
    }

    public String getReference() {
        return this.reference;
    }

    public void setReference(String value) {
        this.reference = value;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String value) {
        this.key = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
