package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

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
            equipBackblingToChestplate(player, item);
            String message = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                    .replace("{item}", getItemName(item));
            player.sendMessage(ChatUtils.format(message));
        } else if (ItemManager.isBalloon(item, configManager)) {
            // Always create the balloon, the manager will handle switching if one exists
            balloonManager.createBalloon(player, item);
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

        // Check cursor item (item being placed)
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            String itemId = ItemManager.getItemId(cursorItem);
            if (itemId != null) {
                // Check if attempting to equip hat (slot 39), backbling (slot 38), or balloon in leggings (slot 37)
                if ((ItemManager.isHat(cursorItem, configManager) && event.getRawSlot() == 39) ||
                        (ItemManager.isBackbling(cursorItem, configManager) && event.getRawSlot() == 38) ||
                        (ItemManager.isBalloon(cursorItem, configManager) && event.getRawSlot() == 37)) {

                    // Check permission before allowing equip
                    if (!ItemManager.hasPermission(player, itemId, configManager)) {
                        event.setCancelled(true);
                        String message = getPrefix() + configManager.getMessagesConfig().getString("no-permission-item")
                                .replace("{item_id}", itemId);
                        player.sendMessage(ChatUtils.format(message));
                        return;
                    }

                    // If it's a balloon being equipped to leggings slot
                    if (ItemManager.isBalloon(cursorItem, configManager) && event.getRawSlot() == 37) {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            balloonManager.createBalloon(player, cursorItem);
                        });
                    }
                }
            }
        }

        // Handle current slot item (item being taken)
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && !currentItem.getType().isAir()) {
            // If taking a balloon from leggings slot
            if (event.getRawSlot() == 37 && ItemManager.isBalloon(currentItem, configManager)) {
                balloonManager.removeBalloon(player.getUniqueId());
            }

            // Handle backbling removal
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && ItemManager.isBackbling(currentItem, configManager)) {
                backblingManager.removeBackbling(player.getUniqueId());
            }
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

    private void equipBackblingToChestplate(Player player, ItemStack item) {
        // Check if player already has a chestplate
        ItemStack currentChestplate = player.getInventory().getChestplate();
        if (currentChestplate != null) {
            // Give back the current chestplate
            player.getInventory().addItem(currentChestplate);
        }

        // Set the backbling item as the chestplate
        player.getInventory().setChestplate(item.clone());
        player.getInventory().removeItem(item);

        // Create the visual backbling display
        backblingManager.createBackbling(player, item);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        backblingManager.removeBackbling(player.getUniqueId());
        balloonManager.removeBalloon(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            backblingManager.checkAndRestoreBackbling(player);
            balloonManager.checkAndRestoreBalloon(player);
        }, 5L);
    }
}