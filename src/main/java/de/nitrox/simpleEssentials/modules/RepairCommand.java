package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class RepairCommand {
    
    private final SimpleEssentials plugin;
    
    public RepairCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void registerRepairCommands() {
        // Main repair command with optional subcommand and optional player target
        new CommandAPICommand("repair")
            .withPermission("simpleessentials.repair")
            .withArguments(new StringArgument("subcommand").setOptional(true))
            .withArguments(new PlayerArgument("target").setOptional(true))
            .executes((sender, args) -> {
                String[] commandArgs = new String[0];
                Player target = null;
                
                if (args.get(0) != null) {
                    commandArgs = new String[]{args.get(0).toString()};
                }
                
                if (args.get(1) != null) {
                    target = (Player) args.get(1);
                }
                
                onCommand(sender, commandArgs, target);
            })
            .register();
    }
    
    public boolean onCommand(CommandSender sender, String[] args, Player target) {
        // Check if sender is a player (unless they have repair.others permission)
        if (!(sender instanceof Player) && target == null) {
            sender.sendMessage(plugin.getMessage("repair.player_only"));
            return true;
        }
        
        Player player = sender instanceof Player ? (Player) sender : null;
        
        // If no target specified, repair self (must be player)
        if (target == null) {
            if (player == null) {
                sender.sendMessage(plugin.getMessage("repair.console_need_target"));
                return true;
            }
            target = player;
        }
        
        // Check if sender is trying to repair someone else
        if (player != null && !player.equals(target)) {
            // Check repair others permission
            if (!player.hasPermission("simpleessentials.repair.others")) {
                sender.sendMessage(plugin.getMessage("repair.no_others_permission"));
                return true;
            }
        } else if (player == null) {
            // Console needs repair.others permission to repair other players
            if (!sender.hasPermission("simpleessentials.repair.others")) {
                sender.sendMessage(plugin.getMessage("repair.no_others_permission"));
                return true;
            }
        }
        
        // Check basic repair permission
        if (!sender.hasPermission("simpleessentials.repair")) {
            sender.sendMessage(plugin.getMessage("repair.no_permission"));
            return true;
        }
        
        // Determine repair mode (default to "all")
        String repairMode = "all";
        if (args.length > 0) {
            repairMode = args[0].toLowerCase();
        }
        
        // Execute repair based on mode
        switch (repairMode) {
            case "all":
            case "inventory":
                repairInventory(sender, target);
                break;
            case "hand":
                repairHand(sender, target);
                break;
            default:
                sender.sendMessage(plugin.getMessage("repair.usage"));
                return true;
        }
        
        return true;
    }
    
    /**
     * Repairs all items in player's inventory
     */
    private void repairInventory(CommandSender sender, Player target) {
        PlayerInventory inventory = target.getInventory();
        int repairedCount = 0;
        
        // Repair main inventory items
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR && isRepairable(item)) {
                repairItem(item);
                inventory.setItem(i, item);
                repairedCount++;
            }
        }
        
        // Repair armor slots
        for (ItemStack armor : inventory.getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR && isRepairable(armor)) {
                repairItem(armor);
                repairedCount++;
            }
        }
        
        // Update armor contents
        inventory.setArmorContents(inventory.getArmorContents());
        
        if (repairedCount > 0) {
            String message;
            if (sender.equals(target)) {
                message = plugin.getMessage("repair.inventory_success")
                        .replace("{count}", String.valueOf(repairedCount));
            } else {
                message = plugin.getMessage("repair.inventory_success_other")
                        .replace("{count}", String.valueOf(repairedCount))
                        .replace("{player}", target.getName());
            }
            sender.sendMessage(message);
            
            // Notify target if someone else repaired their items
            if (!sender.equals(target)) {
                target.sendMessage(plugin.getMessage("repair.inventory_notified")
                        .replace("{count}", String.valueOf(repairedCount))
                        .replace("{sender}", sender.getName()));
            }
        } else {
            sender.sendMessage(plugin.getMessage("repair.no_repairable_items"));
        }
    }
    
    /**
     * Repairs item in player's hand
     */
    private void repairHand(CommandSender sender, Player target) {
        ItemStack item = target.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(plugin.getMessage("repair.no_item_in_hand"));
            return;
        }
        
        if (!isRepairable(item)) {
            sender.sendMessage(plugin.getMessage("repair.item_not_repairable"));
            return;
        }
        
        repairItem(item);
        target.getInventory().setItemInMainHand(item);
        
        String itemName = item.getType().name().toLowerCase().replace("_", " ");
        String message;
        if (sender.equals(target)) {
            message = plugin.getMessage("repair.hand_success")
                    .replace("{item}", itemName);
        } else {
            message = plugin.getMessage("repair.hand_success_other")
                    .replace("{item}", itemName)
                    .replace("{player}", target.getName());
        }
        sender.sendMessage(message);
        
        // Notify target if someone else repaired their item
        if (!sender.equals(target)) {
            target.sendMessage(plugin.getMessage("repair.hand_notified")
                    .replace("{item}", itemName)
                    .replace("{sender}", sender.getName()));
        }
    }
    
    /**
     * Checks if an item is repairable (has durability)
     */
    private boolean isRepairable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            return damageable.hasDamage() && damageable.getDamage() > 0;
        }
        
        return false;
    }
    
    /**
     * Repairs an item by setting its durability to maximum
     */
    private void repairItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            damageable.setDamage(0);
            item.setItemMeta(meta);
        }
    }
}
