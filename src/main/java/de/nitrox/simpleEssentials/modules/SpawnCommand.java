package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SpawnCommand implements Listener {
    
    private final SimpleEssentials plugin;
    
    public SpawnCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    public void registerSpawnCommands() {
        // Spawn Command
        new CommandAPICommand("spawn")
                .withPermission("simpleessentials.spawn")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("spawn.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    Location spawnLocation = getSpawnLocation();
                    
                    if (spawnLocation == null) {
                        player.sendMessage(plugin.getMessage("spawn.not_set"));
                        player.sendMessage(plugin.getMessage("spawn.setspawn_hint"));
                        return;
                    }
                    
                    plugin.debug("Spawn command executed by: " + player.getName());
                    
                    // Teleport player to spawn
                    player.teleport(spawnLocation);
                    player.sendMessage(plugin.getMessage("spawn.teleported")
                            .replace("{world}", spawnLocation.getWorld().getName())
                            .replace("{x}", String.format("%.1f", spawnLocation.getX()))
                            .replace("{y}", String.format("%.1f", spawnLocation.getY()))
                            .replace("{z}", String.format("%.1f", spawnLocation.getZ())));
                })
                .register();
        
        // SetSpawn Command
        new CommandAPICommand("setspawn")
                .withPermission("simpleessentials.setspawn")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("spawn.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    Location playerLocation = player.getLocation();
                    
                    plugin.debug("SetSpawn command executed by: " + player.getName());
                    
                    // Set spawn in config
                    setSpawnLocation(playerLocation);
                    
                    player.sendMessage(plugin.getMessage("spawn.set_success")
                            .replace("{world}", playerLocation.getWorld().getName())
                            .replace("{x}", String.format("%.1f", playerLocation.getX()))
                            .replace("{y}", String.format("%.1f", playerLocation.getY()))
                            .replace("{z}", String.format("%.1f", playerLocation.getZ()))
                            .replace("{yaw}", String.format("%.1f", playerLocation.getYaw()))
                            .replace("{pitch}", String.format("%.1f", playerLocation.getPitch())));
                    
                    // Broadcast if enabled
                    if (plugin.getConfig().getBoolean("spawn.broadcast_setspawn", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("spawn.set_broadcast")
                                .replace("{player}", player.getName())
                                .replace("{world}", playerLocation.getWorld().getName()));
                    }
                })
                .register();
    }
    
    /**
     * Gets the spawn location from config
     */
    public Location getSpawnLocation() {
        if (!plugin.getConfig().contains("spawn.location")) {
            return null;
        }
        
        try {
            String worldName = plugin.getConfig().getString("spawn.location.world");
            double x = plugin.getConfig().getDouble("spawn.location.x");
            double y = plugin.getConfig().getDouble("spawn.location.y");
            double z = plugin.getConfig().getDouble("spawn.location.z");
            float yaw = (float) plugin.getConfig().getDouble("spawn.location.yaw", 0.0);
            float pitch = (float) plugin.getConfig().getDouble("spawn.location.pitch", 0.0);
            
            if (worldName == null || Bukkit.getWorld(worldName) == null) {
                plugin.debug("Spawn world not found: " + worldName);
                return null;
            }
            
            return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.debug("Error loading spawn location: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Sets the spawn location in config
     */
    public void setSpawnLocation(Location location) {
        plugin.getConfig().set("spawn.location.world", location.getWorld().getName());
        plugin.getConfig().set("spawn.location.x", location.getX());
        plugin.getConfig().set("spawn.location.y", location.getY());
        plugin.getConfig().set("spawn.location.z", location.getZ());
        plugin.getConfig().set("spawn.location.yaw", location.getYaw());
        plugin.getConfig().set("spawn.location.pitch", location.getPitch());
        plugin.saveConfig();
        
        plugin.debug("Spawn location set to: " + location.getWorld().getName() + 
                     " at " + location.getX() + "," + location.getY() + "," + location.getZ());
    }
    
    /**
     * Event handler for enforcing spawn on join
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.enforce-spawn-on-join", false)) {
            return;
        }
        
        Player player = event.getPlayer();
        Location spawnLocation = getSpawnLocation();
        
        if (spawnLocation == null) {
            plugin.debug("Spawn location not set, cannot teleport " + player.getName() + " on join");
            return;
        }
        
        // Delay teleport to avoid conflicts with other plugins
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.debug("Teleporting " + player.getName() + " to spawn on join");
                player.teleport(spawnLocation);
                
                // Send message if enabled
                if (plugin.getConfig().getBoolean("spawn.send_message_on_teleport", true)) {
                    player.sendMessage(plugin.getMessage("spawn.join_teleport"));
                }
            }
        }, 20L); // 1 second delay
    }
}
