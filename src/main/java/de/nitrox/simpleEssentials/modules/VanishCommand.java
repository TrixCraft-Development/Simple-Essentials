package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class VanishCommand implements Listener {
    
    private final SimpleEssentials plugin;
    private final Set<UUID> vanishedPlayers = new HashSet<>();
    private final Set<UUID> autoVanishPlayers = ConcurrentHashMap.newKeySet();
    private File autoVanishFile;
    private FileConfiguration autoVanishConfig;
    
    public VanishCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
        setupAutoVanishConfig();
        loadAutoVanishPlayers();
    }
    
    public void registerVanishCommands() {
        new CommandAPICommand("vanish")
                .withAliases("v")
                .withPermission("simpleessentials.vanish")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("vanish.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    toggleVanish(player);
                })
                .register();
        
        new CommandAPICommand("autovanish")
                .withAliases("av")
                .withPermission("simpleessentials.vanish.autovanish")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("vanish.player_only"));
                        return;
                    }
                    
                    Player player = (Player) sender;
                    toggleAutoVanish(player);
                })
                .register();
    }
    
    /**
     * Toggles auto-vanish for a player
     */
    private void toggleAutoVanish(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (autoVanishPlayers.contains(playerId)) {
            // Disable auto-vanish
            autoVanishPlayers.remove(playerId);
            autoVanishConfig.set("autovanish." + playerId.toString(), false);
            saveAutoVanishConfig();
            player.sendMessage(plugin.getMessage("vanish.autovanish_disabled"));
            plugin.debug("Player " + player.getName() + " disabled auto-vanish");
        } else {
            // Enable auto-vanish (check permission)
            if (!player.hasPermission("simpleessentials.vanish.autovanish")) {
                player.sendMessage(plugin.getMessage("vanish.autovanish_no_permission"));
                return;
            }
            
            autoVanishPlayers.add(playerId);
            autoVanishConfig.set("autovanish." + playerId.toString(), true);
            saveAutoVanishConfig();
            player.sendMessage(plugin.getMessage("vanish.autovanish_enabled"));
            plugin.debug("Player " + player.getName() + " enabled auto-vanish");
        }
    }
    
    /**
     * Toggles vanish for a player
     */
    private void toggleVanish(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (vanishedPlayers.contains(playerId)) {
            // Unvanish player
            vanishedPlayers.remove(playerId);
            
            // Make player visible to all players
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showPlayer(plugin, player);
            }
            
            // Make player visible to mobs
            player.setInvulnerable(false);
            
            // Play unvanish sound
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
            
            player.sendMessage(plugin.getMessage("vanish.unvanished"));
            plugin.debug("Player " + player.getName() + " is now visible");
            
        } else {
            // Vanish player
            vanishedPlayers.add(playerId);
            
            // Hide player from players without vanish permission (but not from self)
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                // Don't hide from self
                if (onlinePlayer.equals(player)) continue;
                
                // Don't hide from players with vanish permission
                if (onlinePlayer.hasPermission("simpleessentials.vanish.see")) continue;
                
                // Hide from everyone else
                onlinePlayer.hidePlayer(plugin, player);
            }
            
            // Make player invincible
            player.setInvulnerable(true);
            
            // Play vanish sound
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            
            player.sendMessage(plugin.getMessage("vanish.vanished"));
            plugin.debug("Player " + player.getName() + " is now vanished");
        }
    }
    
    /**
     * Updates player visibility based on vanish state and permissions
     */
    private void updatePlayerVisibility(Player player) {
        boolean isVanished = vanishedPlayers.contains(player.getUniqueId());
        
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Don't modify self-visibility
            if (onlinePlayer.equals(player)) continue;
            
            if (isVanished && !onlinePlayer.hasPermission("simpleessentials.vanish.see")) {
                onlinePlayer.hidePlayer(plugin, player);
            } else {
                onlinePlayer.showPlayer(plugin, player);
            }
        }
    }
    
    /**
     * Checks if a player is vanished
     */
    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Handles player damage events - prevents damage to vanished players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if (vanishedPlayers.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handles entity targeting - prevents mobs from targeting vanished players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player) {
            Player target = (Player) event.getTarget();
            if (vanishedPlayers.contains(target.getUniqueId())) {
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }
    
    /**
     * Handles player interactions - makes chest opens quiet for vanished players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (vanishedPlayers.contains(player.getUniqueId())) {
            // Check if player is right-clicking a chest or other container
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                org.bukkit.block.Block block = event.getClickedBlock();
                if (block != null && isContainer(block.getType())) {
                    // Special handling for shulker boxes
                    if (block.getType().name().contains("SHULKER_BOX")) {
                        // For shulker boxes, we need to handle them differently
                        handleShulkerBoxOpen(player, block, event);
                    } else {
                        // Regular containers - use spectator mode
                        handleContainerOpen(player, event);
                    }
                }
            }
        }
    }
    
    /**
     * Handles regular container opening with spectator mode
     */
    private void handleContainerOpen(Player player, PlayerInteractEvent event) {
        // Temporarily set to spectator mode for silent opening
        org.bukkit.GameMode originalGameMode = player.getGameMode();
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        
        // Restore game mode after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && vanishedPlayers.contains(player.getUniqueId())) {
                player.setGameMode(originalGameMode);
                player.setInvulnerable(true); // Keep invulnerable from vanish
            }
        }, 1L);
        
        plugin.debug("Silent container interaction for vanished player " + player.getName());
    }
    
    /**
     * Handles shulker box opening with special logic
     */
    private void handleShulkerBoxOpen(Player player, org.bukkit.block.Block block, PlayerInteractEvent event) {
        // Store original game mode
        org.bukkit.GameMode originalGameMode = player.getGameMode();
        
        // Cancel the original event to prevent sound
        event.setCancelled(true);
        
        // Switch to spectator mode and manually open the shulker box
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            
            // Simulate right-click on shulker box in spectator mode
            org.bukkit.block.BlockState state = block.getState();
            if (state instanceof org.bukkit.block.ShulkerBox) {
                // Open the shulker box silently
                player.openInventory(((org.bukkit.block.ShulkerBox) state).getInventory());
            }
            
            // Restore game mode after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && vanishedPlayers.contains(player.getUniqueId())) {
                    player.setGameMode(originalGameMode);
                    player.setInvulnerable(true); // Keep invulnerable from vanish
                }
            }, 2L);
        });
        
        plugin.debug("Silent shulker box interaction for vanished player " + player.getName());
    }
    
    /**
     * Checks if a block type is a container that makes sounds
     */
    private boolean isContainer(org.bukkit.Material material) {
        return material == org.bukkit.Material.CHEST ||
               material == org.bukkit.Material.TRAPPED_CHEST ||
               material == org.bukkit.Material.ENDER_CHEST ||
               material == org.bukkit.Material.BARREL ||
               material == org.bukkit.Material.SHULKER_BOX ||
               material.name().endsWith("_SHULKER_BOX");
    }
    
    /**
     * Handles inventory close - prevents shulker close sounds for vanished players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (vanishedPlayers.contains(player.getUniqueId())) {
            // Check if this is a shulker box inventory
            if (event.getInventory().getHolder() instanceof org.bukkit.block.ShulkerBox) {
                // Temporarily set to spectator mode to prevent close sound
                org.bukkit.GameMode originalGameMode = player.getGameMode();
                player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                
                // Restore game mode after a short delay
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && vanishedPlayers.contains(player.getUniqueId())) {
                        player.setGameMode(originalGameMode);
                        player.setInvulnerable(true); // Keep invulnerable from vanish
                    }
                }, 1L);
                
                plugin.debug("Silent shulker box close for vanished player " + player.getName());
            }
        }
    }
    
    /**
     * Handles player movement - prevents walking sounds for vanished players
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (vanishedPlayers.contains(player.getUniqueId())) {
            // Movement is handled by Bukkit's player hiding system
            // No additional sound blocking needed as Bukkit handles this
        }
    }
    
    /**
     * Handles player join - updates visibility for existing vanished players and auto-vanish
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        
        // Check if player has auto-vanish enabled
        if (autoVanishPlayers.contains(joinedPlayer.getUniqueId())) {
            // Cancel join message
            event.setJoinMessage(null);
            
            // Auto-vanish the player
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (joinedPlayer.isOnline()) {
                    vanishedPlayers.add(joinedPlayer.getUniqueId());
                    
                    // Hide from players without vanish permission
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(joinedPlayer) && !onlinePlayer.hasPermission("simpleessentials.vanish.see")) {
                            onlinePlayer.hidePlayer(plugin, joinedPlayer);
                        }
                    }
                    
                    // Make player invincible
                    joinedPlayer.setInvulnerable(true);
                    
                    // Send vanish message to the player
                    joinedPlayer.sendMessage(plugin.getMessage("vanish.auto_vanished"));
                    plugin.debug("Player " + joinedPlayer.getName() + " joined vanished automatically");
                }
            }, 1L); // Small delay to ensure player is fully loaded
        }
        
        // Update visibility for existing vanished players
        for (UUID vanishedId : vanishedPlayers) {
            Player vanishedPlayer = Bukkit.getPlayer(vanishedId);
            if (vanishedPlayer != null && vanishedPlayer.isOnline()) {
                if (!joinedPlayer.hasPermission("simpleessentials.vanish.see")) {
                    joinedPlayer.hidePlayer(plugin, vanishedPlayer);
                }
            }
        }
    }
    
    /**
     * Handles player quit - removes from vanished list
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        vanishedPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Gets the set of vanished players
     */
    public Set<UUID> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayers);
    }
    
    /**
     * Sets up the auto-vanish configuration file
     */
    private void setupAutoVanishConfig() {
        autoVanishFile = new File(plugin.getDataFolder(), "autovanish.yml");
        if (!autoVanishFile.exists()) {
            try {
                autoVanishFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create autovanish.yml file: " + e.getMessage());
            }
        }
        autoVanishConfig = YamlConfiguration.loadConfiguration(autoVanishFile);
    }
    
    /**
     * Loads auto-vanish players from config
     */
    private void loadAutoVanishPlayers() {
        if (autoVanishConfig.contains("autovanish")) {
            for (String uuidString : autoVanishConfig.getConfigurationSection("autovanish").getKeys(false)) {
                boolean enabled = autoVanishConfig.getBoolean("autovanish." + uuidString);
                if (enabled) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        autoVanishPlayers.add(uuid);
                        plugin.debug("Loaded auto-vanish for player: " + uuidString);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in autovanish.yml: " + uuidString);
                    }
                }
            }
        }
    }
    
    /**
     * Saves auto-vanish configuration to file
     */
    private void saveAutoVanishConfig() {
        try {
            autoVanishConfig.save(autoVanishFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save autovanish.yml file: " + e.getMessage());
        }
    }
}
