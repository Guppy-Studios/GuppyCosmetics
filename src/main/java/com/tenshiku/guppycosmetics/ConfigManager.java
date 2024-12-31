package com.tenshiku.guppycosmetics;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public class ConfigManager {
    private final GuppyCosmetics plugin;
    private File hatsFile, backblingFile, messagesFile;
    private FileConfiguration hatsConfig, backblingConfig, messagesConfig;

    public ConfigManager(GuppyCosmetics plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        hatsFile = new File(plugin.getDataFolder(), "hats.yml");
        backblingFile = new File(plugin.getDataFolder(), "backbling.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!hatsFile.exists()) plugin.saveResource("hats.yml", false);
        if (!backblingFile.exists()) plugin.saveResource("backbling.yml", false);
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);

        hatsConfig = YamlConfiguration.loadConfiguration(hatsFile);
        backblingConfig = YamlConfiguration.loadConfiguration(backblingFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getHatsConfig() {
        return hatsConfig;
    }

    public FileConfiguration getBackblingConfig() {
        return backblingConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void reloadAllConfigs() {
        loadAllConfigs();
    }
}
