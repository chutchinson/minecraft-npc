/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import net.rpgtoolkit.minecraft.util.SimpleMetadata;
import org.bukkit.entity.Player;

/**
 *
 * @author Chris
 */
public final class OwnedEntityPlayerMetadata {
 
    public enum Metadata {
                
        SELECTION("npc.selected");
        
        private final String key;
                
        Metadata(String key) {
            this.key = key;
        }
               
        public String key() {
            return this.key;
        }
        
        @Override
        public String toString() {
            return this.key();
        }
        
    }
            
    private SimpleMetadata<Metadata> metadata;
    
    public OwnedEntityPlayerMetadata(Player player) {
        this.metadata = new SimpleMetadata(player,
                NpcPlugin.INSTANCE);
    }
    
    public void setSelection(String id) {
        this.metadata.set(Metadata.SELECTION, id);
    }
    
    public String getSelection() {
        return this.metadata.get(Metadata.SELECTION);
    }
    
    public OwnedEntity<?> getSelectedEntity() {
        final OwnedEntityRepository repository = 
                NpcPlugin.INSTANCE.getRepository();
        
        if (repository != null) {
            String selection = this.metadata.get(Metadata.SELECTION);
            if (selection != null) {
                return repository.get(selection);
            }
        }
        
        return null; 
    }
    
}
