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

        // Validate args length
        if (args.length != 3) {
            sender.sendMessage(ChatUtils.format(getPrefix() + configManager.getMessagesConfig().getString("spawn-usage")));
            return;
        }

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
    }
}