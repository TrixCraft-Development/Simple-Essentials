package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class HomesCommand {
    
    private final SimpleEssentials plugin;
    private File homesFile;
    private FileConfiguration homesConfig;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    
    public HomesCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
        setupHomesFile();
    }
    
    /**
     * Sets up the homes configuration file
     */
    private void setupHomesFile() {
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            plugin.saveResource("homes.yml", false);
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }
    
    /**
     * Reloads the homes configuration
     */
    public void reloadHomesConfig() {
        if (homesFile == null) {
            homesFile = new File(plugin.getDataFolder(), "homes.yml");
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }
    
    /**
     * Saves the homes configuration
     */
    private void saveHomesConfig() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }
    
    /**
     * Registers the homes commands
     */
    public void registerHomesCommands() {
        // /sethome [name] command
        new CommandAPICommand("sethome")
            .withPermission("simpleessentials.sethome")
            .withArguments(new StringArgument("name").setOptional(true))
            .executes((sender, args) -> {
                String homeName = args.get(0) != null ? args.get(0).toString() : "home";
                setHome(sender, homeName);
            })
            .register();
        
        // /home [name] command
        new CommandAPICommand("home")
            .withPermission("simpleessentials.home")
            .withArguments(new StringArgument("name").setOptional(true))
            .executes((sender, args) -> {
                String homeName = args.get(0) != null ? args.get(0).toString() : "home";
                goHome(sender, homeName);
            })
            .register();
        
        // /homes command - list all homes
        new CommandAPICommand("homes")
            .withPermission("simpleessentials.homes")
            .executes((sender, args) -> {
                listHomes(sender);
            })
            .register();
        
        // /delhome [name] command
        new CommandAPICommand("delhome")
            .withPermission("simpleessentials.delhome")
            .withArguments(new StringArgument("name").setOptional(true))
            .executes((sender, args) -> {
                String homeName = args.get(0) != null ? args.get(0).toString() : "home";
                deleteHome(sender, homeName);
            })
            .register();
    }
    
    /**
     * Sets a home for the player
     */
    private void setHome(CommandSender sender, String homeName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("homes.player_only"));
            return;
        }
        
        Player player = (Player) sender;
        
        // Check if home name is valid
        if (homeName == null || homeName.trim().isEmpty()) {
            player.sendMessage(plugin.getMessage("homes.invalid_name"));
            return;
        }
        
        homeName = homeName.toLowerCase().trim();
        
        // Check home limit
        if (!canSetHome(player, homeName)) {
            int maxHomes = getMaxHomes(player);
            player.sendMessage(plugin.getMessage("homes.limit_reached")
                    .replace("{limit}", String.valueOf(maxHomes)));
            return;
        }
        
        // Set the home
        Location location = player.getLocation();
        String playerPath = "homes." + player.getUniqueId().toString() + "." + homeName;
        
        homesConfig.set(playerPath + ".world", location.getWorld().getName());
        homesConfig.set(playerPath + ".x", location.getX());
        homesConfig.set(playerPath + ".y", location.getY());
        homesConfig.set(playerPath + ".z", location.getZ());
        homesConfig.set(playerPath + ".yaw", location.getYaw());
        homesConfig.set(playerPath + ".pitch", location.getPitch());
        homesConfig.set(playerPath + ".created", System.currentTimeMillis());
        
        saveHomesConfig();
        
        player.sendMessage(plugin.getMessage("homes.set_success")
                .replace("{name}", homeName));
    }
    
    /**
     * Teleports player to their home
     */
    private void goHome(CommandSender sender, String homeName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("homes.player_only"));
            return;
        }
        
        Player player = (Player) sender;
        homeName = homeName.toLowerCase().trim();
        
        String playerPath = "homes." + player.getUniqueId().toString() + "." + homeName;
        
        if (!homesConfig.contains(playerPath)) {
            player.sendMessage(plugin.getMessage("homes.not_found")
                    .replace("{name}", homeName));
            return;
        }
        
        // Load home location
        String worldName = homesConfig.getString(playerPath + ".world");
        double x = homesConfig.getDouble(playerPath + ".x");
        double y = homesConfig.getDouble(playerPath + ".y");
        double z = homesConfig.getDouble(playerPath + ".z");
        float yaw = (float) homesConfig.getDouble(playerPath + ".yaw");
        float pitch = (float) homesConfig.getDouble(playerPath + ".pitch");
        
        // Create location (check if world exists)
        if (plugin.getServer().getWorld(worldName) == null) {
            player.sendMessage(plugin.getMessage("homes.world_not_found")
                    .replace("{world}", worldName));
            return;
        }
        
        Location homeLocation = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
        
        // Teleport player
        player.teleport(homeLocation);
        player.sendMessage(plugin.getMessage("homes.teleport_success")
                .replace("{name}", homeName));
    }
    
    /**
     * Lists all homes for the player with clickable teleportation
     */
    private void listHomes(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("homes.player_only"));
            return;
        }
        
        Player player = (Player) sender;
        String playerPath = "homes." + player.getUniqueId().toString();
        
        if (!homesConfig.contains(playerPath)) {
            player.sendMessage(plugin.getMessage("homes.no_homes"));
            return;
        }
        
        // Get all homes for this player
        Set<String> homeNames = homesConfig.getConfigurationSection(playerPath).getKeys(false);
        
        player.sendMessage(plugin.getMessage("homes.list_header")
                .replace("{count}", String.valueOf(homeNames.size()))
                .replace("{limit}", String.valueOf(getMaxHomes(player))));
        
        // Sort homes by creation date (newest first)
        List<Map.Entry<String, Long>> sortedHomes = new ArrayList<>();
        for (String homeName : homeNames) {
            long created = homesConfig.getLong(playerPath + "." + homeName + ".created", 0);
            sortedHomes.add(new AbstractMap.SimpleEntry<>(homeName, created));
        }
        sortedHomes.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        // Display each home with clickable teleport
        for (Map.Entry<String, Long> entry : sortedHomes) {
            String homeName = entry.getKey();
            long created = entry.getValue();
            
            String homePath = playerPath + "." + homeName;
            String worldName = homesConfig.getString(homePath + ".world");
            double x = homesConfig.getDouble(homePath + ".x");
            double y = homesConfig.getDouble(homePath + ".y");
            double z = homesConfig.getDouble(homePath + ".z");
            
            // Format creation date
            String createdDate = dateFormat.format(new Date(created));
            
            // Create clickable home entry
            String fullMessage = plugin.getMessage("homes.list_entry")
                    .replace("{name}", homeName)
                    .replace("{world}", worldName)
                    .replace("{x}", String.format("%.1f", x))
                    .replace("{y}", String.format("%.1f", y))
                    .replace("{z}", String.format("%.1f", z))
                    .replace("{date}", createdDate);
            
            // Find and replace the home name part with clickable version
            int startIndex = fullMessage.indexOf(homeName);
            if (startIndex != -1) {
                TextComponent clickableName = new TextComponent(homeName);
                clickableName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/home " + homeName));
                clickableName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(plugin.getMessage("homes.click_tooltip").replace("{name}", homeName)).create()));
                
                // Rebuild the component with clickable name
                String before = fullMessage.substring(0, startIndex);
                String after = fullMessage.substring(startIndex + homeName.length());
                
                TextComponent fullComponent = new TextComponent(before);
                fullComponent.addExtra(clickableName);
                fullComponent.addExtra(after);
                
                player.spigot().sendMessage(fullComponent);
            } else {
                // Fallback: send the message as regular text
                player.sendMessage(fullMessage);
            }
        }
        
        player.sendMessage(plugin.getMessage("homes.list_footer"));
    }
    
    /**
     * Deletes a home
     */
    private void deleteHome(CommandSender sender, String homeName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("homes.player_only"));
            return;
        }
        
        Player player = (Player) sender;
        homeName = homeName.toLowerCase().trim();
        
        String playerPath = "homes." + player.getUniqueId().toString() + "." + homeName;
        
        if (!homesConfig.contains(playerPath)) {
            player.sendMessage(plugin.getMessage("homes.not_found")
                    .replace("{name}", homeName));
            return;
        }
        
        // Delete the home
        homesConfig.set("homes." + player.getUniqueId().toString() + "." + homeName, null);
        saveHomesConfig();
        
        player.sendMessage(plugin.getMessage("homes.delete_success")
                .replace("{name}", homeName));
    }
    
    /**
     * Checks if a player can set another home
     */
    private boolean canSetHome(Player player, String homeName) {
        String playerPath = "homes." + player.getUniqueId().toString();
        int currentHomes = 0;
        
        if (homesConfig.contains(playerPath)) {
            currentHomes = homesConfig.getConfigurationSection(playerPath).getKeys(false).size();
        }
        
        // If home already exists, allow updating it
        if (homesConfig.contains(playerPath + "." + homeName)) {
            return true;
        }
        
        int maxHomes = getMaxHomes(player);
        return currentHomes < maxHomes;
    }
    
    /**
     * Gets the maximum number of homes a player can have
     */
    private int getMaxHomes(Player player) {
        // Check for permission-based limits first
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("simpleessentials.homes.bypass." + i)) {
                return i;
            }
        }
        
        // Fall back to config default
        return plugin.getConfig().getInt("homes.default_limit", 5);
    }
}
