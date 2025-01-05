package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BackblingManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, ItemDisplay> activeBackblings;
    private final Map<UUID, Location> lastPlayerLocations;

    public BackblingManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.activeBackblings = new HashMap<>();
        this.lastPlayerLocations = new HashMap<>();

        // Start the single update task for all backblings
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllBackblings, 0L, 1L);
    }

    public void createBackbling(Player player, ItemStack backblingItem) {
        String itemId = ItemManager.getItemId(backblingItem);
        if (itemId == null) return;

        // Remove any existing backbling first
        removeBackbling(player.getUniqueId());

        // Get position from config
        double offsetX = configManager.getBackblingConfig().getDouble(itemId + ".position.x", 0.0);
        double offsetY = configManager.getBackblingConfig().getDouble(itemId + ".position.y", 0.4);
        double offsetZ = configManager.getBackblingConfig().getDouble(itemId + ".position.z", 0.2);

        // Create the display entity
        ItemDisplay backbling = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, (display) -> {
            display.setItemStack(backblingItem);
            display.setCustomName("Backbling:" + player.getUniqueId());
            display.setCustomNameVisible(false);

            // Apply position offset using transformation
            Transformation transformation = display.getTransformation();
            transformation.getTranslation().set((float)offsetX, (float)offsetY, (float)offsetZ);
            display.setTransformation(transformation);

            // Store the original item ID
            display.setMetadata("itemId", new FixedMetadataValue(plugin, itemId));
        });

        // Make the backbling ride the player
        player.addPassenger(backbling);

        // Store in our tracking map
        activeBackblings.put(player.getUniqueId(), backbling);
        lastPlayerLocations.put(player.getUniqueId(), player.getLocation());
    }

    public void removeBackbling(UUID playerId) {
        lastPlayerLocations.remove(playerId);

        ItemDisplay backbling = activeBackblings.remove(playerId);
        if (backbling != null && backbling.isValid()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.removePassenger(backbling);
            }
            backbling.remove();
        }
    }

    private void updateAllBackblings() {
        // Create a copy of the entries to avoid concurrent modification
        new HashMap<>(activeBackblings).forEach((playerId, backbling) -> {
            Player player = Bukkit.getPlayer(playerId);

            // Remove invalid backblings or those whose players are offline
            if (!backbling.isValid() || player == null || !player.isOnline()) {
                removeBackbling(playerId);
                return;
            }

            // Validate chestplate
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate == null || !ItemManager.isBackbling(chestplate, configManager)) {
                removeBackbling(playerId);
                return;
            }

            // Handle teleports or large movements
            Location currentLoc = player.getLocation();
            Location lastLoc = lastPlayerLocations.get(playerId);

            if (lastLoc != null && (currentLoc.getWorld() != lastLoc.getWorld() ||
                    currentLoc.distanceSquared(lastLoc) > 100)) { // Distance > 10 blocks
                removeBackbling(playerId);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        ItemStack newChestplate = player.getInventory().getChestplate();
                        if (newChestplate != null && ItemManager.isBackbling(newChestplate, configManager)) {
                            createBackbling(player, newChestplate);
                        }
                    }
                }, 2L);
                return;
            }

            // Update backbling rotation
            backbling.setRotation(currentLoc.getYaw(), 0.0f);

            // Store last location for next update
            lastPlayerLocations.put(playerId, currentLoc.clone());
        });
    }

    public void checkAndRestoreBackbling(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate != null && ItemManager.isBackbling(chestplate, configManager)) {
            createBackbling(player, chestplate);
        }
    }

    public void shutdown() {
        // Remove all backblings on plugin disable
        new HashMap<>(activeBackblings).forEach((playerId, backbling) -> {
            removeBackbling(playerId);
        });
        lastPlayerLocations.clear();
    }
}