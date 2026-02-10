package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.CachedServerIcon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class ServerListModule implements Listener {
    
    private final SimpleEssentials plugin;
    private final Random random = new Random();
    private CachedServerIcon customFavicon;
    private int currentOnlineCount;
    private BukkitRunnable refreshTask;
    
    public ServerListModule(SimpleEssentials plugin) {
        this.plugin = plugin;
        loadCustomFavicon();
        refreshOnlineCount();
        startDynamicRefresh();
    }
    
    /**
     * Loads a custom favicon from the plugin data folder
     */
    private void loadCustomFavicon() {
        try {
            File faviconFile = new File(plugin.getDataFolder(), "server-icon.png");
            if (faviconFile.exists()) {
                BufferedImage image = ImageIO.read(faviconFile);
                if (image != null) {
                    // Ensure image is 64x64 pixels
                    if (image.getWidth() != 64 || image.getHeight() != 64) {
                        BufferedImage resized = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = resized.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.drawImage(image, 0, 0, 64, 64, null);
                        g.dispose();
                        image = resized;
                    }
                    try {
                        customFavicon = Bukkit.loadServerIcon(image);
                        plugin.getLogger().info("Custom server icon loaded successfully");
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not load server icon from image: " + e.getMessage());
                        createDefaultIcon();
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load custom server icon: " + e.getMessage());
            // Create a default icon
            createDefaultIcon();
        } catch (Exception e) {
            plugin.getLogger().warning("Unexpected error loading server icon: " + e.getMessage());
            createDefaultIcon();
        }
    }
    
    /**
     * Creates a default server icon
     */
    private void createDefaultIcon() {
        try {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            
            // Create a simple gradient background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(0, 100, 200), 64, 64, new Color(0, 50, 100));
            g.setPaint(gradient);
            g.fillRect(0, 0, 64, 64);
            
            // Add a simple text or symbol
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g.getFontMetrics();
            String text = "SE";
            int x = (64 - fm.stringWidth(text)) / 2;
            int y = (64 - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
            
            g.dispose();
            customFavicon = Bukkit.loadServerIcon(image);
            plugin.getLogger().info("Default server icon created");
        } catch (Exception e) {
            plugin.getLogger().warning("Could not create default server icon: " + e.getMessage());
            // Set to null to use server default
            customFavicon = null;
        }
    }
    
    /**
     * Starts the dynamic refresh task for fake player counts
     */
    private void startDynamicRefresh() {
        // Check if dynamic refresh is enabled
        if (!plugin.getConfig().getBoolean("serverlist.fake_players.enabled", false) ||
            !plugin.getConfig().getBoolean("serverlist.fake_players.dynamic_refresh.enabled", false)) {
            return;
        }
        
        long refreshInterval = plugin.getConfig().getLong("serverlist.fake_players.dynamic_refresh.interval_ticks", 6000); // Default: 5 minutes
        
        // Cancel existing task if running
        stopDynamicRefresh();
        
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshOnlineCount();
                plugin.debug("Dynamic fake player count refreshed to: " + currentOnlineCount);
            }
        };
        
        refreshTask.runTaskTimer(plugin, refreshInterval, refreshInterval);
        plugin.getLogger().info("Dynamic fake player refresh started every " + (refreshInterval / 20) + " seconds");
    }
    
    /**
     * Stops the dynamic refresh task
     */
    private void stopDynamicRefresh() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }
    
    /**
     * Refreshes the online player count based on configuration
     */
    public void refreshOnlineCount() {
        if (!plugin.getConfig().getBoolean("serverlist.fake_players.enabled", false)) {
            currentOnlineCount = Bukkit.getOnlinePlayers().size();
            return;
        }
        
        String mode = plugin.getConfig().getString("serverlist.fake_players.mode", "static").toLowerCase();
        
        if (mode.equals("static")) {
            currentOnlineCount = plugin.getConfig().getInt("serverlist.fake_players.static_count", Bukkit.getOnlinePlayers().size());
        } else if (mode.equals("range")) {
            int min = plugin.getConfig().getInt("serverlist.fake_players.range.min", 1);
            int max = plugin.getConfig().getInt("serverlist.fake_players.range.max", 10);
            currentOnlineCount = random.nextInt(max - min + 1) + min;
        }
        
        plugin.getLogger().info("ServerList online count set to: " + currentOnlineCount);
    }
    
    /**
     * Handles server list ping events
     */
    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        // Set MOTD
        String motdLine1 = plugin.getConfig().getString("serverlist.motd.line1", "&6Welcome to our Server!");
        String motdLine2 = plugin.getConfig().getString("serverlist.motd.line2", "&7A Minecraft Server");
        
        // Combine lines and translate color codes
        String fullMotd = motdLine1 + "\n" + motdLine2;
        fullMotd = org.bukkit.ChatColor.translateAlternateColorCodes('&', fullMotd);
        
        event.setMotd(fullMotd);
        
        // Set max players
        int maxPlayers = plugin.getConfig().getInt("serverlist.max_players", Bukkit.getMaxPlayers());
        event.setMaxPlayers(maxPlayers);
        
        // Set online players (fake or real)
        if (plugin.getConfig().getBoolean("serverlist.fake_players.enabled", false)) {
            // Try to set fake player count using reflection if the method doesn't exist
            try {
                event.getClass().getMethod("setNumPlayers", int.class).invoke(event, currentOnlineCount);
            } catch (Exception e) {
                // Method doesn't exist, use alternative approach
                plugin.getLogger().warning("setNumPlayers method not available, using reflection failed: " + e.getMessage());
                // As fallback, we'll just log the intended count
                plugin.debug("Intended fake player count: " + currentOnlineCount);
            }
        }
        
        // Set custom favicon if available
        if (customFavicon != null) {
            event.setServerIcon(customFavicon);
        }
        
        plugin.debug("ServerList ping handled - MOTD: " + fullMotd.replace("\n", " | ") + 
                    ", Online: " + event.getNumPlayers() + "/" + event.getMaxPlayers());
    }
    
    /**
     * Gets the current fake online count
     */
    public int getCurrentOnlineCount() {
        return currentOnlineCount;
    }
    
    /**
     * Reloads the server list configuration
     */
    public void reload() {
        stopDynamicRefresh(); // Stop existing task
        loadCustomFavicon();
        refreshOnlineCount();
        startDynamicRefresh(); // Start new task with new settings
        plugin.getLogger().info("ServerList module reloaded");
    }
}
