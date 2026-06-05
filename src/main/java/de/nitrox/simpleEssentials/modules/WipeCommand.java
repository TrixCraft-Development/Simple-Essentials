package de.nitrox.simpleEssentials.modules;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import de.nitrox.simpleEssentials.SimpleEssentials;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

public class WipeCommand {

    private final SimpleEssentials plugin;

    public WipeCommand(SimpleEssentials plugin) {
        this.plugin = plugin;
    }

    public void registerWipeCommand() {
        // Show confirmation reminder first
        new CommandAPICommand("wipe")
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.wipe")
                .executes((sender, args) -> {
                    String playerName = (String) args.get("player");
                    sender.sendMessage("§cThis action will wipe everything for " + playerName + ".");
                    sender.sendMessage("§cRun §f/wipe confirm " + playerName + " §cto actually perform the wipe.");
                })
                .register();

        // Confirmed wipe executes immediately
        new CommandAPICommand("wipe")
                .withArguments(new StringArgument("action").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    return new String[] { "confirm" };
                })))
                .withArguments(new StringArgument("player").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .toArray(String[]::new);
                })))
                .withPermission("simpleessentials.wipe")
                .executes((sender, args) -> {
                    String action = (String) args.get("action");
                    String playerName = (String) args.get("player");

                    if (!"confirm".equalsIgnoreCase(action)) {
                        sender.sendMessage("§cUsage: /wipe confirm <player>");
                        return;
                    }

                    String normalizedPlayerName = Objects.requireNonNull(playerName, "player name cannot be null");
                    OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(normalizedPlayerName);
                    if (!targetOffline.isOnline() || targetOffline.getPlayer() == null) {
                        sender.sendMessage("§cPlayer §f" + normalizedPlayerName + " §cis not online. They must be online to perform a full wipe.");
                        return;
                    }

                    Player target = targetOffline.getPlayer();
                    performWipe(target);

                    sender.sendMessage("§aThe player §e" + playerName + " §ahas been wiped successfully.");
                    target.sendMessage("§cYour player profile has been wiped by §f" + sender.getName() + "§c.");
                    plugin.debug("Wipe command executed for player=" + playerName + " by=" + sender.getName());
                })
                .register();
    }

    private void performWipe(Player player) {
        // Inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();

        // Ender chest
        player.getEnderChest().clear();

        // Experience and levels
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);

        // Health and food
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setFireTicks(0);

        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Reset advancements
        var advancementIterator = Bukkit.getServer().advancementIterator();
        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            for (String criterion : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criterion);
            }
        }

        // Reset statistics
        for (Statistic statistic : Statistic.values()) {
            try {
                switch (statistic.getType()) {
                    case UNTYPED -> player.setStatistic(statistic, 0);
                    case ENTITY -> {
                        for (EntityType entityType : EntityType.values()) {
                            try {
                                player.setStatistic(statistic, entityType, 0);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    case ITEM, BLOCK -> {
                        for (Material material : Material.values()) {
                            try {
                                player.setStatistic(statistic, material, 0);
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                plugin.debug("Unable to reset statistic " + statistic + ": " + e.getMessage());
            }
        }
    }
}
