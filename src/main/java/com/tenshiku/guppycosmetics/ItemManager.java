package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
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

        // Set item model (previously custom_model_data)
        if (config.contains(id + ".item_model")) {
            String itemModel = config.getString(id + ".item_model");
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_model"),
                    PersistentDataType.STRING,
                    itemModel
            );
        }
        // For backward compatibility
        else if (config.contains(id + ".custom_model_data")) {
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

        // Add overlay information to meta for later use
        if (type.equals("hat") && config.contains(id + ".overlay")) {
            String overlayPath = config.getString(id + ".overlay");
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "overlay_path"),
                    PersistentDataType.STRING,
                    overlayPath
            );
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

        // For hats with overlays, create a new item with the equippable component
        if (type.equals("hat") && config.contains(id + ".overlay")) {
            String overlayPath = config.getString(id + ".overlay");
            String itemModelStr = config.getString(id + ".item_model", "minecraft:paper");

            // Create command string similar to your vanilla command example
            String command = String.format(
                    "minecraft:give @p %s[minecraft:item_model=\"%s\",minecraft:equippable={slot:\"head\",equip_sound:\"block.glass.break\",camera_overlay:\"%s\",dispensable:true}]",
                    materialStr.toLowerCase(),
                    itemModelStr,
                    overlayPath
            );

            // Log the command for debugging
            GuppyCosmetics.getPlugin(GuppyCosmetics.class).getLogger().info("Attempting to create hat with overlay: " + command);

            // We'll just return the normal item, and let EventListener handle conversion
            // We don't actually run the command here since that would require a player context
        }

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

    public static String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey idKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_id");
        if (meta.getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        }
        return null;
    }

    public static String getItemModel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey modelKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "item_model");
        if (meta.getPersistentDataContainer().has(modelKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(modelKey, PersistentDataType.STRING);
        }
        return null;
    }

    public static String getOverlayPath(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();

        NamespacedKey overlayKey = new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "overlay_path");
        if (meta.getPersistentDataContainer().has(overlayKey, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(overlayKey, PersistentDataType.STRING);
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
        }

        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
}