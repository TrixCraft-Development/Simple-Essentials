package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import de.nitrox.simpleEssentials.BanlogManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
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
import java.util.UUID;
import java.lang.reflect.Field;

public class UserInfoCommands {
    
    private final SimpleEssentials plugin;
    private final BanlogManager banlogManager;
    private final SimpleDateFormat dateFormat;
    
    public UserInfoCommands(SimpleEssentials plugin, BanlogManager banlogManager) {
        this.plugin = plugin;
        this.banlogManager = banlogManager;
        this.dateFormat = new SimpleDateFormat("dd MM, yyyy HH:mm:ss");
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
        
        // First Join Command
        new CommandAPICommand("firstjoin")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
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

                    sender.sendMessage(plugin.getMessage("userinfo.header").replace("{player}", playerName));

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
