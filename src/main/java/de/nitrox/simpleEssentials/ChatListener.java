package de.nitrox.simpleEssentials;

import de.nitrox.simpleEssentials.modules.ModerationCommands;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ChatListener implements Listener {
    
    private final SimpleEssentials plugin;
    
    public ChatListener(SimpleEssentials plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is muted
        if (ModerationCommands.isMuted(player)) {
            event.setCancelled(true);

            // Send muted message to player
            player.sendMessage(plugin.getMessage("chat.muted")
                    .replace("{player}", player.getName()));

            // Log the attempted chat message (optional, for debugging)
            plugin.debug("Muted player " + player.getName() + " attempted to send message: " + event.getMessage());
        }
    }

            @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
            public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
                Player player = event.getPlayer();
                String command = event.getMessage().toLowerCase();

                // Check if player is trying to use /me command
                if (command.startsWith("/me ")) {
                    // Check if player is muted
                    if (ModerationCommands.isMuted(player)) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getMessage("chat.muted")
                                .replace("{player}", player.getName()));
                        plugin.debug("Muted player " + player.getName() + " attempted to use /me: " + command);
                    }
                }
            }
}
