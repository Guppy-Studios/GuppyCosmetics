package com.tenshiku.guppycosmetics;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigManager {
    private final GuppyCosmetics plugin;
    private File hatsFile, backblingFile, balloonsFile, messagesFile;
    private FileConfiguration hatsConfig, backblingConfig, balloonsConfig, messagesConfig;

    public ConfigManager(GuppyCosmetics plugin) {
        this.plugin = plugin;
    }

    public void loadAllConfigs() {
        // Create cosmetics directory if it doesn't exist
        File cosmeticsDir = new File(plugin.getDataFolder(), "cosmetics");
        if (!cosmeticsDir.exists()) {
            cosmeticsDir.mkdirs();
        }

        // Initialize file objects
        hatsFile = new File(cosmeticsDir, "hats.yml");
        backblingFile = new File(cosmeticsDir, "backbling.yml");
        balloonsFile = new File(cosmeticsDir, "balloons.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Save default configurations if they don't exist
        if (!hatsFile.exists()) saveResource("cosmetics/hats.yml", false);
        if (!backblingFile.exists()) saveResource("cosmetics/backbling.yml", false);
        if (!balloonsFile.exists()) saveResource("cosmetics/balloons.yml", false);
        if (!messagesFile.exists()) saveResource("messages.yml", false);

        // Load configurations
        hatsConfig = YamlConfiguration.loadConfiguration(hatsFile);
        backblingConfig = YamlConfiguration.loadConfiguration(backblingFile);
        balloonsConfig = YamlConfiguration.loadConfiguration(balloonsFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = plugin.getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(plugin.getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(plugin.getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    public FileConfiguration getHatsConfig() {
        return hatsConfig;
    }

    public FileConfiguration getBackblingConfig() {
        return backblingConfig;
    }

    public FileConfiguration getBalloonsConfig() {
        return balloonsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public void reloadAllConfigs() {
        loadAllConfigs();
    }
}