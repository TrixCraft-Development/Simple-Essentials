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

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class HomesCommand {
    
    private final SimpleEssentials plugin;
    private File homesFile;
    private FileConfiguration homesConfig;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    
    public HomesCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
        setupHomesFile();
    }
    
    private void setupHomesFile() {
        homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            plugin.saveResource("homes.yml", false);
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }
    
    public void reloadHomesConfig() {
        if (homesFile == null) {
            homesFile = new File(plugin.getDataFolder(), "homes.yml");
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }
    
    private void saveHomesConfig() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }
    
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
    
    private void setHome(CommandSender sender, String homeName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("homes.player_only"));
            return;
        }
        
        Player player = (Player) sender;
        
        if (homeName == null || homeName.trim().isEmpty()) {
            player.sendMessage(plugin.getMessage("homes.invalid_name"));
            return;
        }
        
        homeName = homeName.toLowerCase().trim();
        
        if (!canSetHome(player, homeName)) {
            int maxHomes = getMaxHomes(player);
            player.sendMessage(plugin.getMessage("homes.limit_reached")
                    .replace("{limit}", String.valueOf(maxHomes)));
            return;
        }
        
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
        
        String worldName = homesConfig.getString(playerPath + ".world");
        double x = homesConfig.getDouble(playerPath + ".x");
        double y = homesConfig.getDouble(playerPath + ".y");
        double z = homesConfig.getDouble(playerPath + ".z");
        float yaw = (float) homesConfig.getDouble(playerPath + ".yaw");
        float pitch = (float) homesConfig.getDouble(playerPath + ".pitch");
        
        if (plugin.getServer().getWorld(worldName) == null) {
            player.sendMessage(plugin.getMessage("homes.world_not_found")
                    .replace("{world}", worldName));
            return;
        }
        
        Location homeLocation = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
        
        player.teleport(homeLocation);
        player.sendMessage(plugin.getMessage("homes.teleport_success")
                .replace("{name}", homeName));
    }
    
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
        
        Set<String> homeNames = homesConfig.getConfigurationSection(playerPath).getKeys(false);
        
        player.sendMessage(plugin.getMessage("homes.list_header")
                .replace("{count}", String.valueOf(homeNames.size()))
                .replace("{limit}", String.valueOf(getMaxHomes(player))));
        
        List<Map.Entry<String, Long>> sortedHomes = new ArrayList<>();
        for (String homeName : homeNames) {
            long created = homesConfig.getLong(playerPath + "." + homeName + ".created", 0);
            sortedHomes.add(new AbstractMap.SimpleEntry<>(homeName, created));
        }
        sortedHomes.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        for (Map.Entry<String, Long> entry : sortedHomes) {
            String homeName = entry.getKey();
            long created = entry.getValue();
            
            String homePath = playerPath + "." + homeName;
            String worldName = homesConfig.getString(homePath + ".world");
            double x = homesConfig.getDouble(homePath + ".x");
            double y = homesConfig.getDouble(homePath + ".y");
            double z = homesConfig.getDouble(homePath + ".z");
            
            String createdDate = dateFormat.format(new Date(created));
            
            String fullMessage = plugin.getMessage("homes.list_entry")
                    .replace("{name}", homeName)
                    .replace("{world}", worldName)
                    .replace("{x}", String.format("%.1f", x))
                    .replace("{y}", String.format("%.1f", y))
                    .replace("{z}", String.format("%.1f", z))
                    .replace("{date}", createdDate);

            int startIndex = fullMessage.indexOf(homeName);
            if (startIndex != -1) {
                String before = fullMessage.substring(0, startIndex);
                String after = fullMessage.substring(startIndex + homeName.length());
                
                Component component = Component.text(before)
                        .append(Component.text(homeName)
                                .clickEvent(ClickEvent.runCommand("/home " + homeName))
                                .hoverEvent(HoverEvent.showText(
                                        Component.text(plugin.getMessage("homes.click_tooltip").replace("{name}", homeName))
                                )))
                        .append(Component.text(after));
                
                player.sendMessage(component);
            } else {
                player.sendMessage(fullMessage);
            }
        }
        
        player.sendMessage(plugin.getMessage("homes.list_footer"));
    }
    
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
        
        homesConfig.set("homes." + player.getUniqueId().toString() + "." + homeName, null);
        saveHomesConfig();
        
        player.sendMessage(plugin.getMessage("homes.delete_success")
                .replace("{name}", homeName));
    }
    
    private boolean canSetHome(Player player, String homeName) {
        String playerPath = "homes." + player.getUniqueId().toString();
        int currentHomes = 0;
        
        if (homesConfig.contains(playerPath)) {
            currentHomes = homesConfig.getConfigurationSection(playerPath).getKeys(false).size();
        }
        
        if (homesConfig.contains(playerPath + "." + homeName)) {
            return true;
        }
        
        int maxHomes = getMaxHomes(player);
        return currentHomes < maxHomes;
    }
    
    private int getMaxHomes(Player player) {
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("simpleessentials.homes.bypass." + i)) {
                return i;
            }
        }
        
        return plugin.getConfig().getInt("homes.default_limit", 5);
    }
}
