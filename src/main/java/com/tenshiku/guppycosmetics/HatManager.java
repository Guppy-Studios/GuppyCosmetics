package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HatManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, String> activeOverlays;

    public HatManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.activeOverlays = new HashMap<>();
    }

    /**
     * Apply camera overlay for the given hat if it has one configured
     * @param player The player to apply the overlay to
     * @param hatItem The hat item being equipped
     */
    public void applyHatOverlay(Player player, ItemStack hatItem) {
        String itemId = ItemManager.getItemId(hatItem);
        if (itemId == null) return;

        // Check if this hat has an overlay configured
        String overlayPath = configManager.getHatsConfig().getString(itemId + ".overlay");
        if (overlayPath == null || overlayPath.isEmpty()) return;

        // Store the player's active overlay
        activeOverlays.put(player.getUniqueId(), overlayPath);

        // Get item model path
        String itemModelPath = configManager.getHatsConfig().getString(itemId + ".item_model", "minecraft:paper");

        // Apply overlay using command - transform regular hat into an overlay hat
        String materialStr = configManager.getHatsConfig().getString(itemId + ".material", "PAPER").toLowerCase();

        String command = String.format(
                "minecraft:give %s %s[minecraft:item_model=\"%s\",minecraft:equippable={slot:\"head\",equip_sound:\"block.glass.break\",camera_overlay:\"%s\",dispensable:true}]",
                player.getName(),
                materialStr,
                itemModelPath,
                overlayPath
        );

        // Log the command being executed
        plugin.getLogger().info("Executing overlay command: " + command);

        // Execute command and save player's current helmet
        ItemStack currentHelmet = player.getInventory().getHelmet();

        // Save original hat data to recover later
        if (currentHelmet != null && currentHelmet.hasItemMeta()) {
            ItemMeta meta = currentHelmet.getItemMeta();
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "original_hat_data"),
                    PersistentDataType.BYTE,
                    (byte)1
            );
            currentHelmet.setItemMeta(meta);
        }

        // Run the command to give the overlay item
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);

        // Schedule task to check if helmet changed and apply original helmet with overlay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove the regular hat from inventory that was just given
            ItemStack overlayHelmet = player.getInventory().getHelmet();
            if (overlayHelmet != null) {
                player.getInventory().setHelmet(null);

                // Remove the original hat from inventory if it exists
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(
                            new NamespacedKey(plugin, "original_hat_data"), PersistentDataType.BYTE)) {
                        player.getInventory().setItem(i, null);
                        break;
                    }
                }

                // Set the overlay hat
                player.getInventory().setHelmet(overlayHelmet);
            }
        }, 2L);

        plugin.getLogger().info("Applied overlay " + overlayPath + " for player " + player.getName());
    }

    /**
     * Remove the hat overlay for a player
     * @param player The player to remove the overlay from
     */
    public void removeHatOverlay(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeOverlays.containsKey(uuid)) {
            // Remove from tracking
            activeOverlays.remove(uuid);
            plugin.getLogger().info("Removed overlay for player " + player.getName());
        }
    }

    /**
     * Check if a player has changed or removed their hat, and update overlay accordingly
     * @param player The player to check
     */
    public void checkAndUpdateOverlay(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();

        // If player has no helmet but has an active overlay, remove it
        if (helmet == null || !ItemManager.isHat(helmet, configManager)) {
            if (activeOverlays.containsKey(player.getUniqueId())) {
                removeHatOverlay(player);
            }
            return;
        }

        // If player has a helmet, check if it's the right one for their active overlay
        String itemId = ItemManager.getItemId(helmet);
        if (itemId == null) {
            removeHatOverlay(player);
            return;
        }

        String configuredOverlay = configManager.getHatsConfig().getString(itemId + ".overlay");
        String activeOverlay = activeOverlays.get(player.getUniqueId());

        // If the overlays don't match, update it
        if ((configuredOverlay == null && activeOverlay != null) ||
                (configuredOverlay != null && !configuredOverlay.equals(activeOverlay))) {

            // Remove old overlay
            removeHatOverlay(player);

            // Apply new overlay if available
            if (configuredOverlay != null && !configuredOverlay.isEmpty()) {
                applyHatOverlay(player, helmet);
            }
        }
    }

    /**
     * Check and restore overlay effects for a player (used on login/world change)
     * @param player The player to restore effects for
     */
    public void checkAndRestoreHatOverlay(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet != null && ItemManager.isHat(helmet, configManager)) {
            String itemId = ItemManager.getItemId(helmet);
            if (itemId != null) {
                String overlayPath = configManager.getHatsConfig().getString(itemId + ".overlay");
                if (overlayPath != null && !overlayPath.isEmpty()) {
                    applyHatOverlay(player, helmet);
                }
            }
        }
    }

    /**
     * Clean up resources when the plugin is disabled
     */
    public void shutdown() {
        // Remove all overlays on plugin disable
        for (UUID uuid : new HashMap<>(activeOverlays).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                removeHatOverlay(player);
            }
        }
        activeOverlays.clear();
    }
}