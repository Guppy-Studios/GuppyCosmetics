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

public class EventListener implements Listener {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;
    private final BackblingManager backblingManager;

    public EventListener(GuppyCosmetics plugin, ConfigManager configManager, BackblingManager backblingManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.backblingManager = backblingManager;
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
        backblingManager.removeBackbling(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Slight delay to ensure player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            backblingManager.checkAndRestoreBackbling(event.getPlayer());
        }, 5L);
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

        // Set the backbling item as the chestplate
        player.getInventory().setChestplate(item.clone());
        player.getInventory().removeItem(item);

        // Create the backbling display
        backblingManager.createBackbling(player, item);
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
                    backblingManager.removeBackbling(player.getUniqueId());
                }
            }
        }
    }
}