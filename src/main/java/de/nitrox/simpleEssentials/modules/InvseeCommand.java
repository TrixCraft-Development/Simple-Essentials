package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class InvseeCommand implements Listener {
    
    private final SimpleEssentials plugin;
    
    public InvseeCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void registerInvseeCommands() {
        new CommandAPICommand("invsee")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.invsee")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("invsee.player_only"));
                        return;
                    }
                    
                    String playerName = (String) args.get("player");
                    Player viewer = (Player) sender;
                    
                    plugin.debug("Invsee command executed: target=" + playerName + ", viewer=" + viewer.getName());
                    
                    // Handle online player
                    Player target = Bukkit.getPlayer(playerName);
                    if (target != null && target.isOnline()) {
                        openOnlineInventory(viewer, target, playerName);
                        return;
                    }
                    
                    // Handle offline player
                    OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(playerName);
                    if (offlineTarget.hasPlayedBefore()) {
                        openOfflineInventory(viewer, offlineTarget, playerName);
                    } else {
                        viewer.sendMessage(plugin.getMessage("player_not_found").replace("{player}", playerName));
                    }
                })
                .register();
    }
    
    /**
     * Opens the inventory of an online player
     */
    private void openOnlineInventory(Player viewer, Player target, String targetName) {
        // Check permissions
        if (!viewer.hasPermission("simpleessentials.invsee.bypass") && 
            target.hasPermission("simpleessentials.invsee.exempt")) {
            viewer.sendMessage(plugin.getMessage("invsee.cannot_view").replace("{player}", targetName));
            return;
        }
        
        // Create a custom inventory that mirrors the target's inventory
        PlayerInventory targetInv = target.getInventory();
        Inventory invView = Bukkit.createInventory(null, 54, "§8" + targetName + "'s Inventory");
        
        // Copy main inventory (slots 0-35)
        for (int i = 0; i < 36; i++) {
            ItemStack item = targetInv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                invView.setItem(i, item.clone());
            }
        }
        
        // Copy armor slots to editable positions (slots 36-39)
        // Helmet -> slot 36
        ItemStack helmet = targetInv.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            invView.setItem(45, helmet.clone());
        }
        
        // Chestplate -> slot 37
        ItemStack chestplate = targetInv.getChestplate();
        if (chestplate != null && chestplate.getType() != Material.AIR) {
            invView.setItem(46, chestplate.clone());
        }
        
        // Leggings -> slot 38
        ItemStack leggings = targetInv.getLeggings();
        if (leggings != null && leggings.getType() != Material.AIR) {
            invView.setItem(47, leggings.clone());
        }
        
        // Boots -> slot 39
        ItemStack boots = targetInv.getBoots();
        if (boots != null && boots.getType() != Material.AIR) {
            invView.setItem(48, boots.clone());
        }
        
        // Off-hand -> slot 40
        ItemStack offHand = targetInv.getItemInOffHand();
        if (offHand != null && offHand.getType() != Material.AIR) {
            invView.setItem(49, offHand.clone());
        }
        
        // Add separator items for visual clarity
        addInventorySeparators(invView);
        
        // Store the target player for saving changes
        viewer.setMetadata("invsee_target", new org.bukkit.metadata.FixedMetadataValue(plugin, target.getUniqueId()));
        viewer.setMetadata("invsee_online", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        
        viewer.openInventory(invView);
        viewer.sendMessage(plugin.getMessage("invsee.opened_online").replace("{player}", targetName));
        viewer.sendMessage(plugin.getMessage("invsee.note_online"));
        
        plugin.debug("Opened online inventory for " + targetName + " viewed by " + viewer.getName());
    }
    
    /**
     * Opens the inventory of an offline player (read-only)
     */
    private void openOfflineInventory(Player viewer, OfflinePlayer offlineTarget, String targetName) {
        // Create a custom inventory for offline player
        Inventory invView = Bukkit.createInventory(null, 54, "§6" + targetName + "'s Inventory (Offline)");
        
        // Try to load the player's data
        try {
            UUID targetUUID = offlineTarget.getUniqueId();
            org.bukkit.World world = viewer.getWorld();
            
            // Create a temporary player to load inventory data
            Player tempPlayer = world.getMetadata("invsee_temp_player").isEmpty() ? 
                null : (Player) world.getMetadata("invsee_temp_player").get(0).value();
            
            if (tempPlayer == null) {
                // This is a simplified approach - in production, you'd want to use
                // a proper data loading method or a plugin like ProtocolLib
                viewer.sendMessage(plugin.getMessage("invsee.offline_limitation"));
                viewer.sendMessage(plugin.getMessage("invsee.offline_tip1"));
                viewer.sendMessage(plugin.getMessage("invsee.offline_tip2"));
                return;
            }
            
            // For now, show an empty inventory with a message
            ItemStack infoItem = new org.bukkit.inventory.ItemStack(Material.BARRIER);
            org.bukkit.inventory.meta.ItemMeta meta = infoItem.getItemMeta();
            meta.setDisplayName(plugin.getMessage("invsee.offline_info"));
            meta.setLore(plugin.getConfig().getStringList("messages.invsee.offline_lore"));
            infoItem.setItemMeta(meta);
            
            invView.setItem(22, infoItem); // Center slot
            
        } catch (Exception e) {
            plugin.debug("Error loading offline inventory: " + e.getMessage());
            viewer.sendMessage(plugin.getMessage("invsee.error_loading"));
            viewer.sendMessage(plugin.getMessage("invsee.error_try_online"));
            return;
        }
        
        // Add separator items
        addInventorySeparators(invView);
        
        viewer.openInventory(invView);
        viewer.sendMessage(plugin.getMessage("invsee.opened_offline").replace("{player}", targetName));
        viewer.sendMessage(plugin.getMessage("invsee.note_offline"));
        
        plugin.debug("Opened offline inventory for " + targetName + " viewed by " + viewer.getName());
    }
    
    /**
     * Adds visual separators to distinguish inventory sections
     */
    private void addInventorySeparators(Inventory inventory) {
        // Create separator items
        ItemStack separator = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta separatorMeta = separator.getItemMeta();
        separatorMeta.setDisplayName("§8");
        separator.setItemMeta(separatorMeta);
        
        // Add only one separator line at row 5 (slots 41-44) to separate main inventory from armor/offhand
        inventory.setItem(36, separator.clone());
        inventory.setItem(37, separator.clone());
        inventory.setItem(38, separator.clone());
        inventory.setItem(39, separator.clone());
        inventory.setItem(40, separator.clone());
        inventory.setItem(41, separator.clone());
        inventory.setItem(42, separator.clone());
        inventory.setItem(43, separator.clone());
        inventory.setItem(44, separator.clone());
    }
    
    /**
     * Prevents clicking on protected slots (glass panes and labels only)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if this is an invsee inventory
        if (!player.hasMetadata("invsee_target")) {
            return;
        }
        
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Prevent clicking on separator items (slots 36-44)
        if (slot >= 36 && slot <= 44) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent clicking on separator items even if they're in other slots (double-click stacking)
        if (clickedItem != null && clickedItem.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            org.bukkit.inventory.meta.ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§8")) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    /**
     * Handles inventory close to restore original armor and prevent separator items from being returned
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Check if this is an invsee inventory
        if (!player.hasMetadata("invsee_target")) {
            return;
        }
        
        // Clear any separator items from player's inventory (delayed to prevent conflicts)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove separator items from armor slots
            if (player.getInventory().getHelmet() != null && 
                player.getInventory().getHelmet().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().setHelmet(null);
            }
            if (player.getInventory().getChestplate() != null && 
                player.getInventory().getChestplate().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().setChestplate(null);
            }
            if (player.getInventory().getLeggings() != null && 
                player.getInventory().getLeggings().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().setLeggings(null);
            }
            if (player.getInventory().getBoots() != null && 
                player.getInventory().getBoots().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().setBoots(null);
            }
            if (player.getInventory().getItemInOffHand() != null && 
                player.getInventory().getItemInOffHand().getType() == Material.GRAY_STAINED_GLASS_PANE) {
                player.getInventory().setItemInOffHand(null);
            }
            
            // Remove separator items from main inventory
            for (int i = 0; i < 36; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == Material.GRAY_STAINED_GLASS_PANE && item.getItemMeta().getDisplayName().equals("§8")) {
                    player.getInventory().setItem(i, null);
                }
            }
            
            // Clear metadata
            player.removeMetadata("invsee_target", plugin);
            player.removeMetadata("invsee_online", plugin);
            
            plugin.debug("Cleared invsee separator items from " + player.getName());
        }, 1L);
    }
}
