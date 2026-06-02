package de.nitrox.simpleEssentials.modules;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class SimpleCommands implements Listener {

    private final SimpleEssentials plugin;

    public SimpleCommands(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerSimpleCommands() {
        new CommandAPICommand("workbench")
                .withAliases("wb", "crafting")
                .withPermission("simpleessentials.workbench")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Workbench command executed by: " + player.getName());

                    Inventory workbench = Bukkit.createInventory(null, InventoryType.WORKBENCH);
                    player.openInventory(workbench);
                    player.sendMessage(plugin.getMessage("simplecommands.workbench_opened"));
                })
                .register();

        new CommandAPICommand("furnace")
                .withPermission("simpleessentials.furnace")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Furnace command executed by: " + player.getName());

                    Inventory furnace = Bukkit.createInventory(null, InventoryType.FURNACE);
                    player.openInventory(furnace);
                    player.sendMessage(plugin.getMessage("simplecommands.furnace_opened"));
                })
                .register();

        new CommandAPICommand("anvil")
                .withPermission("simpleessentials.anvil")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Anvil command executed by: " + player.getName());

                    Inventory anvil = Bukkit.createInventory(null, InventoryType.ANVIL);
                    player.openInventory(anvil);
                    player.sendMessage(plugin.getMessage("simplecommands.anvil_opened"));
                })
                .register();

        new CommandAPICommand("enchantingtable")
                .withPermission("simpleessentials.enchantingtable")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Enchantingtable command executed by: " + player.getName());

                    Inventory enchanting = Bukkit.createInventory(null, InventoryType.ENCHANTING);
                    player.openInventory(enchanting);
                    player.sendMessage(plugin.getMessage("simplecommands.enchant_opened"));
                })
                .register();

        new CommandAPICommand("brewingstand")
                .withAliases("brewing")
                .withPermission("simpleessentials.brewingstand")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Brewingstand command executed by: " + player.getName());

                    Inventory brewing = Bukkit.createInventory(null, InventoryType.BREWING);
                    player.openInventory(brewing);
                    player.sendMessage(plugin.getMessage("simplecommands.brew_opened"));
                })
                .register();

        new CommandAPICommand("smithingtable")
                .withAliases("smithing")
                .withPermission("simpleessentials.smithingtable")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Smithingtable command executed by: " + player.getName());

                    Inventory smithing = Bukkit.createInventory(null, InventoryType.SMITHING);
                    player.openInventory(smithing);
                    player.sendMessage(plugin.getMessage("simplecommands.smith_opened"));
                })
                .register();

        new CommandAPICommand("grindstone")
                .withAliases("grind")
                .withPermission("simpleessentials.grindstone")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Grind command executed by: " + player.getName());

                    Inventory grindstone = Bukkit.createInventory(null, InventoryType.GRINDSTONE);
                    player.openInventory(grindstone);
                    player.sendMessage(plugin.getMessage("simplecommands.grind_opened"));
                })
                .register();

        new CommandAPICommand("stonecutter")
                .withAliases("sc")
                .withPermission("simpleessentials.stonecutter")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Stonecutter command executed by: " + player.getName());

                    Inventory stonecutter = Bukkit.createInventory(null, InventoryType.STONECUTTER);
                    player.openInventory(stonecutter);
                    player.sendMessage(plugin.getMessage("simplecommands.stonecutter_opened"));
                })
                .register();

        new CommandAPICommand("cartographytable")
                .withAliases("cartography")
                .withPermission("simpleessentials.cartographytable")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Cartography command executed by: " + player.getName());

                    Inventory cartography = Bukkit.createInventory(null, InventoryType.CARTOGRAPHY);
                    player.openInventory(cartography);
                    player.sendMessage(plugin.getMessage("simplecommands.cartography_opened"));
                })
                .register();

        new CommandAPICommand("loom")
                .withPermission("simpleessentials.loom")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Loom command executed by: " + player.getName());

                    Inventory loom = Bukkit.createInventory(null, InventoryType.LOOM);
                    player.openInventory(loom);
                    player.sendMessage(plugin.getMessage("simplecommands.loom_opened"));
                })
                .register();

        new CommandAPICommand("enderchest")
                .withPermission("simpleessentials.enderchest")
                .executes((sender, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(plugin.getMessage("simplecommands.player_only"));
                        return;
                    }

                    Player player = (Player) sender;
                    plugin.debug("Default EnderChest command executed by: " + player.getName());

                    player.openInventory(player.getEnderChest());
                    player.sendMessage(plugin.getMessage("simplecommands.enderchest_opened"));
                })
                .register();

        new CommandAPICommand("broadcast")
                .withAliases("bc")
                .withPermission("simpleessentials.broadcast")
                .withArguments(new GreedyStringArgument("message"))
                .executes((sender, args) -> {
                    String message = (String) args.get("message");
                    String prefix = plugin.getConfig().getString("messages.prefix", "&6[SimpleEssentials] ");
                    String formatted = plugin.legacyAmpersandToSection(prefix + message);

                    plugin.debug("Broadcast command executed by: " + sender.getName() + " - message: " + message);
                    Bukkit.broadcastMessage(formatted);
                })
                .register();

        new CommandAPICommand("walkingspeed")
                .withArguments(new StringArgument("speed").replaceSuggestions(ArgumentSuggestions.strings("reset", "-1", "-0.5", "0.5", "1")))
                .withPermission("simpleessentials.walkingspeed")
                .executesPlayer((player, args) -> {
                    String speedStr = (String) args.get("speed");

                    if (speedStr.equalsIgnoreCase("reset")) {
                        player.setWalkSpeed(0.2f);
                        player.sendMessage(plugin.getMessage("walkingspeed.reset"));
                        plugin.debug("Walkingspeed reset for " + player.getName());
                        return;
                    }

                    double speed;
                    try {
                        speed = Double.parseDouble(speedStr);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getMessage("walkingspeed.invalid"));
                        return;
                    }

                    if (speed < -1 || speed > 1) {
                        player.sendMessage(plugin.getMessage("walkingspeed.invalid"));
                        return;
                    }

                    player.setWalkSpeed((float) speed);

                    player.sendMessage(plugin.getMessage("walkingspeed.set").replace("{speed}", speedStr));

                    plugin.debug("Walkingspeed command executed: " + "sender=" + player.getName() + ", speed=" + speedStr);
                })
                .register();

        new CommandAPICommand("walkingspeed")
                .withPermission("simpleessentials.walkingspeed")
                .executes((sender, args) -> {

                    Player player = (Player) sender;

                    player.getWalkSpeed();

                    player.sendMessage(plugin.getMessage("walkingspeed.get").replace("{speed}", String.valueOf(player.getWalkSpeed())));

                    plugin.debug("Walkingspeed Get command executed: " + "sender=" + player.getName());

                })
                .register();

    }
}
