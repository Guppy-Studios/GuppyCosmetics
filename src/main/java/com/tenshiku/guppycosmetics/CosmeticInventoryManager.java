package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CosmeticInventoryManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Inventory> playerCosmeticInventories;

    // Inventory slot constants
    public static final int BACKBLING_SLOT = 3;
    public static final int BALLOON_SLOT = 5;
    private static final String INVENTORY_TITLE = "Cosmetics";

    public CosmeticInventoryManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerCosmeticInventories = new HashMap<>();
    }

    public Inventory getCosmeticInventory(Player player) {
        UUID playerId = player.getUniqueId();

        // Create a new inventory if one doesn't exist
        if (!playerCosmeticInventories.containsKey(playerId)) {
            Inventory inventory = Bukkit.createInventory(null, 9, INVENTORY_TITLE);

            // Create glass pane placeholders
            ItemStack backblingPane = createPlaceholderPane(Material.BLUE_STAINED_GLASS_PANE, "Backbling Slot");
            ItemStack balloonPane = createPlaceholderPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "Balloon Slot");
            ItemStack emptyPane = createPlaceholderPane(Material.BLACK_STAINED_GLASS_PANE, "");

            // Fill inventory with empty panes
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, emptyPane);
            }

            // Set the specific slot placeholders
            inventory.setItem(BACKBLING_SLOT, backblingPane);
            inventory.setItem(BALLOON_SLOT, balloonPane);

            playerCosmeticInventories.put(playerId, inventory);
            return inventory;
        }

        return playerCosmeticInventories.get(playerId);
    }

    public void openCosmeticInventory(Player player) {
        player.openInventory(getCosmeticInventory(player));
    }

    private ItemStack createPlaceholderPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GRAY + name);
            // Add persistent data to identify it as a placeholder
            meta.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "cosmetic_placeholder"),
                    PersistentDataType.STRING,
                    "true"
            );
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public boolean isPlaceholderPane(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "cosmetic_placeholder"),
                PersistentDataType.STRING
        );
    }

    // Methods to handle cosmetic inventory management
    public ItemStack getBackbling(Player player) {
        Inventory cosmeticInventory = getCosmeticInventory(player);
        ItemStack item = cosmeticInventory.getItem(BACKBLING_SLOT);

        // Return null if the item is a placeholder or doesn't exist
        if (item == null || isPlaceholderPane(item)) {
            return null;
        }

        return item;
    }

    public ItemStack getBalloon(Player player) {
        Inventory cosmeticInventory = getCosmeticInventory(player);
        ItemStack item = cosmeticInventory.getItem(BALLOON_SLOT);

        // Return null if the item is a placeholder or doesn't exist
        if (item == null || isPlaceholderPane(item)) {
            return null;
        }

        return item;
    }

    public void setBackbling(Player player, ItemStack backbling) {
        Inventory cosmeticInventory = getCosmeticInventory(player);

        // Get existing backbling first
        ItemStack currentBackbling = getBackbling(player);

        // If there's an existing backbling, return it to the player's inventory
        if (currentBackbling != null) {
            player.getInventory().addItem(currentBackbling);
        }

        // Set the new backbling
        cosmeticInventory.setItem(BACKBLING_SLOT, backbling);
    }

    public void setBalloon(Player player, ItemStack balloon) {
        Inventory cosmeticInventory = getCosmeticInventory(player);

        // Get existing balloon first
        ItemStack currentBalloon = getBalloon(player);

        // If there's an existing balloon, return it to the player's inventory
        if (currentBalloon != null) {
            player.getInventory().addItem(currentBalloon);
        }

        // Set the new balloon
        cosmeticInventory.setItem(BALLOON_SLOT, balloon);
    }

    public void removeBackbling(Player player) {
        Inventory cosmeticInventory = getCosmeticInventory(player);
        ItemStack currentBackbling = getBackbling(player);

        // If there's an existing backbling, return it to the player's inventory
        if (currentBackbling != null) {
            player.getInventory().addItem(currentBackbling);
        }

        // Replace with placeholder
        cosmeticInventory.setItem(BACKBLING_SLOT, createPlaceholderPane(Material.BLUE_STAINED_GLASS_PANE, "Backbling Slot"));
    }

    public void removeBalloon(Player player) {
        Inventory cosmeticInventory = getCosmeticInventory(player);
        ItemStack currentBalloon = getBalloon(player);

        // If there's an existing balloon, return it to the player's inventory
        if (currentBalloon != null) {
            player.getInventory().addItem(currentBalloon);
        }

        // Replace with placeholder
        cosmeticInventory.setItem(BALLOON_SLOT, createPlaceholderPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "Balloon Slot"));
    }

    public void onPlayerQuit(UUID playerId) {
        playerCosmeticInventories.remove(playerId);
    }

    public void savePlayerCosmetics(Player player) {
        // Get player's UUID as a string for storage
        String uuid = player.getUniqueId().toString();

        // Get cosmetic items
        ItemStack backbling = getBackbling(player);
        ItemStack balloon = getBalloon(player);

        // Save in config (create a "players.yml" file for this)
        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "players.yml"));

        // Clear existing data for this player
        playerConfig.set("players." + uuid, null);

        // If there are cosmetics to save
        if (backbling != null || balloon != null) {
            if (backbling != null) {
                playerConfig.set("players." + uuid + ".backbling", backbling);
            }
            if (balloon != null) {
                playerConfig.set("players." + uuid + ".balloon", balloon);
            }

            // Save the file
            try {
                playerConfig.save(new File(plugin.getDataFolder(), "players.yml"));
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save cosmetic data for player: " + player.getName());
                e.printStackTrace();
            }
        }
    }

    public void loadPlayerCosmetics(Player player) {
        // Get player's UUID as a string for storage
        String uuid = player.getUniqueId().toString();

        // Load from config
        File configFile = new File(plugin.getDataFolder(), "players.yml");
        if (!configFile.exists()) {
            return;  // No saved data yet
        }

        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(configFile);

        // Check if player has saved cosmetics
        if (playerConfig.contains("players." + uuid)) {
            ItemStack backbling = playerConfig.getItemStack("players." + uuid + ".backbling");
            ItemStack balloon = playerConfig.getItemStack("players." + uuid + ".balloon");

            // Get the cosmetic inventory and set items
            Inventory cosmeticInventory = getCosmeticInventory(player);

            if (backbling != null) {
                cosmeticInventory.setItem(BACKBLING_SLOT, backbling);
            }

            if (balloon != null) {
                cosmeticInventory.setItem(BALLOON_SLOT, balloon);
            }
        }
    }
}