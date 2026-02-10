package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import de.nitrox.simpleEssentials.BanlogManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;

public class ModerationCommands {
    
    private final SimpleEssentials plugin;
    private final BanlogManager banlogManager;
    private static final Set<UUID> mutedPlayers = new HashSet<>();
    
    public ModerationCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
        this.banlogManager = new BanlogManager(plugin);
    }
    
    public void registerModerationCommands() {
        
        // Unregister vanilla commands to override them
        CommandAPI.unregister("kick");
        CommandAPI.unregister("ban");
        
        // Kick Command with reason
        new CommandAPICommand("kick")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.kick")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = (String) args.get("reason");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Kick command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    if (!target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("kick.not_online").replace("{player}", playerName));
                        return;
                    }
                    
                    Player onlineTarget = target.getPlayer();
                    
                    // Create kick message
                    String kickMessage = plugin.getMessage("kick.screen")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName());
                    
                    // Kick the player
                    onlineTarget.kickPlayer(kickMessage);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "KICK", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("kick.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_kicks", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("kick.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Kick Command without reason
        new CommandAPICommand("kick")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.kick")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = plugin.getConfig().getString("messages.kick.default_reason", "No reason provided");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Kick command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    if (!target.isOnline()) {
                        sender.sendMessage(plugin.getMessage("kick.not_online").replace("{player}", playerName));
                        return;
                    }
                    
                    Player onlineTarget = target.getPlayer();
                    
                    // Create kick message
                    String kickMessage = plugin.getMessage("kick.screen")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName());
                    
                    // Kick the player
                    onlineTarget.kickPlayer(kickMessage);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "KICK", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("kick.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_kicks", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("kick.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Temporary Ban Command
        new CommandAPICommand("tempban")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new StringArgument("duration"))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.ban")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String durationStr = (String) args.get("duration");
                    String reason = (String) args.get("reason");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Tempban command executed: target=" + playerName + ", reason=" + reason + ", duration=" + durationStr + ", sender=" + sender.getName());
                    
                    // Create ban message
                    String banMessage = plugin.getMessage("ban.screen")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName())
                            .replace("{duration}", durationStr);
                    
                    // Handle temporary ban
                    long duration = parseDuration(durationStr);
                    if (duration > 0) {
                        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, reason, 
                                new java.util.Date(System.currentTimeMillis() + duration), sender.getName());
                        banMessage = banMessage.replace("{duration}", formatDuration(duration));
                    } else {
                        sender.sendMessage(plugin.getMessage("ban.invalid_duration"));
                        return;
                    }
                    
                    // Kick the player if online
                    if (target.isOnline()) {
                        target.getPlayer().kickPlayer(banMessage);
                    }
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "TEMPBAN", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("ban.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{duration}", formatDuration(duration)));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_bans", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("ban.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{duration}", formatDuration(duration))
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Ban Command with reason only
        new CommandAPICommand("ban")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.ban")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = (String) args.get("reason");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Ban command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    // Create ban message
                    String banMessage = plugin.getMessage("ban.screen")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName())
                            .replace("{duration}", "Permanent");
                    
                    // Permanent ban
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, reason, null, sender.getName());
                    
                    // Kick the player if online
                    if (target.isOnline()) {
                        target.getPlayer().kickPlayer(banMessage);
                    }
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "BAN", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("ban.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{duration}", "Permanent"));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_bans", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("ban.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{duration}", "Permanent")
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Ban Command without reason
        new CommandAPICommand("ban")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.ban")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = plugin.getConfig().getString("messages.ban.default_reason", "Banned");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Ban command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    // Create ban message
                    String banMessage = plugin.getMessage("ban.screen")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName())
                            .replace("{duration}", "Permanent");
                    
                    // Permanent ban
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(playerName, reason, null, sender.getName());
                    
                    // Kick the player if online
                    if (target.isOnline()) {
                        target.getPlayer().kickPlayer(banMessage);
                    }
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "BAN", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("ban.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason)
                            .replace("{duration}", "Permanent"));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_bans", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("ban.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{duration}", "Permanent")
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Temporary Mute Command
        new CommandAPICommand("tempmute")
                .withArguments(new PlayerArgument("player"))
                .withArguments(new StringArgument("time"))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.mute")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String timeStr = (String) args.get("time");
                    String reason = (String) args.get("reason");
                    
                    plugin.debug("Tempmute command executed: target=" + target.getName() + ", reason=" + reason + ", time=" + timeStr + ", sender=" + sender.getName());
                    
                    UUID playerId = target.getUniqueId();
                    
                    if (mutedPlayers.contains(playerId)) {
                        sender.sendMessage(plugin.getMessage("mute.already_muted").replace("{player}", target.getName()));
                        return;
                    }
                    
                    // Add to muted players
                    mutedPlayers.add(playerId);
                    
                    // Schedule unmute
                    long duration = parseDuration(timeStr);
                    if (duration > 0) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            mutedPlayers.remove(playerId);
                            if (target.isOnline()) {
                                target.sendMessage(plugin.getMessage("mute.expired"));
                            }
                        }, duration / 50); // Convert milliseconds to ticks
                        
                        // Notify target with time
                        target.sendMessage(plugin.getMessage("mute.target_with_time")
                                .replace("{reason}", reason)
                                .replace("{time}", formatDuration(duration))
                                .replace("{sender}", sender.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("mute.invalid_time"));
                        mutedPlayers.remove(playerId);
                        return;
                    }
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("mute.confirmation")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason)
                            .replace("{time}", formatDuration(duration)));
                })
                .register();
        
        // Mute Command with reason only
        new CommandAPICommand("mute")
                .withArguments(new PlayerArgument("player"))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.mute")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String reason = (String) args.get("reason");
                    
                    plugin.debug("Mute command executed: target=" + target.getName() + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    UUID playerId = target.getUniqueId();
                    
                    if (mutedPlayers.contains(playerId)) {
                        sender.sendMessage(plugin.getMessage("mute.already_muted").replace("{player}", target.getName()));
                        return;
                    }
                    
                    // Add to muted players
                    mutedPlayers.add(playerId);
                    
                    // Permanent mute
                    target.sendMessage(plugin.getMessage("mute.target_permanent")
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName()));
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("mute.confirmation")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason)
                            .replace("{time}", "Permanent"));
                })
                .register();
        
        // Mute Command without reason
        new CommandAPICommand("mute")
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.mute")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String reason = plugin.getConfig().getString("messages.mute.default_reason", "Muted");
                    
                    plugin.debug("Mute command executed: target=" + target.getName() + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    UUID playerId = target.getUniqueId();
                    
                    if (mutedPlayers.contains(playerId)) {
                        sender.sendMessage(plugin.getMessage("mute.already_muted").replace("{player}", target.getName()));
                        return;
                    }
                    
                    // Add to muted players
                    mutedPlayers.add(playerId);
                    
                    // Permanent mute
                    target.sendMessage(plugin.getMessage("mute.target_permanent")
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName()));
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("mute.confirmation")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason)
                            .replace("{time}", "Permanent"));
                })
                .register();
        
        // Banlist Command
        new CommandAPICommand("banlist")
                .withPermission("simpleessentials.banlist")
                .executes((sender, args) -> {
                    plugin.debug("Banlist command executed by: " + sender.getName());
                    
                    sender.sendMessage(plugin.getMessage("banlist.header"));
                    
                    // Show active bans
                    org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                    sender.sendMessage(plugin.getMessage("banlist.bans_header"));
                    
                    if (banList.getBanEntries().isEmpty()) {
                        sender.sendMessage(plugin.getMessage("banlist.no_bans"));
                    } else {
                        for (org.bukkit.BanEntry ban : banList.getBanEntries()) {
                            String duration = ban.getExpiration() != null ? 
                                    formatDuration(ban.getExpiration().getTime() - System.currentTimeMillis()) : 
                                    "Permanent";
                            sender.sendMessage(plugin.getMessage("banlist.ban_entry")
                                    .replace("{player}", ban.getTarget())
                                    .replace("{reason}", ban.getReason())
                                    .replace("{duration}", duration)
                                    .replace("{source}", ban.getSource()));
                        }
                    }
                    
                    // Show active mutes
                    sender.sendMessage(plugin.getMessage("banlist.mutes_header"));
                    if (mutedPlayers.isEmpty()) {
                        sender.sendMessage(plugin.getMessage("banlist.no_mutes"));
                    } else {
                        for (UUID mutedUUID : mutedPlayers) {
                            Player mutedPlayer = Bukkit.getPlayer(mutedUUID);
                            String playerName = mutedPlayer != null ? mutedPlayer.getName() : "Unknown";
                            sender.sendMessage(plugin.getMessage("banlist.mute_entry")
                                    .replace("{player}", playerName));
                        }
                    }
                })
                .register();
        
        // Banlog Command
        new CommandAPICommand("banlog")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.banlog")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    UUID targetUUID = target.getUniqueId();
                    
                    plugin.debug("Banlog command executed for: " + playerName + " by: " + sender.getName());
                    
                    sender.sendMessage(plugin.getMessage("banlog.header")
                            .replace("{player}", playerName));
                    
                    List<BanlogManager.ModerationEntry> history = banlogManager.getModerationHistory(targetUUID);
                    
                    if (history.isEmpty()) {
                        sender.sendMessage(plugin.getMessage("banlog.no_history")
                                .replace("{player}", playerName));
                    } else {
                        // Show recent entries (last 20)
                        int showCount = Math.min(20, history.size());
                        for (int i = history.size() - showCount; i < history.size(); i++) {
                            BanlogManager.ModerationEntry entry = history.get(i);
                            sender.sendMessage(plugin.getMessage("banlog.entry")
                                    .replace("{type}", entry.getType())
                                    .replace("{reason}", entry.getReason())
                                    .replace("{executor}", entry.getExecutor())
                                    .replace("{date}", new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(entry.getDate())));
                        }
                    }
                })
                .register();
        
        // Banlog Clear Command
        new CommandAPICommand("banlog")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new StringArgument("action"))
                .withPermission("simpleessentials.banlog.clear")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String action = (String) args.get("action");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    UUID targetUUID = target.getUniqueId();
                    
                    if (!action.equalsIgnoreCase("clear")) {
                        sender.sendMessage(plugin.getMessage("banlog.clear_usage"));
                        return;
                    }
                    
                    plugin.debug("Banlog clear command executed for: " + playerName + " by: " + sender.getName());
                    
                    banlogManager.clearModerationHistory(targetUUID);
                    
                    sender.sendMessage(plugin.getMessage("banlog.cleared")
                            .replace("{player}", playerName)
                            .replace("{sender}", sender.getName()));
                    
                    // Log the clear action
                    banlogManager.addModerationEntry(targetUUID, "BANLOG_CLEAR", "History cleared by " + sender.getName(), sender.getName());
                })
                .register();
        
        // Unban Command
        new CommandAPICommand("unban")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.unban")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = (String) args.get("reason");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Unban command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                    org.bukkit.BanEntry banEntry = banList.getBanEntry(playerName);
                    
                    if (banEntry == null) {
                        sender.sendMessage(plugin.getMessage("unban.not_banned")
                                .replace("{player}", playerName));
                        return;
                    }
                    
                    banList.pardon(playerName);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "UNBAN", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("unban.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_unbans", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("unban.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Unban Command without reason
        new CommandAPICommand("unban")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    // Return online players for tab completion
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.unban")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    String reason = plugin.getConfig().getString("messages.unban.default_reason", "Unbanned");
                    OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
                    
                    plugin.debug("Unban command executed: target=" + playerName + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    org.bukkit.BanList banList = Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                    org.bukkit.BanEntry banEntry = banList.getBanEntry(playerName);
                    
                    if (banEntry == null) {
                        sender.sendMessage(plugin.getMessage("unban.not_banned")
                                .replace("{player}", playerName));
                        return;
                    }
                    
                    banList.pardon(playerName);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(target.getUniqueId(), "UNBAN", reason, sender.getName());
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("unban.confirmation")
                            .replace("{player}", playerName)
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_unbans", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("unban.broadcast")
                                .replace("{player}", playerName)
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Unmute Command
        new CommandAPICommand("unmute")
                .withArguments(new PlayerArgument("player"))
                .withArguments(new GreedyStringArgument("reason"))
                .withPermission("simpleessentials.unmute")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String reason = (String) args.get("reason");
                    UUID targetUUID = target.getUniqueId();
                    
                    plugin.debug("Unmute command executed: target=" + target.getName() + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    if (!mutedPlayers.contains(targetUUID)) {
                        sender.sendMessage(plugin.getMessage("unmute.not_muted")
                                .replace("{player}", target.getName()));
                        return;
                    }
                    
                    mutedPlayers.remove(targetUUID);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(targetUUID, "UNMUTE", reason, sender.getName());
                    
                    // Notify target
                    target.sendMessage(plugin.getMessage("unmute.target")
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName()));
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("unmute.confirmation")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_unmutes", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("unmute.broadcast")
                                .replace("{player}", target.getName())
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
        
        // Unmute Command without reason
        new CommandAPICommand("unmute")
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.unmute")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");
                    String reason = plugin.getConfig().getString("messages.unmute.default_reason", "Unmuted");
                    UUID targetUUID = target.getUniqueId();
                    
                    plugin.debug("Unmute command executed: target=" + target.getName() + ", reason=" + reason + ", sender=" + sender.getName());
                    
                    if (!mutedPlayers.contains(targetUUID)) {
                        sender.sendMessage(plugin.getMessage("unmute.not_muted")
                                .replace("{player}", target.getName()));
                        return;
                    }
                    
                    mutedPlayers.remove(targetUUID);
                    
                    // Add to moderation history
                    banlogManager.addModerationEntry(targetUUID, "UNMUTE", reason, sender.getName());
                    
                    // Notify target
                    target.sendMessage(plugin.getMessage("unmute.target")
                            .replace("{reason}", reason)
                            .replace("{sender}", sender.getName()));
                    
                    // Send confirmation to sender
                    sender.sendMessage(plugin.getMessage("unmute.confirmation")
                            .replace("{player}", target.getName())
                            .replace("{reason}", reason));
                    
                    // Broadcast to server (optional)
                    if (plugin.getConfig().getBoolean("moderation.broadcast_unmutes", true)) {
                        Bukkit.broadcastMessage(plugin.getMessage("unmute.broadcast")
                                .replace("{player}", target.getName())
                                .replace("{reason}", reason)
                                .replace("{sender}", sender.getName()));
                    }
                })
                .register();
    }
    
    private long parseDuration(String duration) {
        try {
            duration = duration.toLowerCase();
            if (duration.endsWith("s")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 1000;
            } else if (duration.endsWith("m")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60 * 1000;
            } else if (duration.endsWith("h")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 60 * 60 * 1000;
            } else if (duration.endsWith("d")) {
                return Long.parseLong(duration.substring(0, duration.length() - 1)) * 24 * 60 * 60 * 1000;
            } else {
                return Long.parseLong(duration) * 1000; // Default to seconds
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
    
    public static boolean isMuted(Player player) {
        return mutedPlayers.contains(player.getUniqueId());
    }
}
