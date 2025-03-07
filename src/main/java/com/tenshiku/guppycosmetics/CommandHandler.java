package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.persistence.PersistentDataType;

public class CommandHandler implements CommandExecutor {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;

    public CommandHandler(GuppyCosmetics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    private String getPrefix() {
        return configManager.getMessagesConfig().getString("prefix", "");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            case "spawn":
                handleSpawn(sender, args);
                break;
            case "cosmetics":
            case "inventory":
                handleCosmeticInventory(sender);
                break;
            default:
                sendUsage(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guppycosmetics.reload")) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        configManager.reloadAllConfigs();
        sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("reloaded-message")));
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("guppycosmetics.spawn")) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        // Validate args length (allow optional "equip" parameter)
        if (args.length < 3 || args.length > 4) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("spawn-usage")));
            return;
        }

        // Check if the optional "equip" parameter is valid
        boolean shouldEquip = args.length == 4 && args[3].equalsIgnoreCase("equip");

        // Ensure sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("player-only")));
            return;
        }

        Player player = (Player) sender;
        CosmeticType type = CosmeticType.fromString(args[1]);
        String itemId = args[2];

        // Validate cosmetic type
        if (type == null) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("invalid-type")));
            return;
        }

        // Check if item exists in config
        boolean itemExists = false;
        switch (type) {
            case HAT:
                itemExists = configManager.getHatsConfig().contains(itemId);
                break;
            case BACKBLING:
                itemExists = configManager.getBackblingConfig().contains(itemId);
                break;
            case BALLOON:
                itemExists = configManager.getBalloonsConfig().contains(itemId);
                break;
        }

        if (!itemExists) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        // Check item-specific permission
        if (!ItemManager.hasPermission(player, itemId, configManager)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        // Special handling for hats with overlays
        if (type == CosmeticType.HAT && ItemManager.hasOverlayInConfig(itemId, configManager)) {
            boolean success = ItemManager.giveHatWithOverlay(player, itemId, configManager);
            if (success) {
                // Send successful message
                String itemName = configManager.getHatsConfig().getString(itemId + ".name", itemId);
                String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                        .replace("{item}", ChatUtils.formatToPlainText(itemName))
                        .replace("{player}", player.getName());
                sender.sendMessage(ChatUtils.format(message));

                // If this is a spawn command for the player themselves, offer to equip it
                if (sender == player && args.length == 4 && args[3].equalsIgnoreCase("equip")) {
                    // Use a delayed task to find and equip the newly created hat
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // Save current helmet if needed
                        ItemStack currentHelmet = player.getInventory().getHelmet();
                        int hatSlot = -1;

                        // Find the newly created hat in player's inventory
                        for (int i = 0; i < player.getInventory().getContents().length; i++) {
                            ItemStack item = player.getInventory().getContents()[i];
                            if (item != null && item.hasItemMeta() &&
                                    item.getItemMeta().getPersistentDataContainer().has(
                                            new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING) &&
                                    itemId.equals(item.getItemMeta().getPersistentDataContainer().get(
                                            new NamespacedKey(plugin, "item_id"), PersistentDataType.STRING))) {

                                hatSlot = i;

                                // Set the item as helmet and remove from inventory
                                player.getInventory().setHelmet(item.clone());
                                player.getInventory().removeItem(item);

                                // If player was already wearing a helmet and we found the hat slot
                                if (currentHelmet != null) {
                                    // Put old helmet in the slot where the new hat was
                                    player.getInventory().setItem(hatSlot, currentHelmet);
                                }

                                // Send equipped message
                                String equippedMessage = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                                        .replace("{item}", getItemName(item));
                                player.sendMessage(ChatUtils.format(equippedMessage));
                                break;
                            }
                        }
                    }, 5L); // Wait 5 ticks to ensure item is properly in inventory with updated metadata
                }
            } else {
                sender.sendMessage(ChatUtils.format(getPrefix() + "Failed to create hat with overlay."));
            }
            return;
        }

        // Standard item generation for other types
        ItemStack item = ItemManager.getItemById(itemId, configManager);

        // Check if item was created successfully
        if (item == null) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        // Give item to player
        player.getInventory().addItem(item);
        String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", getItemName(item))
                .replace("{player}", player.getName());
        sender.sendMessage(ChatUtils.format(message));

        // If should equip and it's a hat
        if (shouldEquip && type == CosmeticType.HAT) {
            // Save current helmet if needed
            ItemStack currentHelmet = player.getInventory().getHelmet();

            // Find the slot where the newly given hat is
            int hatSlot = -1;
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack slotItem = contents[i];
                if (slotItem != null &&
                        slotItem.hasItemMeta() &&
                        itemId.equals(ItemManager.getItemId(slotItem))) {
                    hatSlot = i;
                    break;
                }
            }

            // Set the item as helmet and remove from inventory
            player.getInventory().setHelmet(item.clone());
            player.getInventory().removeItem(item);

            // If player was already wearing a helmet and we found the hat slot
            if (currentHelmet != null && hatSlot != -1) {
                // Put old helmet in the slot where the new hat was
                player.getInventory().setItem(hatSlot, currentHelmet);
            } else if (currentHelmet != null) {
                // Try to add to inventory normally as fallback
                player.getInventory().addItem(currentHelmet);
            }

            // Send equipped message
            String equippedMessage = getPrefix() + configManager.getMessagesConfig().getString("equipped-message")
                    .replace("{item}", getItemName(item));
            player.sendMessage(ChatUtils.format(equippedMessage));
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("guppycosmetics.give")) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        // Validate args length
        if (args.length != 4) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("give-usage")));
            return;
        }

        // Get target player
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("player-not-found")
                    .replace("{player}", args[1]);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        CosmeticType type = CosmeticType.fromString(args[2]);
        String itemId = args[3];

        // Validate cosmetic type
        if (type == null) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("invalid-type")));
            return;
        }

        // Check item-specific permission for target player
        if (!ItemManager.hasPermission(target, itemId, configManager)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("target-no-permission")));
            return;
        }

        // Special handling for hats with overlays
        if (type == CosmeticType.HAT && ItemManager.hasOverlayInConfig(itemId, configManager)) {
            boolean success = ItemManager.giveHatWithOverlay(target, itemId, configManager);
            if (success) {
                String itemName = configManager.getHatsConfig().getString(itemId + ".name", itemId);
                String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                        .replace("{item}", ChatUtils.formatToPlainText(itemName))
                        .replace("{player}", target.getName());
                sender.sendMessage(ChatUtils.format(message));
            } else {
                sender.sendMessage(ChatUtils.format(getPrefix() + "Failed to create hat with overlay."));
            }
            return;
        }

        ItemStack item = ItemManager.getItemById(itemId, configManager);

        // Check if item exists
        if (item == null) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        // Give item to target player
        target.getInventory().addItem(item);
        String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", getItemName(item))
                .replace("{player}", target.getName());
        sender.sendMessage(ChatUtils.format(message));
    }

    private void handleCosmeticInventory(CommandSender sender) {
        // Check permission
        if (!sender.hasPermission("guppycosmetics.cosmetics")) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("player-only")));
            return;
        }

        Player player = (Player) sender;
        plugin.getCosmeticInventoryManager().openCosmeticInventory(player);
    }

    private String getItemName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return "Unknown Item";
    }

    private void sendUsage(CommandSender sender) {
        // Send title
        sender.sendMessage(ChatUtils.format(configManager.getMessagesConfig().getString("commands-title")));

        // Send available commands based on permissions
        if (sender.hasPermission("guppycosmetics.spawn")) {
            sender.sendMessage(ChatUtils.format(configManager.getMessagesConfig().getString("spawn-help")));
        }
        if (sender.hasPermission("guppycosmetics.give")) {
            sender.sendMessage(ChatUtils.format(configManager.getMessagesConfig().getString("give-help")));
        }
        if (sender.hasPermission("guppycosmetics.reload")) {
            sender.sendMessage(ChatUtils.format(configManager.getMessagesConfig().getString("reload-help")));
        }
        // Add help for cosmetics inventory command
        if (sender.hasPermission("guppycosmetics.cosmetics")) {
            sender.sendMessage(ChatUtils.format(configManager.getMessagesConfig().getString("cosmetics-help")));
        }
    }
}