package com.tenshiku.guppycosmetics;

import org.bukkit.plugin.java.JavaPlugin;

public class GuppyCosmetics extends JavaPlugin {

    private ConfigManager configManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        eventListener = new EventListener(this, configManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        commandHandler = new CommandHandler(this, configManager);
        getCommand("guppycosmetics").setExecutor(commandHandler);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
