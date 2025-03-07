package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

        // Set item model using NamespacedKey (1.21 approach)
        String itemModelStr = config.getString(id + ".item_model");
        if (itemModelStr != null && !itemModelStr.isEmpty()) {
            // Parse the namespace:path format
            String[] modelParts = itemModelStr.split(":", 2);

            if (modelParts.length == 2) {
                String namespace = modelParts[0];
                String path = modelParts[1];
                NamespacedKey modelKey = new NamespacedKey(namespace, path);
                meta.setItemModel(modelKey);
            } else {
                // If no namespace is specified, use "minecraft" as default
                NamespacedKey modelKey = new NamespacedKey("minecraft", itemModelStr);
                meta.setItemModel(modelKey);
            }
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

        // For hats, also store overlay information if present
        if (type.equals("hat") && config.contains(id + ".overlay")) {
            String overlayPath = config.getString(id + ".overlay");
            if (overlayPath != null && !overlayPath.isEmpty()) {
                meta.getPersistentDataContainer().set(
                        new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "has_overlay"),
                        PersistentDataType.BYTE,
                        (byte)1
                );

                meta.getPersistentDataContainer().set(
                        new NamespacedKey(GuppyCosmetics.getPlugin(GuppyCosmetics.class), "overlay_path"),
                        PersistentDataType.STRING,
                        overlayPath
                );
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Apply camera overlay component to a hat item
     * @param player The player to give the item to
     * @param itemId The item ID from configuration
     * @param configManager The config manager
     * @return True if successful, false otherwise
     */
    public static boolean giveHatWithOverlay(Player player, String itemId, ConfigManager configManager) {
        if (!configManager.getHatsConfig().contains(itemId)) {
            return false;
        }

        String materialStr = configManager.getHatsConfig().getString(itemId + ".material", "AIR");
        Material material = Material.getMaterial(materialStr.toUpperCase());
        if (material == null) return false;

        String overlayPath = configManager.getHatsConfig().getString(itemId + ".overlay");
        if (overlayPath == null || overlayPath.isEmpty()) {
            return false; // No overlay configured
        }

        String itemModelStr = configManager.getHatsConfig().getString(itemId + ".item_model");
        if (itemModelStr == null || itemModelStr.isEmpty()) {
            itemModelStr = "minecraft:leather_horse_armor"; // Default model
        }

        // Parse overlay path to ensure proper format
        if (!overlayPath.contains(":")) {
            overlayPath = "minecraft:" + overlayPath;
        }

        // Create command with just the essential components
        String command = "minecraft:give " + player.getName() +
                " " + material.toString().toLowerCase() +
                "[minecraft:item_model=\"" + itemModelStr + "\"" +
                ",minecraft:equippable={slot:\"head\",camera_overlay:\"" + overlayPath + "\",dispensable:true}] 1";

        GuppyCosmetics plugin = GuppyCosmetics.getPlugin(GuppyCosmetics.class);

        // Execute the command to create the overlay item
        if (Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
            // Find the newly created item in player's inventory and fix its metadata
            String finalOverlayPath = overlayPath;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                boolean found = false;

                // Look through the player's inventory for the newly created item
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == material) {
                        // Check if this is our newly created item by checking if it has the equippable component
                        // We can't directly check for components in the API, but we can check if it doesn't have our custom data
                        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(
                                new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING)) {

                            // This is likely our new item - apply our custom metadata
                            ItemMeta meta = item.getItemMeta();

                            // Set the display name using MiniMessage
                            String name = configManager.getHatsConfig().getString(itemId + ".name", "");
                            if (!name.isEmpty()) {
                                meta.displayName(ChatUtils.format(name));
                            }

                            // Set the lore using MiniMessage
                            List<String> lore = configManager.getHatsConfig().getStringList(itemId + ".lore");
                            if (!lore.isEmpty()) {
                                meta.lore(ChatUtils.formatList(lore));
                            }

                            // Add all item flags to try to hide every tooltip
                            meta.addItemFlags(
                                    ItemFlag.HIDE_ATTRIBUTES,
                                    ItemFlag.HIDE_ARMOR_TRIM,
                                    ItemFlag.HIDE_DESTROYS,
                                    ItemFlag.HIDE_DYE,
                                    ItemFlag.HIDE_ENCHANTS,
                                    ItemFlag.HIDE_PLACED_ON,
                                    ItemFlag.HIDE_STORED_ENCHANTS,
                                    ItemFlag.HIDE_UNBREAKABLE
                            );

                            // Store the item ID and type in persistent data
                            meta.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "item_id"),
                                    PersistentDataType.STRING,
                                    itemId
                            );
                            meta.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "item_type"),
                                    PersistentDataType.STRING,
                                    "hat"
                            );

                            // Store overlay information
                            meta.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "has_overlay"),
                                    PersistentDataType.BYTE,
                                    (byte)1
                            );
                            meta.getPersistentDataContainer().set(
                                    new NamespacedKey(plugin, "overlay_path"),
                                    PersistentDataType.STRING,
                                    finalOverlayPath
                            );

                            // Manually set armor attribute to 0 using Bukkit API - this is key!
                            try {
                                // Remove existing modifiers for cleaner replacement
                                org.bukkit.attribute.Attribute armorAttribute = Attribute.ARMOR;
                                if (meta.hasAttributeModifiers() && meta.getAttributeModifiers(armorAttribute) != null) {
                                    for (org.bukkit.attribute.AttributeModifier modifier : meta.getAttributeModifiers(armorAttribute)) {
                                        meta.removeAttributeModifier(armorAttribute, modifier);
                                    }
                                }

                                // Add a zero armor modifier with a unique UUID
                                meta.addAttributeModifier(
                                        armorAttribute,
                                        new org.bukkit.attribute.AttributeModifier(
                                                java.util.UUID.randomUUID(), // Generate a unique UUID
                                                "NoArmorModifier",
                                                0.0,
                                                org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                                                org.bukkit.inventory.EquipmentSlot.HEAD
                                        )
                                );
                            } catch (Exception e) {
                                plugin.getLogger().warning("Error setting armor attribute: " + e.getMessage());
                            }

                            // Apply the updated metadata
                            item.setItemMeta(meta);
                            found = true;
                            break;
                        }
                    }
                }

                // If we couldn't find the item, it might have been auto-equipped to the head slot
                if (!found && player.getInventory().getHelmet() != null) {
                    ItemStack helmet = player.getInventory().getHelmet();
                    if (helmet.getType() == material &&
                            (!helmet.hasItemMeta() || !helmet.getItemMeta().getPersistentDataContainer().has(
                                    new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING))) {

                        // Apply the same metadata updates to the helmet
                        ItemMeta meta = helmet.getItemMeta();

                        // Set the display name using MiniMessage
                        String name = configManager.getHatsConfig().getString(itemId + ".name", "");
                        if (!name.isEmpty()) {
                            meta.displayName(ChatUtils.format(name));
                        }

                        // Set the lore using MiniMessage
                        List<String> lore = configManager.getHatsConfig().getStringList(itemId + ".lore");
                        if (!lore.isEmpty()) {
                            meta.lore(ChatUtils.formatList(lore));
                        }

                        // Add all item flags to try to hide every tooltip
                        meta.addItemFlags(
                                ItemFlag.HIDE_ATTRIBUTES,
                                ItemFlag.HIDE_ARMOR_TRIM,
                                ItemFlag.HIDE_DESTROYS,
                                ItemFlag.HIDE_DYE,
                                ItemFlag.HIDE_ENCHANTS,
                                ItemFlag.HIDE_PLACED_ON,
                                ItemFlag.HIDE_STORED_ENCHANTS,
                                ItemFlag.HIDE_UNBREAKABLE
                        );

                        // Store the item ID and type in persistent data
                        meta.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "item_id"),
                                PersistentDataType.STRING,
                                itemId
                        );
                        meta.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "item_type"),
                                PersistentDataType.STRING,
                                "hat"
                        );

                        // Store overlay information
                        meta.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "has_overlay"),
                                PersistentDataType.BYTE,
                                (byte)1
                        );
                        meta.getPersistentDataContainer().set(
                                new NamespacedKey(plugin, "overlay_path"),
                                PersistentDataType.STRING,
                                finalOverlayPath
                        );

                        // Manually set armor attribute to 0 using Bukkit API - this is key!
                        try {
                            // Remove existing modifiers for cleaner replacement
                            org.bukkit.attribute.Attribute armorAttribute = Attribute.ARMOR;
                            if (meta.hasAttributeModifiers() && meta.getAttributeModifiers(armorAttribute) != null) {
                                for (org.bukkit.attribute.AttributeModifier modifier : meta.getAttributeModifiers(armorAttribute)) {
                                    meta.removeAttributeModifier(armorAttribute, modifier);
                                }
                            }

                            // Add a zero armor modifier with a unique UUID
                            meta.addAttributeModifier(
                                    armorAttribute,
                                    new org.bukkit.attribute.AttributeModifier(
                                            java.util.UUID.randomUUID(), // Generate a unique UUID
                                            "NoArmorModifier",
                                            0.0,
                                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                                            org.bukkit.inventory.EquipmentSlot.HEAD
                                    )
                            );
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error setting armor attribute: " + e.getMessage());
                        }

                        // Apply the updated metadata
                        helmet.setItemMeta(meta);
                    }
                }
            }, 2L); // Short delay to ensure item is in inventory

            return true;
        }

        return false;
    }

    /**
     * Checks if an item has an overlay in its config
     * @param itemId The item ID to check
     * @param configManager The config manager
     * @return true if the item has an overlay, false otherwise
     */
    public static boolean hasOverlayInConfig(String itemId, ConfigManager configManager) {
        if (itemId == null) return false;

        // Check only hats config
        if (configManager.getHatsConfig().contains(itemId)) {
            String overlay = configManager.getHatsConfig().getString(itemId + ".overlay");
            return overlay != null && !overlay.isEmpty();
        }

        return false;
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