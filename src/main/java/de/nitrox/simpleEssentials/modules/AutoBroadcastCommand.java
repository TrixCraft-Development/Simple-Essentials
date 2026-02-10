package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoBroadcastCommand {
    
    private final SimpleEssentials plugin;
    private BukkitRunnable broadcastTask;
    private int currentMessageIndex = 0;
    private final Random random = new Random();
    
    public AutoBroadcastCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the auto broadcast system
     */
    public void startAutoBroadcast() {
        plugin.getLogger().info("AutoBroadcast start method called");
        
        // Check if auto broadcast is enabled
        boolean enabled = plugin.getConfig().getBoolean("messages.autobroadcast.enabled", false);
        plugin.getLogger().info("AutoBroadcast enabled setting: " + enabled);
        
        if (!enabled) {
            plugin.getLogger().info("AutoBroadcast is disabled, returning");
            return;
        }
        
        // Get configuration
        long interval = plugin.getConfig().getLong("messages.autobroadcast.interval", 9000); // Default: 15 minutes (9000 ticks)
        String mode = plugin.getConfig().getString("messages.autobroadcast.mode", "sequential").toLowerCase(); // sequential or random
        List<String> messages = plugin.getConfig().getStringList("messages.autobroadcast.messages");
        
        plugin.getLogger().info("AutoBroadcast config - interval: " + interval + ", mode: " + mode + ", messages count: " + messages.size());
        
        if (messages.isEmpty()) {
            plugin.getLogger().warning("AutoBroadcast is enabled but no messages are configured!");
            return;
        }
        
        // Cancel existing task if running
        stopAutoBroadcast();
        
        // Create and start broadcast task
        broadcastTask = new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("AutoBroadcast task running, messages size: " + messages.size());
                if (messages.isEmpty()) return;
                
                // Select message based on mode
                String message;
                if (mode.equals("random")) {
                    message = messages.get(random.nextInt(messages.size()));
                } else {
                    // Sequential mode
                    message = messages.get(currentMessageIndex);
                    currentMessageIndex = (currentMessageIndex + 1) % messages.size();
                }
                
                plugin.getLogger().info("AutoBroadcast selected message: " + message);
                // Broadcast message to all players
                broadcastMessage(message);
            }
        };
        
        broadcastTask.runTaskTimer(plugin, interval, interval);
        plugin.getLogger().info("AutoBroadcast started with " + messages.size() + " messages every " + (interval / 20) + " seconds");
    }
    
    /**
     * Stops the auto broadcast system
     */
    public void stopAutoBroadcast() {
        if (broadcastTask != null && !broadcastTask.isCancelled()) {
            broadcastTask.cancel();
            broadcastTask = null;
            plugin.getLogger().info("AutoBroadcast stopped");
        }
    }
    
    /**
     * Broadcasts a message to all online players
     */
    private void broadcastMessage(String message) {
        plugin.getLogger().info("AutoBroadcast attempting to send message: " + message);
        
        // Play sound before broadcasting if configured
        playBroadcastSound();
        
        // Send the complete message as one block to all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getLogger().info("Sending broadcast to player: " + player.getName());
            // Replace placeholders in the complete message
            String formattedMessage = replacePlaceholders(message, player);
            player.sendMessage(formattedMessage);
        }
        
        // Also send to console
        plugin.getLogger().info("[AutoBroadcast] " + message);
    }
    
    /**
     * Plays the broadcast sound to all players
     */
    private void playBroadcastSound() {
        String soundName = plugin.getConfig().getString("autobroadcast.sound", "");
        if (soundName.isEmpty()) {
            return;
        }
        
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) plugin.getConfig().getDouble("autobroadcast.sound_volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("autobroadcast.sound_pitch", 1.0);
            
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name in AutoBroadcast config: " + soundName);
        }
    }
    
    /**
     * Replaces placeholders in the message
     */
    private String replacePlaceholders(String message, Player player) {
        // Basic placeholders
        message = message.replace("{player}", player.getName());
        message = message.replace("{displayname}", player.getDisplayName());
        message = message.replace("{world}", player.getWorld().getName());
        message = message.replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()));
        message = message.replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers()));
        
        // Time placeholders
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        message = message.replace("{hour}", String.format("%02d", now.getHour()));
        message = message.replace("{minute}", String.format("%02d", now.getMinute()));
        message = message.replace("{second}", String.format("%02d", now.getSecond()));
        
        // Date placeholders
        message = message.replace("{day}", String.format("%02d", now.getDayOfMonth()));
        message = message.replace("{month}", String.format("%02d", now.getMonthValue()));
        message = message.replace("{year}", String.valueOf(now.getYear()));
        
        // Color codes
        message = org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        
        return message;
    }
    
    /**
     * Reloads the auto broadcast configuration
     */
    public void reloadAutoBroadcast() {
        stopAutoBroadcast();
        startAutoBroadcast();
    }
    
    /**
     * Gets the current status of auto broadcast
     */
    public String getStatus() {
        if (broadcastTask == null || broadcastTask.isCancelled()) {
            return "disabled";
        } else {
            return "running";
        }
    }
    
    /**
     * Gets the next message that will be broadcast (for sequential mode)
     */
    public String getNextMessage() {
        List<String> messages = plugin.getConfig().getStringList("autobroadcast.messages");
        if (messages.isEmpty()) {
            return "No messages configured";
        }
        
        String mode = plugin.getConfig().getString("autobroadcast.mode", "sequential").toLowerCase();
        if (mode.equals("random")) {
            return "Random message from " + messages.size() + " messages";
        } else {
            return messages.get(currentMessageIndex);
        }
    }
}
