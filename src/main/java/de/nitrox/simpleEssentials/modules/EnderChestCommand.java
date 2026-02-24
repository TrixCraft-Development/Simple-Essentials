package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnderChestCommand implements Listener {
    
    private final SimpleEssentials plugin;
    private final Map<String, Map<UUID, Inventory>> enderChests = new HashMap<>();
    
    public EnderChestCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void registerEnderChestCommands() {
        // Main EC Command (with optional ID)
        new CommandAPICommand("ec")
                .withArguments(new StringArgument("id").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return available IDs based on player permissions
                    if (info.sender() instanceof Player) {
                        Player player = (Player) info.sender();
                        return getAvailableIds(player).toArray(new String[0]);
                    }
                    return new String[0];
                })).setOptional(true))
                .withPermission("simpleessentials.custom.enderchest")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("enderchest.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    String id = args.getOptional("id").map(Object::toString).orElse("1"); // Default to "1" if no ID provided
                    
                    plugin.debug("EC command executed: player=" + player.getName() + ", id=" + id);
                    
                    // Validate ID
                    if (!isValidId(id)) {
                        player.sendMessage(plugin.getMessage("enderchest.invalid_id")
                                .replace("{id}", id)
                                .replace("{max}", String.valueOf(getMaxId())));
                        return;
                    }
                    
                    // Check permission
                    String permission = "simpleessentials.custom.enderchest." + id;
                    if (!player.hasPermission(permission)) {
                        player.sendMessage(plugin.getMessage("enderchest.no_permission")
                                .replace("{id}", id));
                        return;
                    }
                    
                    // Open enderchest
                    openEnderChest(player, id);
                })
                .register();
    }
    
    /**
     * Opens the enderchest for a player
     */
    private void openEnderChest(Player player, String id) {
        UUID playerUUID = player.getUniqueId();
        
        // Get or create the enderchest inventory for this ID
        Map<UUID, Inventory> idChests = enderChests.computeIfAbsent(id, k -> new HashMap<>());
        
        Inventory inventory = idChests.get(playerUUID);
        if (inventory == null) {
            // Create new inventory
            int slots = getSlotsForId(id);
            inventory = Bukkit.createInventory(null, slots, 
                    "EnderChest " + id);
            
            idChests.put(playerUUID, inventory);
        }
        
        // Store current inventory for saving on close
        player.setMetadata("ec_id", new org.bukkit.metadata.FixedMetadataValue(plugin, id));
        player.setMetadata("ec_inventory", new org.bukkit.metadata.FixedMetadataValue(plugin, inventory));
        
        player.openInventory(inventory);
        player.sendMessage(plugin.getMessage("enderchest.opened")
                .replace("{id}", id)
                .replace("{slots}", String.valueOf(inventory.getSize())));
    }
    
    /**
     * Gets available IDs for a player based on permissions
     */
    private java.util.List<String> getAvailableIds(Player player) {
        java.util.List<String> availableIds = new java.util.ArrayList<>();
        int maxId = getMaxId();
        
        for (int i = 1; i <= maxId; i++) {
            String id = String.valueOf(i);
            if (player.hasPermission("simpleessentials.custom.enderchest." + id)) {
                availableIds.add(id);
            }
        }
        
        return availableIds;
    }
    
    /**
     * Validates if an ID is within the allowed range
     */
    private boolean isValidId(String id) {
        try {
            int idNum = Integer.parseInt(id);
            return idNum >= 1 && idNum <= getMaxId();
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Gets the maximum ID from config
     */
    private int getMaxId() {
        return plugin.getConfig().getInt("enderchest.max_ids", 5);
    }
    
    /**
     * Gets the number of slots for a specific ID
     */
    private int getSlotsForId(String id) {
        // Check if specific ID has custom slots
        String path = "enderchest.slots_per_id." + id;
        if (plugin.getConfig().contains(path)) {
            return plugin.getConfig().getInt(path);
        }
        
        // Use default slots
        return plugin.getConfig().getInt("enderchest.default_slots", 27);
    }
    
    /**
     * Gets the nearest valid inventory size
     */
    private int getNearestInventorySize(int slots) {
        int[] validSizes = {9, 18, 27, 36, 45, 54};
        
        for (int size : validSizes) {
            if (slots <= size) {
                return size;
            }
        }
        
        return 54; // Maximum size
    }
    
    /**
     * Event handler for saving enderchest on close
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        
        // Check if this is an enderchest inventory
        if (!player.hasMetadata("ec_inventory")) {
            return;
        }
        
        try {
            String id = player.getMetadata("ec_id").get(0).asString();
            
            plugin.debug("Saving EC " + id + " for " + player.getName());
            
            // Clear metadata
            player.removeMetadata("ec_id", plugin);
            player.removeMetadata("ec_inventory", plugin);
            
        } catch (Exception e) {
            plugin.debug("Error saving enderchest: " + e.getMessage());
        }
    }

    /**
     * Gets a player's enderchest inventory (for other plugins to access)
     */
    public Inventory getPlayerEnderChest(Player player, String id) {
        if (!isValidId(id)) {
            return null;
        }
        
        Map<UUID, Inventory> idChests = enderChests.get(id);
        if (idChests == null) {
            return null;
        }
        
        return idChests.get(player.getUniqueId());
    }
}
