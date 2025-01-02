package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandHandler implements CommandExecutor {
    private final GuppyCosmetics plugin;
    private final ConfigManager configManager;

    public CommandHandler(GuppyCosmetics plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
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
            sender.sendMessage(ChatUtils.colorize(configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        configManager.reloadAllConfigs();
        sender.sendMessage(ChatUtils.colorize(configManager.getMessagesConfig().getString("reloaded-message")));
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guppycosmetics.spawn")) {
            sender.sendMessage(ChatUtils.colorize(configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtils.colorize("&cThis command can only be used by players."));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatUtils.colorize("&cUsage: /guppycosmetics spawn <item-id>"));
            return;
        }

        String itemId = args[1];
        Player player = (Player) sender;
        ItemStack item = ItemManager.getItemById(itemId, configManager);

        if (item == null) {
            String message = configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.colorize(message));
            return;
        }

        // Check item-specific permission
        if (!ItemManager.hasPermission(player, itemId, configManager)) {
            sender.sendMessage(ChatUtils.colorize(configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        player.getInventory().addItem(item);
        String message = configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", item.getItemMeta().getDisplayName())
                .replace("{player}", player.getName());
        sender.sendMessage(ChatUtils.colorize(message));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guppycosmetics.give")) {
            sender.sendMessage(ChatUtils.colorize(configManager.getMessagesConfig().getString("no-permission")));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatUtils.colorize("&cUsage: /guppycosmetics give <player> <item-id>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            String message = configManager.getMessagesConfig().getString("player-not-found")
                    .replace("{player}", args[1]);
            sender.sendMessage(ChatUtils.colorize(message));
            return;
        }

        String itemId = args[2];
        ItemStack item = ItemManager.getItemById(itemId, configManager);

        if (item == null) {
            String message = configManager.getMessagesConfig().getString("invalid-item-id")
                    .replace("{item-id}", itemId);
            sender.sendMessage(ChatUtils.colorize(message));
            return;
        }

        // Check item-specific permission for the target player
        if (!ItemManager.hasPermission(target, itemId, configManager)) {
            sender.sendMessage(ChatUtils.colorize("&cTarget player doesn't have permission to use this cosmetic."));
            return;
        }

        target.getInventory().addItem(item);
        String message = configManager.getMessagesConfig().getString("item-given")
                .replace("{item}", item.getItemMeta().getDisplayName())
                .replace("{player}", target.getName());
        sender.sendMessage(ChatUtils.colorize(message));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&6GuppyCosmetics Commands:"));
        if (sender.hasPermission("guppycosmetics.spawn")) {
            sender.sendMessage(ChatUtils.colorize("&e/guppycosmetics spawn <item-id> &7- Spawn a cosmetic item"));
        }
        if (sender.hasPermission("guppycosmetics.give")) {
            sender.sendMessage(ChatUtils.colorize("&e/guppycosmetics give <player> <item-id> &7- Give a cosmetic to a player"));
        }
        if (sender.hasPermission("guppycosmetics.reload")) {
            sender.sendMessage(ChatUtils.colorize("&e/guppycosmetics reload &7- Reload configuration files"));
        }
    }
}