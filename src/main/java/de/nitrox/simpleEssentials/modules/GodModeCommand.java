package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GodModeCommand {
    
    private static final Set<UUID> godModePlayers = new HashSet<>();
    private final SimpleEssentials plugin;
    
    public GodModeCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerGodModeCommands() {
        
        new CommandAPICommand("godmode")
                .withAliases("god")
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.godmode.others")
                .executes((sender, args) -> {
                    String modeStr = (String) args.get("mode");
                    Player target = (Player) args.get("player");

                    plugin.debug("Godmode command executed: mode=" + modeStr + ", target=" + target.getName() + ", sender=" + sender.getName());
                    toggleGodMode(target);
                    
                    if (sender != target) {
                        boolean isGodMode = godModePlayers.contains(target.getUniqueId());
                        sender.sendMessage(plugin.getMessage("godmode.toggled_other")
                                .replace("{status}", isGodMode ? "enabled" : "disabled")
                                .replace("{player}", target.getName()));
                    }
                })
                .register();
        
        new CommandAPICommand("godmode")
                .withAliases("god")
                .withPermission("simpleessentials.godmode")
                .executesPlayer((player, args) -> {
                    String modeStr = (String) args.get("mode");
                    plugin.debug("Godmode command executed: mode=" + modeStr + ", sender=" + player.getName());
                    toggleGodMode(player);
                })
                .register();
    }
    
    private void toggleGodMode(Player player) {
        UUID playerId = player.getUniqueId();
        boolean isGodMode = godModePlayers.contains(playerId);
        
        if (isGodMode) {
            godModePlayers.remove(playerId);
            player.setInvulnerable(false);
            for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getTarget() == player) {
                        mob.setTarget(null);
                    }
                }
            }
            player.sendMessage(plugin.getMessage("godmode.disabled"));
        } else {
            godModePlayers.add(playerId);
            player.setInvulnerable(true);
            for (Entity entity : player.getNearbyEntities(50, 50, 50)) {
                if (entity instanceof Mob) {
                    Mob mob = (Mob) entity;
                    if (mob.getTarget() == player) {
                        mob.setTarget(null);
                    }
                }
            }
            player.sendMessage(plugin.getMessage("godmode.enabled"));
        }
    }
    
    public static boolean isGodMode(Player player) {
        return godModePlayers.contains(player.getUniqueId());
    }
}
