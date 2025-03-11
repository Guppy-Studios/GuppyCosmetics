package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class GuppyCosmetics extends JavaPlugin {

    private ConfigManager configManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;
    private BackblingManager backblingManager;
    private BalloonManager balloonManager;
    private CosmeticInventoryManager cosmeticInventoryManager;
    private BalloonLeadProtector balloonLeadProtector; // Add this line

    @Override
    public void onEnable() {
        // Create plugin data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Initialize the players.yml file if it doesn't exist
        File playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
                // Create a default structure
                YamlConfiguration config = YamlConfiguration.loadConfiguration(playersFile);
                config.set("players", null); // Create empty players section
                config.save(playersFile);
            } catch (IOException e) {
                getLogger().severe("Could not create players.yml file!");
                e.printStackTrace();
            }
        }

        configManager = new ConfigManager(this);
        configManager.loadAllConfigs();

        // Initialize inventory manager before other managers
        cosmeticInventoryManager = new CosmeticInventoryManager(this, configManager);

        // Initialize managers
        backblingManager = new BackblingManager(this, configManager);
        balloonManager = new BalloonManager(this, configManager);

        // Initialize and register the balloon lead protector
        balloonLeadProtector = new BalloonLeadProtector(this);
        getServer().getPluginManager().registerEvents(balloonLeadProtector, this);

        // Pass all managers to EventListener
        eventListener = new EventListener(this, configManager, backblingManager, balloonManager);
        getServer().getPluginManager().registerEvents(eventListener, this);

        // Register command handler and tab completer
        commandHandler = new CommandHandler(this, configManager);
        CommandCompleter commandCompleter = new CommandCompleter(configManager);
        getCommand("guppycosmetics").setExecutor(commandHandler);
        getCommand("guppycosmetics").setTabCompleter(commandCompleter);

        // Log a message
        getLogger().info("GuppyCosmetics has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all player cosmetics
        for (Player player : Bukkit.getOnlinePlayers()) {
            cosmeticInventoryManager.savePlayerCosmetics(player);
        }

        // Clean up display entities on shutdown
        if (backblingManager != null) {
            backblingManager.shutdown();
        }
        if (balloonManager != null) {
            balloonManager.shutdown();
        }
    }

    public CosmeticInventoryManager getCosmeticInventoryManager() {
        return cosmeticInventoryManager;
    }
}