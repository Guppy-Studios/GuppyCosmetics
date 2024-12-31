package com.tenshiku.guppycosmetics;

import org.bukkit.plugin.java.JavaPlugin;

public class GuppyCosmetics extends JavaPlugin {

    private ConfigManager configManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;
    private BackblingManager backblingManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        // Initialize BackblingManager before EventListener
        backblingManager = new BackblingManager(this, configManager);

        // Pass backblingManager to EventListener
        eventListener = new EventListener(this, configManager, backblingManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        commandHandler = new CommandHandler(this, configManager);
        getCommand("guppycosmetics").setExecutor(commandHandler);
    }

    @Override
    public void onDisable() {
        // Clean up display entities on shutdown
        if (backblingManager != null) {
            backblingManager.shutdown();
        }
    }
}