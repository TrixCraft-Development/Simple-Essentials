package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.entity.Player;

public class FlyCommand {
    
    private final SimpleEssentials plugin;
    
    public FlyCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerFlyCommands() {
        
        new CommandAPICommand("fly")
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.fly.others")
                .executes((sender, args) -> {
                    String modeStr = (String) args.get("mode");
                    Player target = (Player) args.get("player");

                    plugin.debug("Fly command executed: " + "target=" + target.getName() + ", sender=" + sender.getName());
                    toggleFlight(target);
                    
                    if (sender != target) {
                        sender.sendMessage(plugin.getMessage("fly.toggled_other").replace("{player}", target.getName()));
                    }
                })
                .register();
        
        new CommandAPICommand("fly")
                .withPermission("simpleessentials.fly")
                .executesPlayer((player, args) -> {
                    String modeStr = (String) args.get("mode");
                    plugin.debug("Fly command executed: " + "sender=" + player.getName());
                    toggleFlight(player);
                })
                .register();
    }
    
    private void toggleFlight(Player player) {
        boolean isFlying = player.getAllowFlight();
        player.setAllowFlight(!isFlying);
        player.setFlying(!isFlying);
        
        if (!isFlying) {
            player.sendMessage(plugin.getMessage("fly.enabled"));
        } else {
            player.sendMessage(plugin.getMessage("fly.disabled"));
        }
    }
}
