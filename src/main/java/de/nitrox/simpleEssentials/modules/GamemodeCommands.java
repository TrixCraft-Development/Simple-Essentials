package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class GamemodeCommands {
    
    private final SimpleEssentials plugin;
    
    public GamemodeCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerGamemodeCommands() {

        new CommandAPICommand("gamemode")
                .withAliases("gm")
                .withArguments(new StringArgument("mode"))
                .withArguments(new PlayerArgument("player"))
                .withPermission("simpleessentials.gamemode.others")
                .executes((sender, args) -> {
                    String modeStr = (String) args.get("mode");
                    Player target = (Player) args.get("player");
                    
                    plugin.debug("Gamemode command executed: mode=" + modeStr + ", target=" + target.getName() + ", sender=" + sender.getName());
                    
                    GameMode gameMode = parseGameMode(modeStr);
                    if (gameMode == null) {
                        sender.sendMessage(plugin.getMessage("gamemode.invalid"));
                        plugin.debug("Invalid gamemode attempted: " + modeStr);
                        return;
                    }
                    
                    target.setGameMode(gameMode);
                    String setMsg = plugin.getMessage("gamemode.set").replace("{gamemode}", gameMode.name().toLowerCase());
                    String setOtherMsg = plugin.getMessage("gamemode.set_other")
                            .replace("{player}", target.getName())
                            .replace("{gamemode}", gameMode.name().toLowerCase());
                    
                    target.sendMessage(setMsg);
                    if (sender != target) {
                        sender.sendMessage(setOtherMsg);
                    }
                    
                    plugin.debug("Gamemode changed for " + target.getName() + " to " + gameMode.name());
                })
                .register();
        
        new CommandAPICommand("gamemode")
                .withAliases("gm")
                .withArguments(new StringArgument("mode"))
                .withPermission("simpleessentials.gamemode")
                .executesPlayer((player, args) -> {
                    String modeStr = (String) args.get("mode");
                    plugin.debug("Gamemode command executed: mode=" + modeStr + ", sender=" + player.getName());
                    
                    GameMode gameMode = parseGameMode(modeStr);
                    if (gameMode == null) {
                        player.sendMessage(plugin.getMessage("gamemode.invalid"));
                        return;
                    }
                    
                    player.setGameMode(gameMode);
                    String setMsg = plugin.getMessage("gamemode.set").replace("{gamemode}", gameMode.name().toLowerCase());
                    player.sendMessage(setMsg);
                })
                .register();
    }
    
    private GameMode parseGameMode(String mode) {
        switch (mode.toLowerCase()) {
            case "survival":
            case "s":
            case "0":
                return GameMode.SURVIVAL;
            case "creative":
            case "c":
            case "1":
                return GameMode.CREATIVE;
            case "adventure":
            case "a":
            case "2":
                return GameMode.ADVENTURE;
            case "spectator":
            case "sp":
            case "3":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }
}
