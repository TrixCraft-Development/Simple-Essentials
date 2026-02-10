package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.entity.Player;

public class HealCommand {
    
    private final SimpleEssentials plugin;
    
    public HealCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerHealCommands() {
        
        new CommandAPICommand("heal")
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.heal.others")
                .executes((sender, args) -> {
                    Player target = (Player) args.get("player");

                    plugin.debug("Heal command executed: " + "target=" + target.getName() + ", sender=" + sender.getName());
                    healPlayer(target);
                    
                    if (sender != target) {
                        sender.sendMessage(plugin.getMessage("heal.healed_other").replace("{player}", target.getName()));
                    }
                })
                .register();
        
        new CommandAPICommand("heal")
                .withPermission("simpleessentials.heal")
                .executesPlayer((player, args) -> {
                    plugin.debug("Heal command executed: " + "sender=" + player.getName());
                    healPlayer(player);
                })
                .register();
    }
    
    private void healPlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setFireTicks(0);
        player.sendMessage(plugin.getMessage("heal.healed"));
    }
}
