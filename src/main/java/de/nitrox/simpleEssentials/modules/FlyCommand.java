package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class FlyCommand {
    
    private final SimpleEssentials plugin;
    
    public FlyCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerFlyCommands() {

        new CommandAPICommand("fly")
                .withArguments(new EntitySelectorArgument.OnePlayer("player"))
                .withPermission("simpleessentials.fly.others")
                .executes((sender, args) -> {
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
                    plugin.debug("Fly command executed: " + "sender=" + player.getName());
                    toggleFlight(player);
                })
                .register();

        new CommandAPICommand("flyspeed")
                .withArguments(new StringArgument("speed").replaceSuggestions(ArgumentSuggestions.strings("reset","-1","-0.5","0.5","1")))
                .withPermission("simpleessentials.flyspeed")
                .executesPlayer((player, args) -> {
                    String speedStr = (String) args.get("speed");

                    if (speedStr.equalsIgnoreCase("reset")) {
                        player.setFlySpeed(0.1f);
                        player.sendMessage(plugin.getMessage("flyspeed.reset"));
                        plugin.debug("Flyspeed reset for " + player.getName());
                        return;
                    }

                    double speed;
                    try {
                        speed = Double.parseDouble(speedStr);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getMessage("flyspeed.invalid"));
                        return;
                    }

                    if (speed < -1 || speed > 1) {
                        player.sendMessage(plugin.getMessage("flyspeed.invalid"));
                        return;
                    }

                    player.setFlySpeed((float) speed);

                    player.sendMessage(plugin.getMessage("flyspeed.set").replace("{speed}", speedStr));

                    plugin.debug("Flyspeed command executed: " + "sender=" + player.getName() + ", speed=" + speedStr);
                })
                .register();

        new CommandAPICommand("flyspeed")
                .withPermission("simpleessentials.flyspeed")
                .executes((sender, args) -> {

                    Player player = (Player) sender;

                    player.getFlySpeed();

                    player.sendMessage(plugin.getMessage("flyspeed.get").replace("{speed}", String.valueOf(player.getFlySpeed())));

                    plugin.debug("Flyspeed Get command executed: " + "sender=" + player.getName());

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
