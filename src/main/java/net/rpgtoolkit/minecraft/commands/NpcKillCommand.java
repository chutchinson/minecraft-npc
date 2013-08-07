/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft.commands;

import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityMetadata;
import net.rpgtoolkit.minecraft.OwnedEntityRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Chris
 */
public class NpcKillCommand implements CommandExecutor {

    private OwnedEntityRepository repository;
    
    public NpcKillCommand(OwnedEntityRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
    
        if (cs instanceof Player) {
            
            final Player player = (Player) cs;
            
            if (player.hasMetadata(OwnedEntityMetadata.SELECTED)) {
                OwnedEntity villager = this.repository.get(
                        player.getMetadata(OwnedEntityMetadata.SELECTED).get(0).asString());
                if (villager != null) {
                    villager.getEntity().damage(villager.getEntity().getHealth() + 1);
                    return true;
                }
            }
            
        }
        
        return false;
        
    
    }
    
}
