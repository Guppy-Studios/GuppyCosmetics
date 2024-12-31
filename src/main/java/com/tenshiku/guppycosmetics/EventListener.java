package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;

public class EventListener implements Listener {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;

    public EventListener(GuppyCosmetics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;

        if (ItemManager.isHat(item, configManager)) {
            equipHat(player, item);
            String message = configManager.getMessagesConfig().getString("equipped-message", "&aYou equipped {item}!");
            message = message.replace("{item}", item.getItemMeta().getDisplayName());
            player.sendMessage(ChatUtils.colorize(message));
        } else if (ItemManager.isBackbling(item, configManager)) {
            equipBackblingToChestplate(player, item);
            String message = configManager.getMessagesConfig().getString("equipped-message", "&aYou equipped {item}!");
            message = message.replace("{item}", item.getItemMeta().getDisplayName());
            player.sendMessage(ChatUtils.colorize(message));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeExistingBackbling(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Schedule the check to run after the player fully loads
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            ItemStack chestplate = player.getInventory().getChestplate();

            if (chestplate != null && ItemManager.isBackbling(chestplate, configManager)) {
                // Recreate the backbling display
                String itemId = ItemManager.getItemId(chestplate);
                if (itemId != null) {
                    // Get position from config
                    double offsetX = configManager.getBackblingConfig().getDouble(itemId + ".position.x", 0.0);
                    double offsetY = configManager.getBackblingConfig().getDouble(itemId + ".position.y", 0.4);
                    double offsetZ = configManager.getBackblingConfig().getDouble(itemId + ".position.z", 0.2);

                    // Create the display entity
                    ItemDisplay backbling = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, (display) -> {
                        display.setItemStack(chestplate);
                        display.setGravity(false);
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

                    // Update rotation task
                    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                        if (!backbling.isValid() || !player.isOnline()) {
                            return;
                        }
                        Location location = player.getLocation();
                        backbling.setRotation(location.getYaw(), 0.0f);
                    }, 0L, 1L);

                    backbling.setMetadata("taskId", new FixedMetadataValue(plugin, task.getTaskId()));
                }
            }
        }, 5L); // 5 tick delay to ensure player is fully loaded
    }

    private void equipHat(Player player, ItemStack item) {
        if (player.getInventory().getHelmet() != null) {
            player.getInventory().addItem(player.getInventory().getHelmet());
        }
        player.getInventory().setHelmet(item.clone());
        player.getInventory().removeItem(item);
    }

    private void equipBackblingToChestplate(Player player, ItemStack item) {
        // Check if player already has a chestplate
        ItemStack currentChestplate = player.getInventory().getChestplate();
        if (currentChestplate != null) {
            player.getInventory().addItem(currentChestplate);
        }

        // Remove existing backbling if any
        removeExistingBackbling(player);

        // Set the backbling item as the chestplate
        player.getInventory().setChestplate(item.clone());
        player.getInventory().removeItem(item);

        String itemId = ItemManager.getItemId(item);
        if (itemId == null) return;

        // Get position from config
        double offsetX = configManager.getBackblingConfig().getDouble(itemId + ".position.x", 0.0);
        double offsetY = configManager.getBackblingConfig().getDouble(itemId + ".position.y", 0.4);
        double offsetZ = configManager.getBackblingConfig().getDouble(itemId + ".position.z", 0.2);

        // Create the display entity
        ItemDisplay backbling = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, (display) -> {
            display.setItemStack(item);
            display.setGravity(false);
            display.setCustomName("Backbling:" + player.getUniqueId());
            display.setCustomNameVisible(false);

            // Apply position offset using transformation
            Transformation transformation = display.getTransformation();
            transformation.getTranslation().set((float)offsetX, (float)offsetY, (float)offsetZ);
            display.setTransformation(transformation);

            // Store the original item ID
            if (itemId != null) {
                display.setMetadata("itemId", new FixedMetadataValue(plugin, itemId));
            }
        });

        // Make the backbling ride the player
        player.addPassenger(backbling);

        // Update rotation task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!backbling.isValid() || !player.isOnline()) {
                return;
            }
            Location location = player.getLocation();
            backbling.setRotation(location.getYaw(), 0.0f);
        }, 0L, 1L);

        backbling.setMetadata("taskId", new FixedMetadataValue(plugin, task.getTaskId()));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Handle armor slot clicks
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                // Check if the clicked item is a backbling
                if (ItemManager.isBackbling(item, configManager)) {
                    // Don't cancel the event - let the player remove the item
                    removeExistingBackbling(player);
                }
            }
        }
    }

    private void removeExistingBackbling(Player player) {
        // Remove display entity
        for (Entity passenger : player.getPassengers()) {
            if (passenger instanceof ItemDisplay &&
                    passenger.getCustomName() != null &&
                    passenger.getCustomName().equals("Backbling:" + player.getUniqueId())) {

                // Cancel the update task if it exists
                if (passenger.hasMetadata("taskId")) {
                    Bukkit.getScheduler().cancelTask(
                            passenger.getMetadata("taskId").get(0).asInt()
                    );
                }

                player.removePassenger(passenger);
                passenger.remove();
            }
        }
    }
}