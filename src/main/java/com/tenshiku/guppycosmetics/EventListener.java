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
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.UUID;

public class EventListener implements Listener {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;
    private final BackblingManager backblingManager;
    private final BalloonManager balloonManager;

    public EventListener(GuppyCosmetics plugin, ConfigManager configManager, BackblingManager backblingManager, BalloonManager balloonManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.backblingManager = backblingManager;
        this.balloonManager = balloonManager;
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
                }
            }
        }

        // Handle current slot item (item being taken)
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && !currentItem.getType().isAir()) {
            // We don't need to handle backbling or balloon removal here anymore
            // as they're not in armor slots
        }
    }

    private void equipHat(Player player, ItemStack item) {
        // Store current helmet if exists
        if (player.getInventory().getHelmet() != null) {
            player.getInventory().addItem(player.getInventory().getHelmet());
        }

        // Set the new hat and remove the item from inventory
        player.getInventory().setHelmet(item.clone());
        player.getInventory().removeItem(item);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Remove visual entities
        backblingManager.removeBackbling(uuid);
        balloonManager.removeBalloon(uuid);

        // Clean up inventories
        plugin.getCosmeticInventoryManager().onPlayerQuit(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            backblingManager.checkAndRestoreBackbling(player);
            balloonManager.checkAndRestoreBalloon(player);
        }, 5L);
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