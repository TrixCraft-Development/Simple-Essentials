package de.nitrox.simpleEssentials;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SimpleEssentialsPlaceholder extends PlaceholderExpansion {

    private final SimpleEssentials plugin;

    public SimpleEssentialsPlaceholder(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getIdentifier() {
        return "simpleessentials";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null || params.isEmpty()) return "";
        if (player == null) return "";

        String key = params.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "enderchest_list" -> handleEnderchestList(player);
            case "enderchest_max" -> handleEnderchestMax(player);
            case "vanish" -> handleVanish(player);
            case "homes_count" -> handleHomesCount(player);
            case "warps_count" -> handleWarpsCount();
            case "ping" -> handlePing(player);
            case "banlog_count" -> handleBanlogCount(player);
            case "deathlog_count" -> handleDeathlogCount(player);
            default -> handleVersion(player);
        };
    }

    private String handleEnderchestList(Player player) {
        SimpleEssentialsInstance instance = plugin.getInstance();
        List<String> enderchestlist = instance.getEnderchestMax(player);
        return String.join(",", enderchestlist);
    }

    private String handleEnderchestMax(Player player) {
        SimpleEssentialsInstance instance = plugin.getInstance();
        return String.valueOf(instance.getEnderchestMax(player).size());
    }

    private String handleVanish(Player player) {
        if (plugin.getVanishCommand() == null) {
            return "false";
        }
        return String.valueOf(plugin.getVanishCommand().isVanished(player));
    }

    private String handleHomesCount(Player player) {
        File homesFile = new File(plugin.getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            return "0";
        }

        FileConfiguration homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        String playerPath = "homes." + player.getUniqueId();
        ConfigurationSection section = homesConfig.getConfigurationSection(playerPath);
        if (section == null) {
            return "0";
        }
        Set<String> homes = section.getKeys(false);
        return String.valueOf(homes.size());
    }

    private String handleWarpsCount() {
        File warpsFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            return "0";
        }

        FileConfiguration warpsConfig = YamlConfiguration.loadConfiguration(warpsFile);
        ConfigurationSection section = warpsConfig.getConfigurationSection("warps");
        if (section == null) {
            return "0";
        }
        return String.valueOf(section.getKeys(false).size());
    }

    private String handlePing(Player player) {
        try {
            return String.valueOf(player.getPing());
        } catch (Exception e) {
            return "-1";
        }
    }

    private String handleBanlogCount(Player player) {
        return String.valueOf(plugin.getBanlogManager().getModerationHistory(player.getUniqueId()).size());
    }

    private String handleDeathlogCount(Player player) {
        File deathsFile = new File(plugin.getDataFolder(), "deaths.yml");
        if (!deathsFile.exists()) {
            return "0";
        }

        FileConfiguration deathsConfig = YamlConfiguration.loadConfiguration(deathsFile);
        String playerPath = "deaths." + player.getUniqueId();
        ConfigurationSection section = deathsConfig.getConfigurationSection(playerPath);
        if (section == null) {
            return "0";
        }
        return String.valueOf(section.getKeys(false).size());
    }

    private String handleVersion(Player player) {
        return plugin.getDescription().getVersion();
    }
}
