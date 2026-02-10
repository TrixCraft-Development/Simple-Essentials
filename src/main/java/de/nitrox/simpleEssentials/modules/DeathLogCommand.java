package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;

public class DeathLogCommand implements Listener {
    
    private final SimpleEssentials plugin;
    private File deathsFile;
    private FileConfiguration deathsConfig;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
    private final Map<UUID, Map<String, ItemStack[]>> deathInventories = new HashMap<>();
    
    public DeathLogCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
        setupDeathsFile();
    }
    
    /**
     * Sets up the deaths configuration file
     */
    private void setupDeathsFile() {
        deathsFile = new File(plugin.getDataFolder(), "deaths.yml");
        if (!deathsFile.exists()) {
            try {
                deathsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create deaths.yml: " + e.getMessage());
            }
        }
        deathsConfig = YamlConfiguration.loadConfiguration(deathsFile);
    }
    
    /**
     * Reloads the deaths configuration
     */
    public void reloadDeathsConfig() {
        if (deathsFile == null) {
            deathsFile = new File(plugin.getDataFolder(), "deaths.yml");
        }
        deathsConfig = YamlConfiguration.loadConfiguration(deathsFile);
    }
    
    /**
     * Saves the deaths configuration
     */
    private void saveDeathsConfig() {
        try {
            deathsConfig.save(deathsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save deaths.yml: " + e.getMessage());
        }
    }
    
    /**
     * Registers the deathlog commands
     */
    public void registerDeathLogCommands() {
        // /deathlog [player] command
        new CommandAPICommand("deathlog")
            .withPermission("simpleessentials.deathlog")
            .withArguments(new PlayerArgument("target").setOptional(true))
            .executes((sender, args) -> {
                Player target = null;
                
                if (args.get(0) != null) {
                    target = (Player) args.get(0);
                } else {
                    // No target specified, use sender if player
                    if (sender instanceof Player) {
                        target = (Player) sender;
                    } else {
                        sender.sendMessage(plugin.getMessage("deathlog.player_only"));
                        return;
                    }
                }
                
                openDeathLogGUI((CommandSender) sender, target);
            })
            .register();
    }
    
    /**
     * Handles player death events
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Create death entry
        String deathId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        
        // Store death information
        String playerPath = "deaths." + player.getUniqueId().toString() + "." + deathId;
        deathsConfig.set(playerPath + ".timestamp", timestamp);
        deathsConfig.set(playerPath + ".world", player.getWorld().getName());
        deathsConfig.set(playerPath + ".x", player.getLocation().getX());
        deathsConfig.set(playerPath + ".y", player.getLocation().getY());
        deathsConfig.set(playerPath + ".z", player.getLocation().getZ());
        deathsConfig.set(playerPath + ".death_reason", event.getDeathMessage());
        deathsConfig.set(playerPath + ".gamemode", player.getGameMode().name());
        deathsConfig.set(playerPath + ".server", plugin.getServer().getName());
        deathsConfig.set(playerPath + ".xp_level", player.getLevel());
        deathsConfig.set(playerPath + ".dimension", player.getWorld().getEnvironment().name());
        
        saveDeathsConfig();
        
        // Store player inventory at death
        ItemStack[] inventoryContents = player.getInventory().getContents();
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        ItemStack[] offhandContents = new ItemStack[]{player.getInventory().getItemInOffHand()};
        
        // Combine all items
        ItemStack[] allItems = new ItemStack[inventoryContents.length + armorContents.length + offhandContents.length];
        System.arraycopy(inventoryContents, 0, allItems, 0, inventoryContents.length);
        System.arraycopy(armorContents, 0, allItems, inventoryContents.length, armorContents.length);
        System.arraycopy(offhandContents, 0, allItems, inventoryContents.length + armorContents.length, offhandContents.length);
        
        // Store in memory for GUI access
        deathInventories.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(deathId, allItems);
        
        plugin.getLogger().info("Death logged for " + player.getName() + ": " + event.getDeathMessage());
    }

    /**
     * Handles inventory click events for the death log GUIs
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // Check if this is a death log GUI
        if (title.contains("Death Log")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType() != Material.SKELETON_SKULL) {
                return;
            }

            // Get the death ID from the skull's display name (timestamp)
            String deathTime = clicked.getItemMeta().getDisplayName().replace("§e", "");

            // Find the player and death ID by matching timestamp across all players
            for (UUID playerUuid : deathInventories.keySet()) {
                Map<String, ItemStack[]> playerDeaths = deathInventories.get(playerUuid);
                if (playerDeaths != null) {
                    for (String deathId : playerDeaths.keySet()) {
                        // Get timestamp from deaths config
                        String playerPath = "deaths." + playerUuid.toString() + "." + deathId;
                        long timestamp = deathsConfig.getLong(playerPath + ".timestamp", 0);
                        String formattedTime = dateFormat.format(new Date(timestamp));

                        if (formattedTime.equals(deathTime)) {
                            Player target = Bukkit.getPlayer(playerUuid);
                            return;
                        }
                    }
                }
            }
        }
        
        // Check if this is a death inventory GUI
        if (title.contains("Death Inventory")) {
            // Prevent all item movement in death inventory
            event.setCancelled(true);
        }
    }

    /**
     * Opens the death log GUI for a player
     */
    private void openDeathLogGUI(CommandSender sender, Player target) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("deathlog.player_only"));
            return;
        }

        Player viewer = (Player) sender;
        String playerPath = "deaths." + target.getUniqueId().toString();

        if (!deathsConfig.contains(playerPath)) {
            viewer.sendMessage(plugin.getMessage("deathlog.no_deaths")
                    .replace("{player}", target.getName()));
            return;
        }

        // Get all death entries
        Set<String> deathIds = deathsConfig.getConfigurationSection(playerPath).getKeys(false);
        List<Map.Entry<String, Long>> sortedDeaths = new ArrayList<>();

        for (String deathId : deathIds) {
            long timestamp = deathsConfig.getLong(playerPath + "." + deathId + ".timestamp", 0);
            sortedDeaths.add(new AbstractMap.SimpleEntry<>(deathId, timestamp));
        }

        // Sort by timestamp (newest first)
        sortedDeaths.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        // Create GUI (54 slots - 6 rows, last row for navigation)
        Inventory gui = Bukkit.createInventory(null, 54, "Death Log");

        // Add death skulls (first 5 rows, 45 slots)
        int slot = 0;
        for (int i = 0; i < Math.min(sortedDeaths.size(), 45); i++) {
            Map.Entry<String, Long> entry = sortedDeaths.get(i);
            String deathId = entry.getKey();
            long timestamp = entry.getValue();

            // Create skull item
            ItemStack skull = createDeathSkull(target, deathId, timestamp);
            gui.setItem(slot, skull);
            slot++;
        }

        // Add navigation items (last row)
        addNavigationItems(gui, target, sortedDeaths, 0);

        viewer.openInventory(gui);
    }
    
    /**
     * Creates a skull item for a death entry
     */
    private ItemStack createDeathSkull(Player target, String deathId, long timestamp) {
        ItemStack skull = new ItemStack(Material.SKELETON_SKULL);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        
        if (meta != null) {
            // Set display name with death time
            String deathTime = dateFormat.format(new Date(timestamp));
            meta.setDisplayName(plugin.getMessage("deathlog.skull_name")
                    .replace("{time}", deathTime));
            
            // Create lore with death information
            List<String> lore = new ArrayList<>();
            String playerPath = "deaths." + target.getUniqueId().toString() + "." + deathId;
            
            lore.add(plugin.getMessage("deathlog.lore_world")
                    .replace("{world}", deathsConfig.getString(playerPath + ".world", "Unknown")));
            lore.add(plugin.getMessage("deathlog.lore_coordinates")
                    .replace("{x}", String.format("%.1f", deathsConfig.getDouble(playerPath + ".x")))
                    .replace("{y}", String.format("%.1f", deathsConfig.getDouble(playerPath + ".y")))
                    .replace("{z}", String.format("%.1f", deathsConfig.getDouble(playerPath + ".z"))));
            lore.add(plugin.getMessage("deathlog.lore_reason")
                    .replace("{reason}", deathsConfig.getString(playerPath + ".death_reason", "Unknown")));
            lore.add(plugin.getMessage("deathlog.lore_gamemode")
                    .replace("{gamemode}", deathsConfig.getString(playerPath + ".gamemode", "Unknown")));
            lore.add(plugin.getMessage("deathlog.lore_server")
                    .replace("{server}", deathsConfig.getString(playerPath + ".server", "Unknown")));
            lore.add(plugin.getMessage("deathlog.lore_xp")
                    .replace("{level}", String.valueOf(deathsConfig.getInt(playerPath + ".xp_level", 0))));
            lore.add(plugin.getMessage("deathlog.lore_dimension")
                    .replace("{dimension}", deathsConfig.getString(playerPath + ".dimension", "Unknown")));
            
            meta.setLore(lore);
            skull.setItemMeta(meta);
        }
        
        return skull;
    }
    
    /**
     * Adds navigation items to the GUI
     */
    private void addNavigationItems(Inventory gui, Player target, List<Map.Entry<String, Long>> deaths, int page) {
        // Previous page button (slot 45)
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(plugin.getMessage("deathlog.prev_page"));
                prevButton.setItemMeta(prevMeta);
            }
            gui.setItem(45, prevButton);
        }
        
        // Next page button (slot 53)
        if ((page + 1) * 45 < deaths.size()) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(plugin.getMessage("deathlog.next_page"));
                nextButton.setItemMeta(nextMeta);
            }
            gui.setItem(53, nextButton);
        }
        
        // Info item (slot 49 - center of last row)
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(plugin.getMessage("deathlog.info_title"));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getMessage("deathlog.info_page").replace("{current}", String.valueOf(page + 1))
                    .replace("{total}", String.valueOf((int) Math.ceil(deaths.size() / 45.0))));
            lore.add(plugin.getMessage("deathlog.info_total").replace("{count}", String.valueOf(deaths.size())));
            infoMeta.setLore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        gui.setItem(49, infoItem);
    }

    
    /**
     * Opens the death inventory GUI for an offline player
     */
    private void openDeathInventoryForOfflinePlayer(Player viewer, UUID playerUuid, String deathId) {
        Map<String, ItemStack[]> playerDeaths = deathInventories.get(playerUuid);
        if (playerDeaths == null || !playerDeaths.containsKey(deathId)) {
            viewer.sendMessage(plugin.getMessage("deathlog.inventory_not_found"));
            return;
        }
        
        ItemStack[] deathItems = playerDeaths.get(deathId);
        
        // Create inventory with death items
        Inventory deathInv = Bukkit.createInventory(null, 54, "Death Inventory");
        
        // Place items (first 36 slots for main inventory, next 4 for armor, last for offhand)
        for (int i = 0; i < Math.min(deathItems.length, 54); i++) {
            if (deathItems[i] != null) {
                deathInv.setItem(i, deathItems[i].clone());
            }
        }
        
        viewer.openInventory(deathInv);
    }
    
    /**
     * Gets death inventory for a player (for inventory click handling)
     */
    public Map<UUID, Map<String, ItemStack[]>> getDeathInventories() {
        return deathInventories;
    }
}
