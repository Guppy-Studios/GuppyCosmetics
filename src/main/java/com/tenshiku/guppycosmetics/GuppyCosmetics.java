package com.tenshiku.guppycosmetics;

import org.bukkit.plugin.java.JavaPlugin;

public class GuppyCosmetics extends JavaPlugin {

    private ConfigManager configManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;
    private BackblingManager backblingManager;
    private BalloonManager balloonManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        // Initialize managers
        backblingManager = new BackblingManager(this, configManager);
        balloonManager = new BalloonManager(this, configManager);

        // Pass both managers to EventListener
        eventListener = new EventListener(this, configManager, backblingManager, balloonManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Register command handler and tab completer
        commandHandler = new CommandHandler(this, configManager);
        CommandCompleter commandCompleter = new CommandCompleter(configManager);
        getCommand("guppycosmetics").setExecutor(commandHandler);
        getCommand("guppycosmetics").setTabCompleter(commandCompleter);
    }

    @Override
    public void onDisable() {
        // Clean up display entities on shutdown
        if (backblingManager != null) {
            backblingManager.shutdown();
        }
        if (balloonManager != null) {
            balloonManager.shutdown();
        }
    }
}