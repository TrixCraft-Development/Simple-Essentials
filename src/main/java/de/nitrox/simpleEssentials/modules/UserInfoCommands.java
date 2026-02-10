package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import de.nitrox.simpleEssentials.BanlogManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.lang.reflect.Field;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class UserInfoCommands {
    
    private final SimpleEssentials plugin;
    private final BanlogManager banlogManager;
    private final SimpleDateFormat dateFormat;
    
    public UserInfoCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
        this.banlogManager = new BanlogManager(plugin);
        this.dateFormat = new SimpleDateFormat("dd MM, yyyy HH:mm:ss");
    }
    
    /**
     * Fetches name history from Mojang API
     * @param uuid Player UUID
     * @return CompletableFuture containing list of name changes with dates
     */
    private CompletableFuture<List<NameChange>> fetchNameHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use the correct Minecraft Services API endpoint
                String urlStr = "https://api.minecraftservices.com/minecraft/profile/lookup/" + uuid.toString().replace("-", "");
                plugin.debug("Fetching profile from: " + urlStr);
                
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("User-Agent", "SimpleEssentials/1.0");
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                plugin.debug("Minecraft Services API response code: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();
                    plugin.debug("Minecraft Services API response: " + jsonResponse);
                    
                    Gson gson = new Gson();
                    JsonObject profile = gson.fromJson(jsonResponse, JsonObject.class);
                    List<NameChange> nameHistory = new ArrayList<>();
                    
                    // Add current name
                    if (profile.has("name")) {
                        String currentName = profile.get("name").getAsString();
                        nameHistory.add(new NameChange(currentName, null)); // Current name has no change timestamp
                    }
                    
                    // The Minecraft Services API doesn't provide name history directly
                    // We need to use the legacy Mojang API for name history as a fallback
                    return fetchNameHistoryFromLegacyAPI(uuid, nameHistory);
                    
                } else if (responseCode == 404) {
                    // 404 Not Found means player doesn't exist in Minecraft database
                    plugin.debug("Player not found in Minecraft database (404 response)");
                    return new ArrayList<>();
                } else {
                    plugin.debug("Failed to fetch profile: HTTP " + responseCode);
                    // Fallback to legacy API
                    return fetchNameHistoryFromLegacyAPI(uuid, new ArrayList<>());
                }
            } catch (Exception e) {
                plugin.debug("Error fetching profile: " + e.getMessage());
                e.printStackTrace();
                // Fallback to legacy API
                return fetchNameHistoryFromLegacyAPI(uuid, new ArrayList<>());
            }
        });
    }
    
    /**
     * Fallback method to fetch name history from the legacy Mojang API
     */
    private List<NameChange> fetchNameHistoryFromLegacyAPI(UUID uuid, List<NameChange> existingHistory) {
        try {
            String urlStr = "https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names";
            plugin.debug("Fallback: Fetching name history from legacy API: " + urlStr);
            
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "SimpleEssentials/1.0");
            connection.setRequestProperty("Accept", "application/json");
            
            int responseCode = connection.getResponseCode();
            plugin.debug("Legacy Mojang API response code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String jsonResponse = response.toString();
                plugin.debug("Legacy Mojang API response: " + jsonResponse);
                
                // Debug: Show raw response to console
                System.out.println("[SimpleEssentials DEBUG] Raw Mojang API Response: " + jsonResponse);
                
                Gson gson = new Gson();
                JsonArray jsonArray = gson.fromJson(jsonResponse, JsonArray.class);
                List<NameChange> nameHistory = new ArrayList<>(existingHistory);
                
                for (JsonElement element : jsonArray) {
                    JsonObject nameObj = element.getAsJsonObject();
                    String name = nameObj.get("name").getAsString();
                    
                    Long changedToAt = null;
                    if (nameObj.has("changedToAt") && !nameObj.get("changedToAt").isJsonNull()) {
                        changedToAt = nameObj.get("changedToAt").getAsLong();
                    }
                    
                    plugin.debug("Legacy API - Found name: " + name + " with timestamp: " + changedToAt);
                    
                    // Avoid duplicates - check if this name already exists
                    boolean exists = nameHistory.stream().anyMatch(nc -> nc.getName().equals(name));
                    if (!exists) {
                        nameHistory.add(new NameChange(name, changedToAt));
                        plugin.debug("Added name to history: " + name);
                    } else {
                        plugin.debug("Skipped duplicate name: " + name);
                    }
                }
                
                plugin.debug("Parsed " + nameHistory.size() + " total name changes");
                return nameHistory;
            } else if (responseCode == 204) {
                plugin.debug("Player exists but has no name history (204 response)");
                return existingHistory;
            } else if (responseCode == 404) {
                plugin.debug("Player not found in Mojang database (404 response)");
                return existingHistory;
            } else if (responseCode == 429) {
                plugin.debug("Rate limited by Mojang API (429 response) - trying NameMC fallback");
                return fetchNameHistoryFromNameMC(uuid, existingHistory);
            } else {
                plugin.debug("Failed to fetch name history from legacy API: HTTP " + responseCode + " - trying NameMC fallback");
                return fetchNameHistoryFromNameMC(uuid, existingHistory);
            }
        } catch (Exception e) {
            plugin.debug("Error fetching name history from legacy API: " + e.getMessage() + " - trying NameMC fallback");
            return fetchNameHistoryFromNameMC(uuid, existingHistory);
        }
    }
    
    /**
     * Final fallback: Try to get basic info from NameMC (public profile)
     */
    private List<NameChange> fetchNameHistoryFromNameMC(UUID uuid, List<NameChange> existingHistory) {
        try {
            // NameMC public profile page (scraping approach)
            String urlStr = "https://namemc.com/profile/" + uuid.toString().replace("-", "");
            plugin.debug("Final fallback: Checking NameMC profile: " + urlStr);
            
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (SimpleEssentials/1.0)");
            
            int responseCode = connection.getResponseCode();
            plugin.debug("NameMC response code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                String htmlContent = response.toString();
                List<NameChange> nameHistory = new ArrayList<>(existingHistory);
                
                // Simple HTML parsing to extract current name from title or meta tags
                // This is a basic approach - NameMC doesn't have a public API for name history
                if (htmlContent.contains("<title>")) {
                    String title = htmlContent.substring(htmlContent.indexOf("<title>") + 7, htmlContent.indexOf("</title>"));
                    if (title.contains(" - NameMC")) {
                        String currentName = title.replace(" - NameMC", "").trim();
                        if (!currentName.isEmpty() && !nameHistory.stream().anyMatch(nc -> nc.getName().equals(currentName))) {
                            nameHistory.add(new NameChange(currentName, null));
                            plugin.debug("Found current name from NameMC: " + currentName);
                        }
                    }
                }
                
                // Look for any name history information in the page
                // Note: This is limited as NameMC doesn't expose full history via simple scraping
                if (htmlContent.contains("Name history")) {
                    plugin.debug("NameMC page contains name history information");
                    // Could add more sophisticated parsing here if needed
                }
                
                return nameHistory;
            } else {
                plugin.debug("NameMC fallback failed: HTTP " + responseCode);
                return existingHistory;
            }
        } catch (Exception e) {
            plugin.debug("Error with NameMC fallback: " + e.getMessage());
            return existingHistory;
        }
    }
    
    /**
     * Inner class to represent a name change
     */
    private static class NameChange {
        private final String name;
        private final Long changedToAt;
        
        public NameChange(String name, Long changedToAt) {
            this.name = name;
            this.changedToAt = changedToAt;
        }
        
        public String getName() { return name; }
        public Long getChangedToAt() { return changedToAt; }
    }
    
    public void registerUserInfoCommands() {
        
        // UUID Command
        new CommandAPICommand("uuid")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.uuid")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    UUID playerId = target.getUniqueId();
                    
                    plugin.debug("UUID command executed: player=" + playerName + ", sender=" + sender.getName());
                    
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("uuid.never_joined").replace("{player}", playerName));
                        return;
                    }
                    
                    String messageText = plugin.getMessage("uuid.result")
                            .replace("{player}", playerName)
                            .replace("{uuid}", playerId.toString());
                    
                    if (sender instanceof Player) {
                        // Create clickable UUID component
                        TextComponent message = new TextComponent(messageText);
                        
                        // Find the UUID part and make it clickable
                        String uuidText = playerId.toString();
                        int uuidIndex = messageText.indexOf(uuidText);
                        if (uuidIndex != -1) {
                            TextComponent prefix = new TextComponent(messageText.substring(0, uuidIndex));
                            TextComponent uuidComponent = new TextComponent(uuidText);
                            uuidComponent.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, uuidText));
                            uuidComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                    new ComponentBuilder("Click to copy UUID").create()));
                            
                            TextComponent fullMessage = new TextComponent();
                            fullMessage.addExtra(prefix);
                            fullMessage.addExtra(uuidComponent);
                            
                            ((Player) sender).spigot().sendMessage(fullMessage);
                        } else {
                            ((Player) sender).sendMessage(messageText);
                        }
                    } else {
                        sender.sendMessage(messageText);
                    }
                })
                .register();
        
        // Name History Command
        new CommandAPICommand("namehistory")
                .withAliases("history")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.namehistory")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    UUID playerId = target.getUniqueId();
                    
                    plugin.debug("Name history command executed: player=" + playerName + ", sender=" + sender.getName());
                    
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("namehistory.never_joined").replace("{player}", playerName));
                        return;
                    }
                    
                    sender.sendMessage(plugin.getMessage("namehistory.header")
                            .replace("{player}", playerName));
                    
                    sender.sendMessage("§7Fetching name history from Mojang API...");
                    
                    // Fetch name history from Mojang API
                    fetchNameHistory(playerId).thenAccept(nameHistory -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (nameHistory.isEmpty()) {
                                sender.sendMessage("§cNo name history found for this player.");
                                sender.sendMessage("§7This could be due to:");
                                sender.sendMessage("§8• §7Player has never changed their name");
                                sender.sendMessage("§8• §7Player is from offline/cracked server");
                                sender.sendMessage("§8• §7API rate limiting (try again in 1 minute)");
                                sender.sendMessage("§8• §7Network connectivity issues");
                                sender.sendMessage("§7UUID: §f" + playerId.toString());
                                sender.sendMessage("§7Tip: For complete history, visit §ehttps://namemc.com/profile/" + playerId.toString().replace("-", ""));
                                return;
                            }
                            
                            // Display name history in reverse order (oldest to newest)
                            for (int i = nameHistory.size() - 1; i >= 0; i--) {
                                NameChange nameChange = nameHistory.get(i);
                                String name = nameChange.getName();
                                Long changedToAt = nameChange.getChangedToAt();
                                
                                plugin.debug("Processing name: " + name + " with timestamp: " + changedToAt);
                                
                                if (changedToAt != null) {
                                    // This name was changed at a specific time
                                    Date changeDate = new Date(changedToAt);
                                    String formattedDate = dateFormat.format(changeDate);
                                    
                                    if (i == nameHistory.size() - 1) {
                                        // First name (original name)
                                        sender.sendMessage("§8» §7" + name + " §8(§7Original name§8)");
                                    } else {
                                        // Changed name
                                        sender.sendMessage("§8» §7" + name + " §8(§7Changed on " + formattedDate + "§8)");
                                    }
                                } else {
                                    // Current name (no change date)
                                    sender.sendMessage("§8» §7" + name + " §8(§7Current name§8)");
                                }
                            }
                            
                            int totalChanges = Math.max(0, nameHistory.size() - 1);
                            sender.sendMessage("§7Total name changes: §e" + totalChanges);
                            
                            // Debug information
                            if (plugin.getConfig().getBoolean("settings.debug", false)) {
                                sender.sendMessage("§8[DEBUG] Found §7" + nameHistory.size() + " §8total names");
                                for (NameChange nc : nameHistory) {
                                    sender.sendMessage("§8[DEBUG] Name: §7" + nc.getName() + " §8| Time: §7" + nc.getChangedToAt());
                                }
                            }
                        });
                    });
                })
                .register();
        
        // First Join Command
        new CommandAPICommand("firstjoin")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.firstjoin")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("First join command executed: player=" + playerName + ", sender=" + sender.getName());
                    
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("firstjoin.never_joined").replace("{player}", playerName));
                        return;
                    }
                    
                    long firstPlayed = target.getFirstPlayed();
                    
                    if (firstPlayed > 0) {
                        String joinDate = dateFormat.format(new Date(firstPlayed));
                        sender.sendMessage(plugin.getMessage("firstjoin.result")
                                .replace("{player}", playerName)
                                .replace("{date}", joinDate));
                    } else {
                        sender.sendMessage(plugin.getMessage("firstjoin.unknown")
                                .replace("{player}", playerName));
                    }
                })
                .register();
        
        // Last Join Command
        new CommandAPICommand("lastjoin")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.lastjoin")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Last join command executed: player=" + playerName + ", sender=" + sender.getName());
                    
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("lastjoin.never_joined").replace("{player}", playerName));
                        return;
                    }
                    
                    long lastPlayed = target.getLastPlayed();
                    
                    if (lastPlayed > 0) {
                        String joinDate = dateFormat.format(new Date(lastPlayed));
                        sender.sendMessage(plugin.getMessage("lastjoin.result")
                                .replace("{player}", playerName)
                                .replace("{date}", joinDate));
                    } else {
                        sender.sendMessage(plugin.getMessage("lastjoin.unknown")
                                .replace("{player}", playerName));
                    }
                })
                .register();
        
        // User Info Command
        new CommandAPICommand("userinfo")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.userinfo")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Userinfo command executed: player=" + playerName + ", sender=" + sender.getName());
                    
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("userinfo.never_joined").replace("{player}", playerName));
                        return;
                    }
                    
                    // Send header
                    sender.sendMessage(plugin.getMessage("userinfo.header").replace("{player}", playerName));
                    
                    // Basic info with clickable/pasteable fields
                    sendPasteableMessage(sender, plugin.getMessage("userinfo.username").replace("{player}", playerName), playerName);
                    sendPasteableMessage(sender, plugin.getMessage("userinfo.uuid").replace("{uuid}", target.getUniqueId().toString()), target.getUniqueId().toString());
                    
                    if (target.isOnline()) {
                        Player onlineTarget = target.getPlayer();
                        
                        // Online player info
                        sender.sendMessage(plugin.getMessage("userinfo.status_online"));
                        sender.sendMessage(plugin.getMessage("userinfo.ip_address").replace("{ip}", onlineTarget.getAddress().getAddress().getHostAddress()));
                        sender.sendMessage(plugin.getMessage("userinfo.host_name").replace("{host}", onlineTarget.getAddress().getAddress().getHostName()));
                        sender.sendMessage(plugin.getMessage("userinfo.ping").replace("{ping}", String.valueOf(getPing(onlineTarget))));
                        sender.sendMessage(plugin.getMessage("userinfo.server").replace("{server}", onlineTarget.getServer().getName()));
                        sender.sendMessage(plugin.getMessage("userinfo.world").replace("{world}", onlineTarget.getWorld().getName()));
                        String coords = onlineTarget.getLocation().getBlockX() + ", " + onlineTarget.getLocation().getBlockY() + ", " + onlineTarget.getLocation().getBlockZ();
                        sendPasteableMessage(sender, plugin.getMessage("userinfo.coordinates")
                                .replace("{x}", String.valueOf(onlineTarget.getLocation().getBlockX()))
                                .replace("{y}", String.valueOf(onlineTarget.getLocation().getBlockY()))
                                .replace("{z}", String.valueOf(onlineTarget.getLocation().getBlockZ())), coords);
                        sender.sendMessage(plugin.getMessage("userinfo.health").replace("{health}", String.valueOf(onlineTarget.getHealth())));
                        sender.sendMessage(plugin.getMessage("userinfo.saturation").replace("{saturation}", String.valueOf(onlineTarget.getSaturation())));
                        sender.sendMessage(plugin.getMessage("userinfo.xp_level").replace("{xp}", String.valueOf(onlineTarget.getLevel())));
                        
                        // Add client info
                        String clientInfo = getClientInfo(onlineTarget);
                        sender.sendMessage(plugin.getMessage("userinfo.client").replace("{client}", clientInfo));
                        
                        // Interactive buttons
                        sender.sendMessage(plugin.getMessage("userinfo.actions.header"));
                        
                        // Banlog button
                        int banlogCount = banlogManager.getModerationHistory(target.getUniqueId()).size();
                        sendClickableCommand(sender, plugin.getMessage("userinfo.actions.banlog")
                                .replace("{count}", String.valueOf(banlogCount)), "/banlog " + playerName, "View banlog for " + playerName);
                        
                        // Invsee button
                        sendClickableCommand(sender, plugin.getMessage("userinfo.actions.invsee"), "/invsee " + playerName, "Open " + playerName + "'s inventory");
                        
                        // Teleport button
                        String tpCoords = onlineTarget.getLocation().getBlockX() + " " + onlineTarget.getLocation().getBlockY() + " " + onlineTarget.getLocation().getBlockZ();
                        sendClickableCommand(sender, plugin.getMessage("userinfo.actions.teleport")
                                .replace("{x}", String.valueOf(onlineTarget.getLocation().getBlockX()))
                                .replace("{y}", String.valueOf(onlineTarget.getLocation().getBlockY()))
                                .replace("{z}", String.valueOf(onlineTarget.getLocation().getBlockZ())), "/tp " + sender.getName() + " " + tpCoords, "Teleport to " + playerName);
                        
                    } else {
                        // Offline player info
                        sender.sendMessage(plugin.getMessage("userinfo.status_offline"));
                        sender.sendMessage(plugin.getMessage("userinfo.last_seen").replace("{date}", dateFormat.format(new Date(target.getLastPlayed()))));
                        
                        // Offline actions
                        sender.sendMessage(plugin.getMessage("userinfo.actions.header"));
                        
                        // Banlog button
                        int banlogCount = banlogManager.getModerationHistory(target.getUniqueId()).size();
                        sendClickableCommand(sender, plugin.getMessage("userinfo.actions.banlog")
                                .replace("{count}", String.valueOf(banlogCount)), "/banlog " + playerName, "View banlog for " + playerName);
                    }
                    
                    // Send footer
                    sender.sendMessage(plugin.getMessage("userinfo.footer"));
                })
                .register();
    }
    
    /**
     * Gets the ping of a player
     */
    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            // Fallback for newer versions
            try {
                return player.getPing();
            } catch (Exception ex) {
                return -1; // Unknown ping
            }
        }
    }
    
    /**
     * Sends a clickable message that pastes text into chat
     */
    private void sendPasteableMessage(org.bukkit.command.CommandSender sender, String message, String textToPaste) {
        if (sender instanceof Player) {
            TextComponent component = new TextComponent(message);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, textToPaste));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to paste into chat").create()));
            ((Player) sender).spigot().sendMessage(component);
        } else {
            sender.sendMessage(message);
        }
    }
    
    /**
     * Gets client information (brand and version)
     */
    private String getClientInfo(Player player) {
        if (player.hasMetadata("client_brand")) {
            String storedBrand = player.getMetadata("client_brand").get(0).asString();
            if (!storedBrand.isEmpty()) {
                plugin.debug("Client brand from metadata: " + storedBrand);
                return storedBrand;
            }
        }
        
        try {
            // Method 1: Try to get client brand from Player's ClientSettings (most reliable for modern versions)
            try {
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = entityPlayer.getClass().getField("connection").get(entityPlayer);

                // Try to get client brand from connection
                try {
                    Object clientBrand = connection.getClass().getMethod("getBrand").invoke(connection);
                    if (clientBrand != null && !clientBrand.toString().isEmpty()) {
                        String brand = clientBrand.toString();
                        plugin.debug("Client brand from connection: " + brand);
                        return brand;
                    }
                } catch (Exception e) {
                }
                
                // Try to get from packet listener
                try {
                    Object packetListener = connection.getClass().getField("packetListener").get(connection);
                    if (packetListener != null) {
                        Object brand = packetListener.getClass().getMethod("getBrand").invoke(packetListener);
                        if (brand != null && !brand.toString().isEmpty()) {
                            String brandStr = brand.toString();
                            plugin.debug("Client brand from packet listener: " + brandStr);
                            return brandStr;
                        }
                    }
                } catch (Exception e) {
                }
                
                // Try to get brand from connection's network manager
                try {
                    Object networkManager = connection.getClass().getField("networkManager").get(connection);
                    if (networkManager != null) {
                        Object channel = networkManager.getClass().getMethod("getChannel").invoke(networkManager);
                        if (channel != null) {
                            // Try to get client brand from channel attributes
                            try {
                                Object brandAttr = channel.getClass().getMethod("attr", String.class).invoke(channel, "minecraft:brand");
                                if (brandAttr != null) {
                                    Object brand = brandAttr.getClass().getMethod("get").invoke(brandAttr);
                                    if (brand != null && !brand.toString().isEmpty()) {
                                        String brandStr = brand.toString();
                                        plugin.debug("Client brand from channel attribute: " + brandStr);
                                        return brandStr;
                                    }
                                }
                            } catch (Exception e) {
                                // Continue to next method
                            }
                        }
                    }
                } catch (Exception e) {
                    // Continue to next method
                }
            } catch (Exception e) {
                plugin.debug("Error in connection-based detection: " + e.getMessage());
            }
            
            // Method 2: Try to get from network manager and channel (for older versions)
            try {
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                Object connection = entityPlayer.getClass().getField("connection").get(entityPlayer);
                
                // Try different field names for network manager
                Object networkManager = null;
                try {
                    networkManager = connection.getClass().getField("networkManager").get(connection);
                } catch (NoSuchFieldException e) {
                    try {
                        networkManager = connection.getClass().getField("a").get(connection);
                    } catch (NoSuchFieldException e2) {
                        // Try alternative field names
                        Field[] fields = connection.getClass().getDeclaredFields();
                        for (Field field : fields) {
                            field.setAccessible(true);
                            Object value = field.get(connection);
                            if (value != null && value.getClass().getSimpleName().contains("NetworkManager")) {
                                networkManager = value;
                                break;
                            }
                        }
                    }
                }
                
                if (networkManager != null) {
                    Object channel = networkManager.getClass().getMethod("getChannel").invoke(networkManager);
                    if (channel != null) {
                        // Try to get client brand from channel pipeline
                        try {
                            Object pipeline = channel.getClass().getMethod("pipeline").invoke(channel);
                            if (pipeline != null) {
                                // Look for brand handler in pipeline
                                Object brandHandler = null;
                                try {
                                    brandHandler = pipeline.getClass().getMethod("get", String.class).invoke(pipeline, "brand");
                                } catch (Exception e) {
                                    // Try to find handler by iterating
                                    java.util.List<Object> handlers = (java.util.List<Object>) pipeline.getClass().getMethod("values").invoke(pipeline);
                                    for (Object handler : handlers) {
                                        if (handler.getClass().getSimpleName().toLowerCase().contains("brand")) {
                                            brandHandler = handler;
                                            break;
                                        }
                                    }
                                }
                                
                                if (brandHandler != null) {
                                    try {
                                        Object brand = brandHandler.getClass().getMethod("getBrand").invoke(brandHandler);
                                        if (brand != null && !brand.toString().isEmpty()) {
                                            String brandStr = brand.toString();
                                            plugin.debug("Client brand from pipeline: " + brandStr);
                                            return brandStr;
                                        }
                                    } catch (Exception e) {
                                        // Try alternative method names
                                        try {
                                            Object brand = brandHandler.getClass().getMethod("brand").invoke(brandHandler);
                                            if (brand != null && !brand.toString().isEmpty()) {
                                                String brandStr = brand.toString();
                                                plugin.debug("Client brand from pipeline alt: " + brandStr);
                                                return brandStr;
                                            }
                                        } catch (Exception e2) {
                                            // Continue
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            plugin.debug("Error in pipeline detection: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                plugin.debug("Error in network manager detection: " + e.getMessage());
            }
            
            // Method 3: Try to get from Player's ClientBrandEvent (if available)
            try {
                // This would require listening to ClientBrandEvent, but we can try to get stored data
                if (player.hasMetadata("client_brand")) {
                    String brand = player.getMetadata("client_brand").get(0).asString();
                    if (!brand.isEmpty()) {
                        plugin.debug("Client brand from metadata: " + brand);
                        return brand;
                    }
                }
            } catch (Exception e) {
                // Continue to fallback
            }
            
            // Method 4: Try aggressive field access to find brand information
            try {
                Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                
                // Try all fields in entityPlayer to find brand-related data
                Field[] entityFields = entityPlayer.getClass().getDeclaredFields();
                for (Field field : entityFields) {
                    field.setAccessible(true);
                    Object value = field.get(entityPlayer);
                    if (value != null) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("brand") || fieldName.contains("client")) {
                            try {
                                String brandStr = value.toString();
                                if (!brandStr.isEmpty() && !brandStr.contains("EntityPlayer") && !brandStr.contains("ServerPlayer")) {
                                    plugin.debug("Client brand from entity field " + fieldName + ": " + brandStr);
                                    return brandStr;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                }
                
                // Try connection fields
                Object connection = entityPlayer.getClass().getField("connection").get(entityPlayer);
                Field[] connectionFields = connection.getClass().getDeclaredFields();
                for (Field field : connectionFields) {
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value != null) {
                        String fieldName = field.getName().toLowerCase();
                        if (fieldName.contains("brand") || fieldName.contains("client")) {
                            try {
                                String brandStr = value.toString();
                                if (!brandStr.isEmpty() && !brandStr.contains("Connection") && !brandStr.contains("NetworkManager")) {
                                    plugin.debug("Client brand from connection field " + fieldName + ": " + brandStr);
                                    return brandStr;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.debug("Error in aggressive field detection: " + e.getMessage());
            }
            
            // Final fallback: Return server version with indicator
            String serverVersion = Bukkit.getVersion();
            String cleanVersion = serverVersion.split("\\(")[0].trim();
            plugin.debug("Using fallback version: " + cleanVersion);
            return cleanVersion + " (Unknown brand)";
            
        } catch (Exception e) {
            plugin.debug("Critical error in client detection: " + e.getMessage());
            e.printStackTrace();
            return Bukkit.getVersion().split("\\(")[0].trim() + " (Error detecting)";
        }
    }
    
    /**
     * Sends a clickable message that runs a command
     */
    private void sendClickableCommand(org.bukkit.command.CommandSender sender, String message, String command, String hoverText) {
        if (sender instanceof Player) {
            TextComponent component = new TextComponent(message);
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
            ((Player) sender).spigot().sendMessage(component);
        } else {
            sender.sendMessage(message);
        }
    }
}