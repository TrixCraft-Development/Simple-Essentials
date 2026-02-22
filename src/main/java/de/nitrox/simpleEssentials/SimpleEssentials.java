package de.nitrox.simpleEssentials;

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
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleEssentials extends JavaPlugin {

    private SimpleEssentialsInstance instance;
    private BanlogManager banlogManager;
    private AutoBroadcastCommand autoBroadcastCommand;
    private ServerListModule serverListModule;
    private VanishCommand vanishCommand;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(
                new CommandAPIBukkitConfig(this)
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
                    sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GRAY +"SimpleEssentials Commands" + ChatColor.GREEN + " ===");
                    sender.sendMessage(ChatColor.GRAY + "/se reload");
                    sender.sendMessage(ChatColor.GRAY + "/se help");
                })

                .withSubcommand(
                        new CommandAPICommand("reload")
                                .withPermission("simpleessentials.reload")
                                .executes((sender, args) -> {
                                    reloadConfig();
                                    autoBroadcastCommand.reloadAutoBroadcast();
                                    serverListModule.reload();
                                    sender.sendMessage(ChatColor.GREEN + "SimpleEssentials configuration reloaded!");
                                    
                                    if (getConfig().getBoolean("settings.debug", false)) {
                                        sender.sendMessage(ChatColor.YELLOW + "Debug mode is enabled!");
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
        String prefix = getConfig().getString("messages.prefix", "&6[SimpleEssentials] ").replace("&", "§");
        String fullPath = "messages." + path;
        if (!getConfig().contains(fullPath)) {
            if (isDebugMode()) {
                debug("Missing message config key: " + fullPath);
            } else {
                getLogger().warning("Missing message config key: " + fullPath);
            }
        }
        String message = getConfig().getString(fullPath, "").replace("&", "§");
        return prefix + message;
    }

    @Override
    public void onDisable() {
        // Stop auto broadcast
        if (autoBroadcastCommand != null) {
            autoBroadcastCommand.stopAutoBroadcast();
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

