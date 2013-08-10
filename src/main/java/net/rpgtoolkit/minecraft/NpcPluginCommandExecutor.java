/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.rpgtoolkit.minecraft;

import net.rpgtoolkit.minecraft.commands.NpcKillCommand;
import net.rpgtoolkit.minecraft.commands.NpcShopInfiniteInventoryCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Chris Hutchinson
 */
public final class NpcPluginCommandExecutor 
    implements CommandExecutor {
    
    private NpcPlugin plugin;
    
    public NpcPluginCommandExecutor(final NpcPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String name, String[] arguments) {
        
        if (!name.equalsIgnoreCase("npc")) {
            return false;
        }
       
        if (arguments.length < 1) {
            return false;
        }
        
        final String commandName = arguments[0].trim().toLowerCase();
        
        CommandExecutor executor = null;
       
        switch(commandName) {
            case "infinite":
                executor = new NpcShopInfiniteInventoryCommand();
                break;
            case "kill":
                executor = new NpcKillCommand();
                break;
        }
        
        if (executor != null) {
            return executor.onCommand(cs, cmnd, name, arguments);
        }

        return false;
    
    }
    
}
