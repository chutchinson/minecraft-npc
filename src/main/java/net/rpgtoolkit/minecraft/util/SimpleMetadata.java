/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft.util;

import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.Plugin;

/**
 * Simplified metadata container that only stores and 
 * reads metadata as Strings for a single plugin, and
 * only stores fixed metadata values.
 */
public class SimpleMetadata<T> {
    
    private Metadatable metadata;
    private Plugin plugin;
    
    public SimpleMetadata(Metadatable metadata, Plugin plugin) {
        if (metadata == null || plugin == null) {
            throw new NullPointerException();
        }
        this.metadata = metadata;
        this.plugin = plugin;
    }
    
    public String get(T key) {
        return this.get(key.toString());
    }
    
    public String get(String key) {
        if (this.metadata.hasMetadata(key)) {
            return this.metadata.getMetadata(key).get(0).asString();
        }
        return null;
    }
    
    public void set(T key, String value) {
        this.set(key.toString(), value);
    }
    
    public void set(String key, String value) {
        if (value != null) {
            this.metadata.setMetadata(key,
                    new FixedMetadataValue(this.plugin, value));
        }
        else {
            if (this.metadata.hasMetadata(key)) {
                this.metadata.removeMetadata(key, this.plugin);
            }
        }
    }
    
}
