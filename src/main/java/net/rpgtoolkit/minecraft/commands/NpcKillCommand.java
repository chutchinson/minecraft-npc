/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft.commands;

import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityPlayerMetadata;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Chris
 */
public class NpcKillCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
    
        if (cs instanceof Player) {
            
            final Player player = (Player) cs;
            final OwnedEntityPlayerMetadata metadata = 
                    new OwnedEntityPlayerMetadata(player);
            
            OwnedEntity entity = metadata.getSelectedEntity();
            if (entity != null) {
                entity.getEntity().damage(entity.getEntity().getHealth() + 1);
                return true;
            }

        }
        
        return false;
        
    
    }
    
}
