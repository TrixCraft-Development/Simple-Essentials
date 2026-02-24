package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class WarpCommands {
    
    private final SimpleEssentials plugin;
    private File warpFile;
    private FileConfiguration warpConfig;
    
    public WarpCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
        setupWarpFile();
    }
    
    /**
     * Sets up the warps.yml file
     */
    private void setupWarpFile() {
        warpFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpFile.exists()) {
            plugin.saveResource("warps.yml", false);
        }
        warpConfig = YamlConfiguration.loadConfiguration(warpFile);
    }
    
    /**
     * Saves the warps.yml file
     */
    private void saveWarpsFile() {
        try {
            warpConfig.save(warpFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }
    
    /**
     * Reloads the warps.yml file
     */
    private void reloadWarpsFile() {
        warpConfig = YamlConfiguration.loadConfiguration(warpFile);
    }
    
    /**
     * Registers all warp commands
     */
    public void registerWarpCommands() {
        
        // SetWarp Command
        new CommandAPICommand("setwarp")
                .withArguments(new StringArgument("name"))
                .withPermission("simpleessentials.setwarp")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("warp.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    String warpName = (String) args.get("name");
                    
                    plugin.debug("SetWarp command executed: warp=" + warpName + ", player=" + player.getName());
                    
                    // Validate warp name
                    if (warpName.isEmpty()) {
                        player.sendMessage(plugin.getMessage("warp.set.name_empty"));
                        return;
                    }
                    
                    if (warpName.length() > 20) {
                        player.sendMessage(plugin.getMessage("warp.set.name_too_long"));
                        return;
                    }
                    
                    if (warpName.contains(" ") || warpName.contains(".") || warpName.contains("/")) {
                        player.sendMessage(plugin.getMessage("warp.set.invalid_characters"));
                        return;
                    }
                    
                    // Check if warp already exists
                    if (warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.set.already_exists").replace("{warp}", warpName));
                        return;
                    }
                    
                    // Create warp
                    Location loc = player.getLocation();
                    String path = "warps." + warpName;
                    
                    warpConfig.set(path + ".world", loc.getWorld().getName());
                    warpConfig.set(path + ".x", loc.getX());
                    warpConfig.set(path + ".y", loc.getY());
                    warpConfig.set(path + ".z", loc.getZ());
                    warpConfig.set(path + ".yaw", loc.getYaw());
                    warpConfig.set(path + ".pitch", loc.getPitch());
                    warpConfig.set(path + ".created_by", player.getName());
                    warpConfig.set(path + ".created_at", System.currentTimeMillis());
                    
                    saveWarpsFile();
                    
                    player.sendMessage(plugin.getMessage("warp.set.success")
                            .replace("{warp}", warpName)
                            .replace("{world}", loc.getWorld().getName())
                            .replace("{x}", String.valueOf(loc.getBlockX()))
                            .replace("{y}", String.valueOf(loc.getBlockY()))
                            .replace("{z}", String.valueOf(loc.getBlockZ())));
                    
                    plugin.debug("Warp created: " + warpName + " by " + player.getName());
                })
                .register();
        
        // DelWarp Command
        new CommandAPICommand("delwarp")
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return existing warps for tab completion
                    reloadWarpsFile();
                    org.bukkit.configuration.ConfigurationSection warpsSection = warpConfig.getConfigurationSection("warps");
                    if (warpsSection != null) {
                        Set<String> warps = warpsSection.getKeys(false);
                        return warps.toArray(new String[0]);
                    }
                    return new String[0];
                })))
                .withPermission("simpleessentials.delwarp")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("warp.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    String warpName = (String) args.get("name");
                    
                    plugin.debug("DelWarp command executed: warp=" + warpName + ", player=" + player.getName());
                    
                    // Check if warp exists
                    if (!warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.delete.not_found").replace("{warp}", warpName));
                        return;
                    }
                    
                    // Delete warp
                    warpConfig.set("warps." + warpName, null);
                    saveWarpsFile();
                    
                    player.sendMessage(plugin.getMessage("warp.delete.success").replace("{warp}", warpName));
                    
                    plugin.debug("Warp deleted: " + warpName + " by " + player.getName());
                })
                .register();
        
        // Warp Command
        new CommandAPICommand("warp")
                .withArguments(new StringArgument("name").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    reloadWarpsFile();
                    org.bukkit.configuration.ConfigurationSection warpsSection = warpConfig.getConfigurationSection("warps");
                    if (warpsSection != null) {
                        Set<String> warps = warpsSection.getKeys(false);
                        return warps.toArray(new String[0]);
                    }
                    return new String[0];
                })))
                .withPermission("simpleessentials.warp")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("warp.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    String warpName = (String) args.get("name");
                    
                    plugin.debug("Warp command executed: warp=" + warpName + ", player=" + player.getName());
                    
                    // Check if warp exists
                    if (!warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.not_found").replace("{warp}", warpName));
                        return;
                    }
                    
                    // Check specific warp permission
                    String warpPermission = "simpleessentials.warp." + warpName.toLowerCase();
                    if (!player.hasPermission(warpPermission)) {
                        player.sendMessage(plugin.getMessage("warp.no_permission").replace("{warp}", warpName));
                        return;
                    }
                    
                    // Get warp location
                    String path = "warps." + warpName;
                    String worldName = warpConfig.getString(path + ".world");
                    double x = warpConfig.getDouble(path + ".x");
                    double y = warpConfig.getDouble(path + ".y");
                    double z = warpConfig.getDouble(path + ".z");
                    float yaw = (float) warpConfig.getDouble(path + ".yaw");
                    float pitch = (float) warpConfig.getDouble(path + ".pitch");
                    
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        player.sendMessage(plugin.getMessage("warp.world_not_found").replace("{world}", worldName));
                        return;
                    }
                    
                    // Teleport player
                    Location warpLoc = new Location(world, x, y, z, yaw, pitch);
                    player.teleport(warpLoc);
                    
                    player.sendMessage(plugin.getMessage("warp.teleport.success")
                            .replace("{warp}", warpName)
                            .replace("{world}", worldName)
                            .replace("{x}", String.valueOf(warpLoc.getBlockX()))
                            .replace("{y}", String.valueOf(warpLoc.getBlockY()))
                            .replace("{z}", String.valueOf(warpLoc.getBlockZ())));
                    
                    plugin.debug("Player " + player.getName() + " teleported to warp: " + warpName);
                })
                .register();
        
        // Warps List Command
        new CommandAPICommand("warps")
                .withPermission("simpleessentials.warps")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("warp.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    
                    plugin.debug("Warps list command executed by: " + player.getName());
                    
                    reloadWarpsFile();
                    org.bukkit.configuration.ConfigurationSection warpsSection = warpConfig.getConfigurationSection("warps");
                    
                    if (warpsSection == null) {
                        player.sendMessage(plugin.getMessage("warp.list.empty"));
                        return;
                    }
                    
                    Set<String> warps = warpsSection.getKeys(false);
                    
                    if (warps.isEmpty()) {
                        player.sendMessage(plugin.getMessage("warp.list.empty"));
                        return;
                    }
                    
                    player.sendMessage(plugin.getMessage("warp.list.header"));
                    
                    int count = 0;
                    for (String warpName : warps) {
                        String warpPermission = "simpleessentials.warp." + warpName.toLowerCase();
                        if (player.hasPermission(warpPermission)) {
                            String path = "warps." + warpName;
                            String worldName = warpConfig.getString(path + ".world");
                            String createdBy = warpConfig.getString(path + ".created_by", "Unknown");

                            sendClickableWarp(player, warpName, worldName, createdBy);
                            count++;
                        }
                    }
                    
                    if (count == 0) {
                        player.sendMessage(plugin.getMessage("warp.list.no_access"));
                    } else {
                        player.sendMessage(plugin.getMessage("warp.list.footer").replace("{count}", String.valueOf(count)));
                    }
                })
                .register();
    }
    
    /**
     * Sends a clickable warp message that teleports the player
     */
    private void sendClickableWarp(Player player, String warpName, String worldName, String createdBy) {
        // Get the config message and parse it properly to preserve colors
        String rawMessage = plugin.getMessage("warp.list.entry")
                .replace("{warp}", "{WARP_PLACEHOLDER}")
                .replace("{world}", worldName)
                .replace("{creator}", createdBy);

        // Split by placeholder and reconstruct with clickable warp
        String[] parts = rawMessage.split("\\{WARP_PLACEHOLDER\\}", 2);
        
        if (parts.length == 2) {
            TextComponent component = new TextComponent();
            
            // Add first part with colors
            TextComponent beforePart = new TextComponent(parts[0]);
            component.addExtra(beforePart);
            
            // Add clickable warp name with white color (from config &f)
            TextComponent warpComponent = new TextComponent(warpName);
            warpComponent.setColor(net.md_5.bungee.api.ChatColor.WHITE);
            warpComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/warp " + warpName));
            warpComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder("§aClick to teleport to " + warpName + "\n§7World: " + worldName + "\n§7Created by: " + createdBy).create()));
            component.addExtra(warpComponent);
            
            // Add second part with colors
            TextComponent afterPart = new TextComponent(parts[1]);
            component.addExtra(afterPart);
            
            player.spigot().sendMessage(component);
        } else {
            // Fallback: send the message without click functionality but with colors
            player.sendMessage(rawMessage.replace("{WARP_PLACEHOLDER}", warpName));
        }
    }
}
