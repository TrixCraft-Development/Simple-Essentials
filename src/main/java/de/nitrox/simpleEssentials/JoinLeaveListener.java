package de.nitrox.simpleEssentials;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    private final SimpleEssentials plugin;

    public JoinLeaveListener(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("settings.disable_joinMessage", false)) {
            event.joinMessage(null);
        }

        if (plugin.getConfig().getBoolean("settings.notify_staff_on_join", false)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&6[SimpleEssentials] ");
            String template = plugin.getConfig().getString("messages.staff_join_notification", "&f{player} &ehas joined the Server &7({world})");
            String message = plugin.legacyAmpersandToSection(prefix + template
                    .replace("{player}", player.getName())
                    .replace("{world}", player.getWorld().getName()));

            for (Player online : player.getServer().getOnlinePlayers()) {
                if (online.hasPermission("simpleessentials.notify_staff") && !online.equals(player)) {
                    online.sendMessage(message);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfig().getBoolean("settings.disable_joinMessage", false)) {
            event.quitMessage(null);
        }

        if (plugin.getConfig().getBoolean("settings.notify_staff_on_join", false)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&6[SimpleEssentials] ");
            String template = plugin.getConfig().getString("messages.staff_leave_notification", "&f{player} &ehas left the Server &7({world})");
            String message = plugin.legacyAmpersandToSection(prefix + template
                    .replace("{player}", player.getName())
                    .replace("{world}", player.getWorld().getName()));

            for (Player online : player.getServer().getOnlinePlayers()) {
                if (online.hasPermission("simpleessentials.notify_staff") && !online.equals(player)) {
                    online.sendMessage(message);
                }
            }
        }
    }
}
