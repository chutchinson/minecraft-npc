package net.rpgtoolkit.minecraft.commands;

import net.rpgtoolkit.minecraft.OwnedEntity;
import net.rpgtoolkit.minecraft.OwnedEntityMetadata;
import net.rpgtoolkit.minecraft.OwnedEntityRepository;
import net.rpgtoolkit.minecraft.roles.ShopkeeperRole;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopInfiniteInventoryCommand implements CommandExecutor {

    private OwnedEntityRepository repository;
    
    public ShopInfiniteInventoryCommand(OwnedEntityRepository repo) {
        this.repository = repo;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
                    String[] args) {
        
        if (sender instanceof Player && sender.hasPermission("npc.infinite")) {
            
            final Player player = (Player) sender;
            
            if (player.hasMetadata(OwnedEntityMetadata.SELECTED)) {
                
                OwnedEntity npc = this.repository.get(
                        player.getMetadata(OwnedEntityMetadata.SELECTED).get(0).asString());
               
                if (npc != null && npc.getRole() instanceof ShopkeeperRole) {
                    
                    ShopkeeperRole role = (ShopkeeperRole) npc.getRole();
                    
                    if (role.getShop() != null) {
                        role.getShop().setHasFiniteInventory(
                                !role.getShop().hasFiniteInventory());
                        sender.sendMessage(ChatColor.YELLOW + "Infinite inventory: " + 
                                String.valueOf(!role.getShop().hasFiniteInventory()));
                        return true;
                    }
                    
                }
            }

            sender.sendMessage(ChatColor.RED + 
                    "You must select an NPC with a shop setup.");
            
        }
       
        sender.sendMessage(ChatColor.RED +
                "You do not have permission to setup an infinite shop.");

        return false;

    }

}
