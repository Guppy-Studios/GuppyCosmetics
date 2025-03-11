package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Chicken;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
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
import org.bukkit.persistence.PersistentDataType;

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

    /**
     * Prevent players from interacting with balloon entities
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();

        // Check if the entity is a balloon armorstand
        if (entity instanceof ArmorStand && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("Balloon:")) {
            event.setCancelled(true);
            return;
        }

        // Check if the entity is a balloon anchor chicken
        if (entity instanceof Chicken && entity.getCustomName() != null &&
                entity.getCustomName().startsWith("BalloonAnchor:")) {
            event.setCancelled(true);
            return;
        }

        // Also prevent interaction with lead hitches (if any)
        if (entity instanceof org.bukkit.entity.LeashHitch) {
            // Get nearby entities to see if this might be connected to a balloon
            for (Entity nearby : entity.getNearbyEntities(5, 5, 5)) {
                if ((nearby instanceof Chicken && nearby.getCustomName() != null &&
                        nearby.getCustomName().startsWith("BalloonAnchor:")) ||
                        (nearby instanceof ArmorStand && nearby.getCustomName() != null &&
                                nearby.getCustomName().startsWith("Balloon:"))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
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
            // Simplified: Use normal equip method for all hats (including those with overlays)
            equipHat(player, item);
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
                // Check hat slot
                if (ItemManager.isHat(cursorItem, configManager) && event.getRawSlot() == 39) {
                    // Check permission before allowing equip
                    if (!ItemManager.hasPermission(player, itemId, configManager)) {
                        event.setCancelled(true);
                        String message = getPrefix() + configManager.getMessagesConfig().getString("no-permission-item")
                                .replace("{item_id}", itemId);
                        player.sendMessage(ChatUtils.format(message));
                    }
                    // We don't need to handle overlay hats specially here since direct inventory equipping works fine
                }
            }
        }
    }

    private void equipHat(Player player, ItemStack item) {
        // Get the currently worn helmet
        ItemStack currentHelmet = player.getInventory().getHelmet();

        // Get the slot where the new hat is located
        int hatSlot = -1;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].equals(item)) {
                hatSlot = i;
                break;
            }
        }

        // Set the new hat and remove from inventory
        player.getInventory().setHelmet(item.clone());
        player.getInventory().removeItem(item);

        // If player was already wearing a hat, and we found the slot of the new hat
        if (currentHelmet != null && hatSlot != -1) {
            // Put the old hat in the slot where the new hat was
            player.getInventory().setItem(hatSlot, currentHelmet);
        } else if (currentHelmet != null) {
            // If we couldn't find the slot, or it was somehow invalid,
            // try to add it to inventory normally
            player.getInventory().addItem(currentHelmet);
        }

        // Send equipped message
        String message = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                .replace("{item}", getItemName(item));
        player.sendMessage(ChatUtils.format(message));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Save player cosmetics data first
        plugin.getCosmeticInventoryManager().savePlayerCosmetics(player);

        // Then remove visual entities
        backblingManager.removeBackbling(uuid);
        balloonManager.removeBalloon(uuid);

        // Clean up inventories
        plugin.getCosmeticInventoryManager().onPlayerQuit(uuid);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();

            // First, load the saved cosmetics from file
            plugin.getCosmeticInventoryManager().loadPlayerCosmetics(player);

            // Then restore visual entities
            backblingManager.checkAndRestoreBackbling(player);
            balloonManager.checkAndRestoreBalloon(player);
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