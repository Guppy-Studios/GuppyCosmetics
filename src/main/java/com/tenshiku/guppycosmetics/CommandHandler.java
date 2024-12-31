package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.RED + "Usage: /guppycosmetics <spawn/reload>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            configManager.reloadAllConfigs();
            sender.sendMessage(ChatColor.GREEN + "Configurations reloaded!");
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /guppycosmetics spawn <item-id> [player]");
                return true;
            }

            String itemId = args[1];
            Player target = sender instanceof Player ? (Player) sender : null;

            if (args.length == 3) {
                target = Bukkit.getPlayer(args[2]);
            }

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }

            ItemStack item = ItemManager.getItemById(itemId, configManager);
            if (item == null) {
                sender.sendMessage(ChatColor.RED + "Item ID not found in the config!");
                return true;
            }

            target.getInventory().addItem(item);
            sender.sendMessage(ChatColor.GREEN + "Gave " + itemId + " to " + target.getName());
            return true;
        }

        return false;
    }
}
