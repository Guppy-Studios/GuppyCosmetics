package com.tenshiku.guppycosmetics;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class EventListener implements Listener {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;
    private final BackblingManager backblingManager;

    public EventListener(GuppyCosmetics plugin, ConfigManager configManager, BackblingManager backblingManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.backblingManager = backblingManager;
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
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.AIR || !item.hasItemMeta()) return;

        String itemId = ItemManager.getItemId(item);
        if (itemId == null) return;

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
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Remove backbling display when player leaves
        backblingManager.removeBackbling(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Slight delay to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check and restore any equipped backbling
            backblingManager.checkAndRestoreBackbling(event.getPlayer());
        }, 5L);
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
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Check cursor item (item being placed)
        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && !cursorItem.getType().isAir()) {
            String itemId = ItemManager.getItemId(cursorItem);
            if (itemId != null) {
                // Check if attempting to equip hat (slot 39) or backbling (slot 38)
                if ((ItemManager.isHat(cursorItem, configManager) && event.getSlotType() == InventoryType.SlotType.ARMOR && event.getRawSlot() == 39) ||
                        (ItemManager.isBackbling(cursorItem, configManager) && event.getSlotType() == InventoryType.SlotType.ARMOR && event.getRawSlot() == 38)) {
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

        // Handle armor slot clicks for removal
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            ItemStack item = event.getCurrentItem();
            if (item != null && !item.getType().isAir()) {
                // Check if the clicked item is a backbling
                if (ItemManager.isBackbling(item, configManager)) {
                    // Remove the backbling display when unequipping
                    backblingManager.removeBackbling(player.getUniqueId());
                }
            }
        }
    }
}