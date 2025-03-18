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
    private final Map<UUID, ItemStack> glidingRemovedBackblings;

    public BackblingManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.activeBackblings = new HashMap<>();
        this.lastPlayerLocations = new HashMap<>();
        this.glidingRemovedBackblings = new HashMap<>();

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

    public void removeBackbling(UUID uuid) {
        lastPlayerLocations.remove(uuid);

        ItemDisplay backbling = activeBackblings.remove(uuid);
        if (backbling != null && backbling.isValid()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.removePassenger(backbling);
            }
            backbling.remove();
        }
    }

    private void updateAllBackblings() {
        // Create a copy of the entries to avoid concurrent modification
        new HashMap<>(activeBackblings).forEach((uuid, backbling) -> {
            Player player = Bukkit.getPlayer(uuid);

            // Remove invalid backblings or those whose players are offline
            if (!backbling.isValid() || player == null || !player.isOnline()) {
                removeBackbling(uuid);
                return;
            }

            // Validate from cosmetic inventory instead of chestplate
            ItemStack cosmeticBackbling = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBackbling(player);
            if (cosmeticBackbling == null || !ItemManager.isBackbling(cosmeticBackbling, configManager)) {
                removeBackbling(uuid);
                return;
            }

            // Handle teleports or large movements
            Location currentLoc = player.getLocation();
            Location lastLoc = lastPlayerLocations.get(uuid);

            if (lastLoc != null) {
                // Check if player has teleported or moved significantly
                boolean hasTeleported = currentLoc.getWorld() != lastLoc.getWorld();
                boolean hasMovedFar = currentLoc.distanceSquared(lastLoc) > 100; // > 10 blocks

                if (hasTeleported || hasMovedFar) {
                    // Always recreate the backbling on significant movements for proper attachment
                    removeBackbling(uuid);

                    // Use a slightly longer delay for stability
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            ItemStack newBackbling = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBackbling(player);
                            if (newBackbling != null && ItemManager.isBackbling(newBackbling, configManager)) {
                                createBackbling(player, newBackbling);
                            }
                        }
                    }, 3L);
                    return;
                }
            }

            // Check if the entity is actually riding the player
            // If not, recreate it (fixes stuck cosmetics)
            if (!player.getPassengers().contains(backbling)) {
                removeBackbling(uuid);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        ItemStack newBackbling = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBackbling(player);
                        if (newBackbling != null && ItemManager.isBackbling(newBackbling, configManager)) {
                            createBackbling(player, newBackbling);
                        }
                    }
                }, 2L);
                return;
            }

            // Update backbling rotation
            backbling.setRotation(currentLoc.getYaw(), 0.0f);

            // Store last location for next update
            lastPlayerLocations.put(uuid, currentLoc.clone());
        });
    }

    public void checkAndRestoreBackbling(Player player) {
        ItemStack backbling = ((GuppyCosmetics)plugin).getCosmeticInventoryManager().getBackbling(player);
        if (backbling != null && ItemManager.isBackbling(backbling, configManager)) {
            createBackbling(player, backbling);
        }
    }

    /**
     * Store backbling data when it's temporarily removed due to gliding
     * @param playerUuid The player's UUID
     * @param backblingItem The backbling item to store
     */
    public void storeBackblingForGliding(UUID playerUuid, ItemStack backblingItem) {
        glidingRemovedBackblings.put(playerUuid, backblingItem);
    }

    /**
     * Restore a backbling that was removed due to gliding
     * @param playerUuid The player's UUID
     */
    public void restoreBackblingAfterGliding(UUID playerUuid) {
        ItemStack backblingItem = glidingRemovedBackblings.remove(playerUuid);
        if (backblingItem != null) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                createBackbling(player, backblingItem);
            }
        }
    }

    /**
     * Check if a player has an active backbling
     * @param uuid The player's UUID
     * @return true if the player has a backbling, false otherwise
     */
    public boolean hasBackbling(UUID uuid) {
        return activeBackblings.containsKey(uuid);
    }

    public void shutdown() {
        // Remove all backblings on plugin disable
        new HashMap<>(activeBackblings).forEach((uuid, backbling) -> {
            removeBackbling(uuid);
        });
        lastPlayerLocations.clear();
        glidingRemovedBackblings.clear();
    }
}