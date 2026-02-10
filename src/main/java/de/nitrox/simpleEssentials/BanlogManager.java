package de.nitrox.simpleEssentials;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BanlogManager {
    
    private final JavaPlugin plugin;
    private File banlogFile;
    private FileConfiguration banlogConfig;
    
    public BanlogManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupBanlogFile();
    }
    
    /**
     * Sets up the banlog.yml file
     */
    private void setupBanlogFile() {
        banlogFile = new File(plugin.getDataFolder(), "banlog.yml");
        
        if (!banlogFile.exists()) {
            plugin.saveResource("banlog.yml", false);
        }
        
        banlogConfig = YamlConfiguration.loadConfiguration(banlogFile);
    }
    
    /**
     * Reloads the banlog configuration from file
     */
    public void reloadBanlog() {
        banlogConfig = YamlConfiguration.loadConfiguration(banlogFile);
    }
    
    /**
     * Saves the banlog configuration to file
     */
    public void saveBanlog() {
        try {
            banlogConfig.save(banlogFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save banlog.yml: " + e.getMessage());
        }
    }
    
    /**
     * Adds a moderation entry to the banlog
     * @param playerUUID Player's UUID
     * @param type Type of moderation action (KICK, BAN, MUTE, etc.)
     * @param reason Reason for the action
     * @param executor Who performed the action
     */
    public void addModerationEntry(UUID playerUUID, String type, String reason, String executor) {
        String uuidString = playerUUID.toString();
        
        // Create player section if it doesn't exist
        if (!banlogConfig.contains("players." + uuidString)) {
            banlogConfig.set("players." + uuidString + ".uuid", uuidString);
            banlogConfig.set("players." + uuidString + ".entries", new ArrayList<>());
        }
        
        // Create the entry
        List<String> entries = banlogConfig.getStringList("players." + uuidString + ".entries");
        String entry = type + ":" + reason + ":" + executor + ":" + new Date().getTime();
        entries.add(entry);
        
        // Update the entries list
        banlogConfig.set("players." + uuidString + ".entries", entries);
        
        // Save to file
        saveBanlog();
        
        ((SimpleEssentials) plugin).debug("Added moderation entry for " + playerUUID + ": " + type + " by " + executor);
    }
    
    /**
     * Gets the moderation history for a player
     * @param playerUUID Player's UUID
     * @return List of moderation entries
     */
    public List<ModerationEntry> getModerationHistory(UUID playerUUID) {
        List<ModerationEntry> history = new ArrayList<>();
        String uuidString = playerUUID.toString();
        
        if (!banlogConfig.contains("players." + uuidString + ".entries")) {
            return history;
        }
        
        List<String> entries = banlogConfig.getStringList("players." + uuidString + ".entries");
        
        for (String entry : entries) {
            String[] parts = entry.split(":", 4); // Split into max 4 parts
            if (parts.length >= 4) {
                String type = parts[0];
                String reason = parts[1];
                String executor = parts[2];
                long timestamp = Long.parseLong(parts[3]);
                
                history.add(new ModerationEntry(type, reason, executor, new Date(timestamp)));
            }
        }
        
        return history;
    }
    
    /**
     * Clears the moderation history for a player
     * @param playerUUID Player's UUID
     */
    public void clearModerationHistory(UUID playerUUID) {
        String uuidString = playerUUID.toString();
        
        if (banlogConfig.contains("players." + uuidString)) {
            banlogConfig.set("players." + uuidString + ".entries", new ArrayList<>());
            saveBanlog();
            
            ((SimpleEssentials) plugin).debug("Cleared moderation history for " + playerUUID);
        }
    }
    
    /**
     * Gets all players with moderation history
     * @return List of player UUIDs
     */
    public List<UUID> getPlayersWithHistory() {
        List<UUID> players = new ArrayList<>();
        
        if (banlogConfig.contains("players")) {
            for (String uuidString : banlogConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    players.add(uuid);
                } catch (IllegalArgumentException e) {
                    ((SimpleEssentials) plugin).debug("Invalid UUID in banlog: " + uuidString);
                }
            }
        }
        
        return players;
    }
    
    /**
     * Inner class to represent a moderation entry
     */
    public static class ModerationEntry {
        private final String type;
        private final String reason;
        private final String executor;
        private final Date date;
        
        public ModerationEntry(String type, String reason, String executor, Date date) {
            this.type = type;
            this.reason = reason;
            this.executor = executor;
            this.date = date;
        }
        
        public String getType() { return type; }
        public String getReason() { return reason; }
        public String getExecutor() { return executor; }
        public Date getDate() { return date; }
    }
}
