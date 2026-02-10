package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class SimpleCommands implements Listener {
    
    private final SimpleEssentials plugin;
    
    public SimpleCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void registerSimpleCommands() {
        // Workbench Command
        new CommandAPICommand("workbench")
                .withAliases("wb", "crafting")
                .withPermission("simpleessentials.workbench")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Workbench command executed by: " + player.getName());
                    
                    Inventory workbench = Bukkit.createInventory(null, InventoryType.WORKBENCH);
                    player.openInventory(workbench);
                    player.sendMessage(plugin.getMessage("simplecommands.workbench_opened"));
                })
                .register();
        
        // Furnace Command
        new CommandAPICommand("furnace")
                .withAliases("f", "smelt")
                .withPermission("simpleessentials.furnace")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Furnace command executed by: " + player.getName());
                    
                    Inventory furnace = Bukkit.createInventory(null, InventoryType.FURNACE);
                    player.openInventory(furnace);
                    player.sendMessage(plugin.getMessage("simplecommands.furnace_opened"));
                })
                .register();
        
        // Anvil Command
        new CommandAPICommand("anvil")
                .withPermission("simpleessentials.anvil")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Anvil command executed by: " + player.getName());
                    
                    Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL);
                    player.openInventory(anvil);
                    player.sendMessage(plugin.getMessage("simplecommands.anvil_opened"));
                })
                .register();
        
        // Enchanting Table Command
        new CommandAPICommand("enchantingtable")
                .withPermission("simpleessentials.enchantingtable")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Enchantingtable command executed by: " + player.getName());
                    
                    Inventory enchanting = Bukkit.createInventory(null, InventoryType.ENCHANTING);
                    player.openInventory(enchanting);
                    player.sendMessage(plugin.getMessage("simplecommands.enchant_opened"));
                })
                .register();
        
        // Brewing Stand Command
        new CommandAPICommand("brewingstand")
                .withAliases("brewing", "brew")
                .withPermission("simpleessentials.brewingstand")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Brewingstand command executed by: " + player.getName());
                    
                    Inventory brewing = Bukkit.createInventory(null, InventoryType.BREWING);
                    player.openInventory(brewing);
                    player.sendMessage(plugin.getMessage("simplecommands.brew_opened"));
                })
                .register();
        
        // Smithing Table Command
        new CommandAPICommand("smithingtable")
                .withAliases("smithing", "smith")
                .withPermission("simpleessentials.smithingtable")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Smithingtable command executed by: " + player.getName());
                    
                    Inventory smithing = Bukkit.createInventory(null, InventoryType.SMITHING);
                    player.openInventory(smithing);
                    player.sendMessage(plugin.getMessage("simplecommands.smith_opened"));
                })
                .register();
        
        // Grindstone Command
        new CommandAPICommand("grind")
                .withAliases("grindstone")
                .withPermission("simpleessentials.grind")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Grind command executed by: " + player.getName());
                    
                    Inventory grindstone = Bukkit.createInventory(null, InventoryType.GRINDSTONE);
                    player.openInventory(grindstone);
                    player.sendMessage(plugin.getMessage("simplecommands.grind_opened"));
                })
                .register();
        
        // Stonecutter Command
        new CommandAPICommand("stonecutter")
                .withAliases("sc", "stone")
                .withPermission("simpleessentials.stonecutter")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Stonecutter command executed by: " + player.getName());
                    
                    Inventory stonecutter = Bukkit.createInventory(null, InventoryType.STONECUTTER);
                    player.openInventory(stonecutter);
                    player.sendMessage(plugin.getMessage("simplecommands.stonecutter_opened"));
                })
                .register();
        
        // Cartography Table Command
        new CommandAPICommand("cartography")
                .withAliases("cartographytable", "carto", "map")
                .withPermission("simpleessentials.cartography")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Cartography command executed by: " + player.getName());
                    
                    Inventory cartography = Bukkit.createInventory(null, InventoryType.CARTOGRAPHY);
                    player.openInventory(cartography);
                    player.sendMessage(plugin.getMessage("simplecommands.cartography_opened"));
                })
                .register();
        
        // Loom Command
        new CommandAPICommand("loom")
                .withAliases("banner")
                .withPermission("simpleessentials.loom")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Loom command executed by: " + player.getName());
                    
                    Inventory loom = Bukkit.createInventory(null, InventoryType.LOOM);
                    player.openInventory(loom);
                    player.sendMessage(plugin.getMessage("simplecommands.loom_opened"));
                })
                .register();
        
        // Default EnderChest Command (vanilla enderchest)
        new CommandAPICommand("enderchest")
                .withPermission("simpleessentials.enderchest")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    plugin.debug("Default EnderChest command executed by: " + player.getName());
                    
                    // Open the player's default vanilla enderchest
                    player.openInventory(player.getEnderChest());
                    player.sendMessage(plugin.getMessage("simplecommands.enderchest_opened"));
                })
                .register();
    }
    
    /**
     * Event handler for inventory close events (for future enhancements)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Log when players close utility inventories (for debugging)
        if (inventory.getType() != InventoryType.PLAYER && 
            inventory.getType() != InventoryType.CHEST && 
            inventory.getType() != InventoryType.ENDER_CHEST) {
            
            plugin.debug("Player " + player.getName() + " closed " + inventory.getType() + " inventory");
        }
    }
}
