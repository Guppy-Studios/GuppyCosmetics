package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import java.util.List;

public class ItemManager {

    public static ItemStack getItemById(String id, ConfigManager configManager) {
        if (configManager.getHatsConfig().contains(id)) {
            return createItem(configManager.getHatsConfig(), id, "hat");
        } else if (configManager.getBackblingConfig().contains(id)) {
            return createItem(configManager.getBackblingConfig(), id, "backbling");
        } else if (configManager.getBalloonsConfig().contains(id)) {
            return createItem(configManager.getBalloonsConfig(), id, "balloon");
        } else if (configManager.getItemsConfig().contains(id)) {
            return createItem(configManager.getItemsConfig(), id, "item");
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

        // Set the display name using MiniMessage
        String name = config.getString(id + ".name", "");
        if (!name.isEmpty()) {
            meta.displayName(ChatUtils.format(name));
        }

        // Set the lore using MiniMessage
        List<String> lore = config.getStringList(id + ".lore");
        if (!lore.isEmpty()) {
            meta.lore(ChatUtils.formatList(lore));
        }

        // Set custom model data
        if (config.contains(id + ".custom_model_data")) {
            int modelData = config.getInt(id + ".custom_model_data");
            meta.setCustomModelData(modelData);
        }

        // Hide all possible item flags
        meta.addItemFlags(
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                ItemFlag.HIDE_ARMOR_TRIM,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_DESTROYS,
                ItemFlag.HIDE_DYE,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON,
                ItemFlag.HIDE_STORED_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        );

        // Remove armor attribute for armor items
        if (material.name().contains("LEATHER") || material.name().endsWith("_HELMET")
                || material.name().endsWith("_CHESTPLATE") || material.name().endsWith("_LEGGINGS")
                || material.name().endsWith("_BOOTS") || material.name().endsWith("_HORSE_ARMOR")) {
            meta.addAttributeModifier(org.bukkit.attribute.Attribute.ARMOR,
                    new org.bukkit.attribute.AttributeModifier("armor", 0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER));
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

    public static boolean isBalloon(ItemStack item, ConfigManager configManager) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey typeKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING).equals("balloon");
    }

    public static boolean isGeneralItem(ItemStack item, ConfigManager configManager) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey typeKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_type");
        return meta.getPersistentDataContainer().has(typeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING).equals("item");
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

    public static boolean hasPermission(Player player, String itemId, ConfigManager configManager) {
        String permission = null;

        // Check all config files
        if (configManager.getHatsConfig().contains(itemId)) {
            permission = configManager.getHatsConfig().getString(itemId + ".permission");
        } else if (configManager.getBackblingConfig().contains(itemId)) {
            permission = configManager.getBackblingConfig().getString(itemId + ".permission");
        } else if (configManager.getBalloonsConfig().contains(itemId)) {
            permission = configManager.getBalloonsConfig().getString(itemId + ".permission");
        } else if (configManager.getItemsConfig().contains(itemId)) {
            permission = configManager.getItemsConfig().getString(itemId + ".permission");
        }

        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
}