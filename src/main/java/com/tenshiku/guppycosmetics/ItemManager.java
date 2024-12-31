package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.ArrayList;

public class ItemManager {

    public static ItemStack getItemById(String id, ConfigManager configManager) {
        FileConfiguration hatsConfig = configManager.getHatsConfig();
        FileConfiguration backblingConfig = configManager.getBackblingConfig();

        if (hatsConfig.contains(id)) {
            return createItem(hatsConfig, id, "hat");
        } else if (backblingConfig.contains(id)) {
            return createItem(backblingConfig, id, "backbling");
        }
        return null;
    }

    private static ItemStack createItem(FileConfiguration config, String id, String type) {
        String materialStr = config.getString(id + ".material", "AIR");
        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // Set the display name
        String name = config.getString(id + ".name", "");
        if (!name.isEmpty()) {
            meta.setDisplayName(ChatUtils.colorize(name));
        }

        // Set the lore
        List<String> lore = config.getStringList(id + ".lore");
        if (!lore.isEmpty()) {
            meta.setLore(ChatUtils.colorizeList(lore));
        }

        // Set custom model data as integer
        if (config.contains(id + ".custom_model_data")) {
            int modelData = config.getInt(id + ".custom_model_data");
            meta.setCustomModelData(modelData);
        }

        // Store the item ID and type in persistent data
        meta.getPersistentDataContainer().set(
                new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_id"),
                PersistentDataType.STRING,
                id
        );
        meta.getPersistentDataContainer().set(
                new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_type"),
                PersistentDataType.STRING,
                type
        );

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHat(ItemStack item, ConfigManager configManager) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey typeKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING).equals("hat");
    }

    public static boolean isBackbling(ItemStack item, ConfigManager configManager) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey typeKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING).equals("backbling");
    }

    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey idKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_id");
        if (meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        }
        return null;
    }
}