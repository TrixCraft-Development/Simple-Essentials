package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
    
    private void setupWarpFile() {
        warpFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpFile.exists()) {
            plugin.saveResource("warps.yml", false);
        }
        warpConfig = YamlConfiguration.loadConfiguration(warpFile);
    }
    
    private void saveWarpsFile() {
        try {
            warpConfig.save(warpFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save warps.yml: " + e.getMessage());
        }
    }
    
    private void reloadWarpsFile() {
        warpConfig = YamlConfiguration.loadConfiguration(warpFile);
    }
    
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
                    
                    if (warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.set.already_exists").replace("{warp}", warpName));
                        return;
                    }
                    
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
                    
                    if (!warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.delete.not_found").replace("{warp}", warpName));
                        return;
                    }
                    
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
                    
                    if (!warpConfig.contains("warps." + warpName)) {
                        player.sendMessage(plugin.getMessage("warp.not_found").replace("{warp}", warpName));
                        return;
                    }
                    
                    String warpPermission = "simpleessentials.warp." + warpName.toLowerCase();
                    if (!player.hasPermission(warpPermission)) {
                        player.sendMessage(plugin.getMessage("warp.no_permission").replace("{warp}", warpName));
                        return;
                    }
                    
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
                    
                    Location warpLoc = new Location(world, x, y, z, yaw, pitch);
                    if (player.teleport(warpLoc)) {
                        player.sendMessage(plugin.getMessage("warp.teleport.success")
                                .replace("{warp}", warpName)
                                .replace("{world}", worldName)
                                .replace("{x}", String.valueOf(warpLoc.getBlockX()))
                                .replace("{y}", String.valueOf(warpLoc.getBlockY()))
                                .replace("{z}", String.valueOf(warpLoc.getBlockZ())));
                        plugin.debug("Player " + player.getName() + " teleported to warp: " + warpName);
                    } else {
                        player.sendMessage(plugin.getMessage("warp.teleport.cancelled").replace("{warp}", warpName));
                        plugin.debug("Teleport cancelled for " + player.getName() + " to warp: " + warpName);
                    }
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
    
    private void sendClickableWarp(Player player, String warpName, String worldName, String createdBy) {
        String rawMessage = plugin.getMessage("warp.list.entry")
                .replace("{warp}", "{WARP_PLACEHOLDER}")
                .replace("{world}", worldName)
                .replace("{creator}", createdBy);

        String[] parts = rawMessage.split("\\{WARP_PLACEHOLDER\\}", 2);
        
        if (parts.length == 2) {
            Component component = Component.text(parts[0])
                    .append(Component.text(warpName, NamedTextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand("/warp " + warpName))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("§aClick to teleport to " + warpName + "\n§7World: " + worldName + "\n§7Created by: " + createdBy)
                            )))
                    .append(Component.text(parts[1]));
            
            player.sendMessage(component);
        } else {
            player.sendMessage(rawMessage.replace("{WARP_PLACEHOLDER}", warpName));
        }
    }
}
