package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EventListener implements Listener {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;
    private final BackblingManager backblingManager;
    private final BalloonManager balloonManager;
    private final HatManager hatManager;

    // Map to track player's previous helmet for change detection
    private final Map<UUID, ItemStack> previousHelmetMap = new HashMap<>();

    public EventListener(GuppyCosmetics plugin, ConfigManager configManager,
                         BackblingManager backblingManager, BalloonManager balloonManager,
                         HatManager hatManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.backblingManager = backblingManager;
        this.balloonManager = balloonManager;
        this.hatManager = hatManager;
    }

    private String getPrefix() {
        return configManager.getMessagesConfig().getString("prefix", "");
    }

    private String getItemName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return "Unknown Item";
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String itemId = ItemManager.getItemId(item);
        if (itemId == null) return;

        // Cancel the event for our cosmetic items
        event.setCancelled(true);

        // Check permission before equipping
        if (!ItemManager.hasPermission(player, itemId, configManager)) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("no-permission-item")
                    .replace("{item_id}", itemId);
            player.sendMessage(ChatUtils.format(message));
            return;
        }

        if (ItemManager.isHat(item, configManager)) {
            equipHat(player, item);
            String message = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                    .replace("{item}", getItemName(item));
            player.sendMessage(ChatUtils.format(message));
        } else if (ItemManager.isBackbling(item, configManager)) {
            // Instead of equipping to chestplate, use the cosmetic inventory
            plugin.getCosmeticInventoryManager().setBackbling(player, item.clone());
            backblingManager.createBackbling(player, item);
            player.getInventory().removeItem(item);
            String message = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                    .replace("{item}", getItemName(item));
            player.sendMessage(ChatUtils.format(message));
        } else if (ItemManager.isBalloon(item, configManager)) {
            // Instead of equipping to leggings, use the cosmetic inventory
            plugin.getCosmeticInventoryManager().setBalloon(player, item.clone());
            balloonManager.createBalloon(player, item);
            player.getInventory().removeItem(item);
            String message = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                    .replace("{item}", getItemName(item));
            player.sendMessage(ChatUtils.format(message));
        }
    }

    @EventHandler
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = event.getMainHandItem();  // Item going to main hand
        ItemStack offHandItem = event.getOffHandItem();    // Item going to off hand

        // If we're swapping a balloon item out of the off-hand
        if (offHandItem != null && ItemManager.isBalloon(offHandItem, configManager)) {
            balloonManager.removeBalloon(player.getUniqueId());
        }

        // If we're swapping a balloon item into the off-hand
        if (mainHandItem != null && ItemManager.isBalloon(mainHandItem, configManager)) {
            // Let the swap happen, then create the balloon next tick
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                balloonManager.createBalloon(player, mainHandItem);
            });
        }
    }

    // When inventory is clicked, check for helmet changes
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Check if it's our cosmetic inventory
        if (event.getView().getTitle().equals("Cosmetics")) {
            event.setCancelled(true); // Cancel all interactions with cosmetic inventory by default

            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();

            // Handle removing an item
            if (clickedItem != null && !plugin.getCosmeticInventoryManager().isPlaceholderPane(clickedItem) &&
                    (cursorItem == null || cursorItem.getType() == Material.AIR)) {
                // They're picking up an item
                if (event.getSlot() == CosmeticInventoryManager.BACKBLING_SLOT) {
                    plugin.getCosmeticInventoryManager().removeBackbling(player);
                    backblingManager.removeBackbling(player.getUniqueId());
                } else if (event.getSlot() == CosmeticInventoryManager.BALLOON_SLOT) {
                    plugin.getCosmeticInventoryManager().removeBalloon(player);
                    balloonManager.removeBalloon(player.getUniqueId());
                }
            }
            // Handle placing an item
            else if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                if (event.getSlot() == CosmeticInventoryManager.BACKBLING_SLOT &&
                        ItemManager.isBackbling(cursorItem, configManager)) {
                    plugin.getCosmeticInventoryManager().setBackbling(player, cursorItem.clone());
                    backblingManager.createBackbling(player, cursorItem);
                    player.setItemOnCursor(null);
                } else if (event.getSlot() == CosmeticInventoryManager.BALLOON_SLOT &&
                        ItemManager.isBalloon(cursorItem, configManager)) {
                    plugin.getCosmeticInventoryManager().setBalloon(player, cursorItem.clone());
                    balloonManager.createBalloon(player, cursorItem);
                    player.setItemOnCursor(null);
                }
            }
            return;
        }

        // Check cursor item (item being placed)
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            String itemId = ItemManager.getItemId(cursorItem);
            if (itemId != null) {
                // Only handle hat slots now, since backbling and balloon use cosmetic inventory
                if (ItemManager.isHat(cursorItem, configManager) && event.getRawSlot() == 39) {
                    // Check permission before allowing equip
                    if (!ItemManager.hasPermission(player, itemId, configManager)) {
                        event.setCancelled(true);
                        String message = getPrefix() + configManager.getMessagesConfig().getString("no-permission-item")
                                .replace("{item_id}", itemId);
                        player.sendMessage(ChatUtils.format(message));
                        return;
                    }

                    // Schedule overlay check for next tick when inventory changes are complete
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        checkHelmetChange(player);
                    });
                }
            }
        }

        // Handle current slot item (item being taken)
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && !currentItem.getType().isAir()) {
            // If removing a hat, check for overlay removal
            if (event.getSlot() == 39 && ItemManager.isHat(currentItem, configManager)) {
                // Schedule overlay removal for next tick
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    checkHelmetChange(player);
                });
            }
        }

        // For any inventory click, check after a delay to see if helmet changed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkHelmetChange(player);
        }, 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // When inventory is closed, check if helmet has changed
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkHelmetChange(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        // When an item is dropped, check if it was their helmet
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkHelmetChange(player);
        }, 1L);
    }

    // Helper method to check if a player's helmet has changed
    private void checkHelmetChange(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack currentHelmet = player.getInventory().getHelmet();
        ItemStack previousHelmet = previousHelmetMap.get(uuid);

        // Check for a change
        boolean changed = false;

        // If previous was null but current isn't, or vice versa
        if ((previousHelmet == null && currentHelmet != null) ||
                (previousHelmet != null && currentHelmet == null)) {
            changed = true;
        }
        // If both exist but are different items
        else if (previousHelmet != null && currentHelmet != null) {
            if (!previousHelmet.equals(currentHelmet)) {
                changed = true;
            }
        }

        // If there was a change, update overlay
        if (changed) {
            // If old helmet was a hat with overlay, remove it
            if (previousHelmet != null && ItemManager.isHat(previousHelmet, configManager)) {
                hatManager.removeHatOverlay(player);
            }

            // If new helmet is a hat with overlay, apply it
            if (currentHelmet != null && ItemManager.isHat(currentHelmet, configManager)) {
                String itemId = ItemManager.getItemId(currentHelmet);
                if (itemId != null) {
                    String overlayPath = configManager.getHatsConfig().getString(itemId + ".overlay");
                    if (overlayPath != null && !overlayPath.isEmpty()) {
                        hatManager.applyHatOverlay(player, currentHelmet);
                    }
                }
            }

            // Update previous helmet
            previousHelmetMap.put(uuid, currentHelmet == null ? null : currentHelmet.clone());
        }
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack brokenItem = event.getBrokenItem();

        // If a hat breaks, check if it had an overlay
        if (ItemManager.isHat(brokenItem, configManager)) {
            hatManager.removeHatOverlay(player);

            // Update previous helmet record
            previousHelmetMap.put(player.getUniqueId(), null);
        }
    }

    private void equipHat(Player player, ItemStack item) {
        // Store previous helmet
        ItemStack previousHelmet = player.getInventory().getHelmet();
        previousHelmetMap.put(player.getUniqueId(), previousHelmet);

        // If current helmet exists, add to inventory
        if (previousHelmet != null) {
            player.getInventory().addItem(previousHelmet);
        }

        // Set the new hat and remove the item from inventory
        player.getInventory().setHelmet(item.clone());
        player.getInventory().removeItem(item);

        // Check if this hat has an overlay and apply it if so
        String itemId = ItemManager.getItemId(item);
        if (itemId != null) {
            String overlayPath = configManager.getHatsConfig().getString(itemId + ".overlay");
            if (overlayPath != null && !overlayPath.isEmpty()) {
                hatManager.applyHatOverlay(player, item);
            }
        }

        // Update previous helmet record
        previousHelmetMap.put(player.getUniqueId(), item.clone());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Save player cosmetics data first
        plugin.getCosmeticInventoryManager().savePlayerCosmetics(player);

        // Then remove visual entities and effects
        backblingManager.removeBackbling(uuid);
        balloonManager.removeBalloon(uuid);
        hatManager.removeHatOverlay(player);

        // Clean up inventories and tracking
        plugin.getCosmeticInventoryManager().onPlayerQuit(uuid);
        previousHelmetMap.remove(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();

            // First, load the saved cosmetics from file
            plugin.getCosmeticInventoryManager().loadPlayerCosmetics(player);

            // Then restore visual entities and effects
            backblingManager.checkAndRestoreBackbling(player);
            balloonManager.checkAndRestoreBalloon(player);
            hatManager.checkAndRestoreHatOverlay(player);

            // Initialize helmet tracking
            previousHelmetMap.put(player.getUniqueId(), player.getInventory().getHelmet());
        }, 5L); // Keep a small delay for stability
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        // Check if the entity is one of our balloon armorstands
        if (entity instanceof ArmorStand && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("Balloon:")) {
            // Cancel the damage event
            event.setCancelled(true);
        }

        // Also protect the chicken anchor
        if (entity instanceof Chicken && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("BalloonAnchor:")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Remove any balloon-related entities from the block list to prevent them from being destroyed
        event.blockList().removeIf(block -> {
            // Check nearby entities that might be our balloons
            for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 2, 2, 2)) {
                if ((entity instanceof ArmorStand && entity.getCustomName() != null &&
                        entity.getCustomName().startsWith("Balloon:")) ||
                        (entity instanceof Chicken && entity.getCustomName() != null &&
                                entity.getCustomName().startsWith("BalloonAnchor:"))) {
                    return true;
                }
            }
            return false;
        });
    }
}