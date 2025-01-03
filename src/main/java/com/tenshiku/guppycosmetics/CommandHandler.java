package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class CommandHandler implements CommandExecutor {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;

    public CommandHandler(GuppyCosmetics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    // Helper method to get prefix from config
    private String getPrefix() {
        return configManager.getMessagesConfig().getString("prefix", "");
    }

    // Helper method to get item name
    private String getItemName(ItemStack item) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
        }
        return "Unknown Item";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender, args);
                break;
            case "spawn":
                handleSpawn(sender, args);
                break;
            case "give":
                handleGive(sender, args);
                break;
            default:
                sendUsage(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender, String[] args) {
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

        // Ensure sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("player-only")));
            return;
        }

        // Check correct command usage
        if (args.length < 2) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("spawn-usage")));
            return;
        }

        String itemId = args[1];
        Player player = (Player) sender;
        ItemStack item = ItemManager.getItemById(itemId, configManager);

        // Check if item exists
        if (item == null) {
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

        // Give item to player
        player.getInventory().addItem(item);
        String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", getItemName(item))
                .replace("{player}", player.getName());
        sender.sendMessage(ChatUtils.format(message));
    }

    private void handleGive(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("guppycosmetics.give")) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        // Check correct command usage
        if (args.length < 3) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("give-usage")));
            return;
        }

        // Check if target player is online
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("player-not-found")
                    .replace("{player}", args[1]);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        String itemId = args[2];
        ItemStack item = ItemManager.getItemById(itemId, configManager);

        // Check if item exists
        if (item == null) {
            String message = getPrefix() + configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.format(message));
            return;
        }

        // Check item-specific permission for target player
        if (!ItemManager.hasPermission(target, itemId, configManager)) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("target-no-permission")));
            return;
        }

        // Give item to target player
        target.getInventory().addItem(item);
        String message = getPrefix() + configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", getItemName(item))
                .replace("{player}", target.getName());
        sender.sendMessage(ChatUtils.format(message));
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
    }
}