package de.nitrox.simpleEssentials;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InvseeListener implements Listener {
    
    private final SimpleEssentials plugin;
    
    public InvseeListener(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Check if this is an invsee inventory
        if (!player.hasMetadata("invsee_target")) {
            return;
        }
        
        try {
            // Get the target player's UUID
            org.bukkit.metadata.MetadataValue targetMeta = player.getMetadata("invsee_target").get(0);
            java.util.UUID targetUUID = (java.util.UUID) targetMeta.value();
            
            // Check if the target is online
            boolean isOnline = player.hasMetadata("invsee_online") && 
                (boolean) player.getMetadata("invsee_online").get(0).value();
            
            if (isOnline) {
                // Save changes to online player
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null && target.isOnline()) {
                    saveInventoryChanges(inventory, target);
                    player.sendMessage(plugin.getMessage("invsee.changes_saved").replace("{player}", target.getName()));
                    plugin.debug("Saved inventory changes for " + target.getName() + " from " + player.getName());
                }
            }
            
            // Clear metadata
            player.removeMetadata("invsee_target", plugin);
            player.removeMetadata("invsee_online", plugin);
            
        } catch (Exception e) {
            plugin.debug("Error saving invsee changes: " + e.getMessage());
            player.sendMessage(plugin.getMessage("invsee.error_saving"));
        }
    }
    
    /**
     * Saves changes from the viewed inventory back to the target player
     */
    private void saveInventoryChanges(Inventory viewInventory, Player target) {
        PlayerInventory targetInv = target.getInventory();
        
        // Save main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = viewInventory.getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                targetInv.setItem(i, item.clone());
            } else {
                targetInv.setItem(i, null);
            }
        }
        
        // Save armor slots (slots 45-49 in our custom inventory)
        // Helmet -> slot 45
        ItemStack helmet = viewInventory.getItem(45);
        if (helmet != null && helmet.getType() != org.bukkit.Material.AIR) {
            targetInv.setHelmet(helmet.clone());
        } else {
            targetInv.setHelmet(null);
        }
        
        // Chestplate -> slot 46
        ItemStack chestplate = viewInventory.getItem(46);
        if (chestplate != null && chestplate.getType() != org.bukkit.Material.AIR) {
            targetInv.setChestplate(chestplate.clone());
        } else {
            targetInv.setChestplate(null);
        }
        
        // Leggings -> slot 47
        ItemStack leggings = viewInventory.getItem(47);
        if (leggings != null && leggings.getType() != org.bukkit.Material.AIR) {
            targetInv.setLeggings(leggings.clone());
        } else {
            targetInv.setLeggings(null);
        }
        
        // Boots -> slot 48
        ItemStack boots = viewInventory.getItem(48);
        if (boots != null && boots.getType() != org.bukkit.Material.AIR) {
            targetInv.setBoots(boots.clone());
        } else {
            targetInv.setBoots(null);
        }
        
        // Off-hand -> slot 49
        ItemStack offHand = viewInventory.getItem(40);
        if (offHand != null && offHand.getType() != org.bukkit.Material.AIR) {
            targetInv.setItemInOffHand(offHand.clone());
        } else {
            targetInv.setItemInOffHand(null);
        }
        
        // Update the player's inventory
        target.updateInventory();
    }
}
