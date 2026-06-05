package de.nitrox.simpleEssentials;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import de.nitrox.simpleEssentials.modules.AutoBroadcastCommand;
import de.nitrox.simpleEssentials.modules.DeathLogCommand;
import de.nitrox.simpleEssentials.modules.EnderChestCommand;
import de.nitrox.simpleEssentials.modules.FlyCommand;
import de.nitrox.simpleEssentials.modules.GamemodeCommands;
import de.nitrox.simpleEssentials.modules.GodModeCommand;
import de.nitrox.simpleEssentials.modules.HealCommand;
import de.nitrox.simpleEssentials.modules.HomesCommand;
import de.nitrox.simpleEssentials.modules.InvseeCommand;
import de.nitrox.simpleEssentials.modules.ModerationCommands;
import de.nitrox.simpleEssentials.modules.RepairCommand;
import de.nitrox.simpleEssentials.modules.ServerListModule;
import de.nitrox.simpleEssentials.modules.SimpleCommands;
import de.nitrox.simpleEssentials.modules.SpawnCommand;
import de.nitrox.simpleEssentials.modules.UserInfoCommands;
import de.nitrox.simpleEssentials.modules.VanishCommand;
import de.nitrox.simpleEssentials.modules.WarpCommands;
import de.nitrox.simpleEssentials.modules.WipeCommand;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class SimpleEssentials extends JavaPlugin {

    private SimpleEssentialsInstance instance;
    private BanlogManager banlogManager;
    private AutoBroadcastCommand autoBroadcastCommand;
    private ServerListModule serverListModule;
    private VanishCommand vanishCommand;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(
                new CommandAPIPaperConfig(this)
                        .verboseOutput(false)
        );
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        
        saveDefaultConfig();
        reloadConfig();
        
        if (getConfig().getBoolean("settings.debug", false)) {
            getLogger().info("Debug mode enabled!");
        }

        instance = new SimpleEssentialsInstance(this);
        banlogManager = new BanlogManager(this);
        
        getLogger().info("SimpleEssentials enabled!");

        // Register join/leave listener
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(this), this);

        // Register chat listener for mute functionality
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Register invsee listener for inventory viewing
        Bukkit.getPluginManager().registerEvents(new InvseeListener(this), this);
        
        // Register invsee command and listener
        InvseeCommand invseeCommand = new InvseeCommand(this);
        Bukkit.getPluginManager().registerEvents(invseeCommand, this);
        
        // Register spawn command and listener
        SpawnCommand spawnCommand = new SpawnCommand(this);
        Bukkit.getPluginManager().registerEvents(spawnCommand, this);
        
        // Register simple commands and listener
        SimpleCommands simpleCommands = new SimpleCommands(this);
        Bukkit.getPluginManager().registerEvents(simpleCommands, this);
        
        // Register enderchest command and listener
        EnderChestCommand enderChestCommand = new EnderChestCommand(this);
        Bukkit.getPluginManager().registerEvents(enderChestCommand, this);

        // Register repair command
        RepairCommand repairCommand = new RepairCommand(this);

        // Register auto broadcast command
        autoBroadcastCommand = new AutoBroadcastCommand(this);

        // Register homes command
        HomesCommand homesCommand = new HomesCommand(this);

        // Register death log command
        DeathLogCommand deathLogCommand = new DeathLogCommand(this);

        // Register server list module
        serverListModule = new ServerListModule(this);
        Bukkit.getPluginManager().registerEvents(serverListModule, this);

        // Register vanish command and listener
        vanishCommand = new VanishCommand(this);
        Bukkit.getPluginManager().registerEvents(vanishCommand, this);

        // Register warp commands
        new WarpCommands(this).registerWarpCommands();

        new GamemodeCommands(this).registerGamemodeCommands();
        new FlyCommand(this).registerFlyCommands();
        new HealCommand(this).registerHealCommands();
        new GodModeCommand(this).registerGodModeCommands();
        new UserInfoCommands(this, banlogManager).registerUserInfoCommands();
        new ModerationCommands(this, banlogManager).registerModerationCommands();
        new WipeCommand(this).registerWipeCommand();
        invseeCommand.registerInvseeCommands();
        spawnCommand.registerSpawnCommands();
        simpleCommands.registerSimpleCommands();
        enderChestCommand.registerEnderChestCommands();
        repairCommand.registerRepairCommands();
        autoBroadcastCommand.startAutoBroadcast();
        homesCommand.registerHomesCommands();
        deathLogCommand.registerDeathLogCommands();
        vanishCommand.registerVanishCommands();
        Bukkit.getPluginManager().registerEvents(deathLogCommand, this);
        registerMainCommand();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SimpleEssentialsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI found — SimpleEssentialsPlaceholder registered.");
        } else {
            getLogger().info("PlaceholderAPI not found — placeholders unavailable.");
        }
    }

    private void registerMainCommand() {

        new CommandAPICommand("simpleessentials")
                .withAliases("se")
                .executes((sender, args) -> {
                    sender.sendMessage(Component.text("=== SimpleEssentials Commands ===", NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("/se reload", NamedTextColor.GRAY));
                })

                .withSubcommand(
                        new CommandAPICommand("reload")
                                .withPermission("simpleessentials.reload")
                                .executes((sender, args) -> {
                                    reloadConfig();
                                    autoBroadcastCommand.reloadAutoBroadcast();
                                    serverListModule.reload();
                                    sender.sendMessage(Component.text("SimpleEssentials configuration reloaded!", NamedTextColor.GREEN));
                                    
                                    if (getConfig().getBoolean("settings.debug", false)) {
                                        sender.sendMessage(Component.text("Debug mode is enabled!", NamedTextColor.YELLOW));
                                        getLogger().info("Configuration reloaded by " + sender.getName());
                                    }
                                })
                )
                .register();
    }
    
    public boolean isDebugMode() {
        return getConfig().getBoolean("settings.debug", false);
    }
    
    public void debug(String message) {
        if (isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
    
    public String getMessage(String path) {
        String prefix = getConfig().getString("messages.prefix", "&6[SimpleEssentials] ");
        String fullPath = "messages." + path;
        if (!getConfig().contains(fullPath)) {
            if (isDebugMode()) {
                debug("Missing message config key: " + fullPath);
            } else {
                getLogger().warning("Missing message config key: " + fullPath);
            }
        }
        String message = getConfig().getString(fullPath, "");
        return legacyAmpersandToSection(prefix + message);
    }

    public String legacyAmpersandToSection(String text) {
        String withMiniMessage = legacyAmpersandToMiniMessage(text);
        try {
            Component component = MINIMESSAGE.deserialize(withMiniMessage);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            return text.replace("&", "§");
        }
    }

    private static final Pattern AMPERSAND_CODE = Pattern.compile("&([0-9a-fklmnor])");
    private static final Pattern HEX_CODE = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();

    private static String legacyAmpersandToMiniMessage(String text) {
        text = HEX_CODE.matcher(text).replaceAll(mr -> "<color:#" + mr.group(1) + ">");
        StringBuffer sb = new StringBuffer();
        Matcher m = AMPERSAND_CODE.matcher(text);
        while (m.find()) {
            String tag = switch (m.group(1).charAt(0)) {
                case '0' -> "black"; case '1' -> "dark_blue"; case '2' -> "dark_green";
                case '3' -> "dark_aqua"; case '4' -> "dark_red"; case '5' -> "dark_purple";
                case '6' -> "gold"; case '7' -> "gray"; case '8' -> "dark_gray";
                case '9' -> "blue"; case 'a' -> "green"; case 'b' -> "aqua";
                case 'c' -> "red"; case 'd' -> "light_purple"; case 'e' -> "yellow";
                case 'f' -> "white"; case 'k' -> "obfuscated"; case 'l' -> "bold";
                case 'm' -> "strikethrough"; case 'n' -> "underlined"; case 'o' -> "italic";
                case 'r' -> "reset";
                default -> null;
            };
            if (tag != null) {
                String repl = tag.equals("reset") ? "</reset>" : "<" + tag + ">";
                m.appendReplacement(sb, Matcher.quoteReplacement(repl));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public void onDisable() {
        // Stop auto broadcast
        if (autoBroadcastCommand != null) {
            autoBroadcastCommand.stopAutoBroadcast();
        }
        
        // Stop vanish action bar
        if (vanishCommand != null) {
            vanishCommand.stopActionBarTask();
        }
        
        CommandAPI.onDisable();
        getLogger().info("SimpleEssentials disabled!");
    }

    public SimpleEssentialsInstance getInstance() {
        return instance;
    }

    public BanlogManager getBanlogManager() {
        return banlogManager;
    }

    public VanishCommand getVanishCommand() {
        return vanishCommand;
    }
}

