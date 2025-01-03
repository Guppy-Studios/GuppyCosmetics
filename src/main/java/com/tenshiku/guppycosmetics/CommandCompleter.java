package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCompleter implements TabCompleter {
    private final ConfigManager configManager;

    public CommandCompleter(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - command type
            if (sender.hasPermission("guppycosmetics.spawn")) completions.add("spawn");
            if (sender.hasPermission("guppycosmetics.give")) completions.add("give");
            if (sender.hasPermission("guppycosmetics.reload")) completions.add("reload");
        } else if (args.length == 2) {
            // Second argument
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("guppycosmetics.give")) {
                // Return online player names for 'give' command
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            } else if (args[0].equalsIgnoreCase("spawn") && sender.hasPermission("guppycosmetics.spawn")) {
                // Return available cosmetic IDs for 'spawn' command
                completions.addAll(getAvailableCosmeticIds(sender));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give") && sender.hasPermission("guppycosmetics.give")) {
            // Third argument for 'give' command - cosmetic IDs
            completions.addAll(getAvailableCosmeticIds(sender));
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getAvailableCosmeticIds(CommandSender sender) {
        List<String> ids = new ArrayList<>();
        FileConfiguration hatsConfig = configManager.getHatsConfig();
        FileConfiguration backblingConfig = configManager.getBackblingConfig();

        // Add hat IDs
        for (String id : hatsConfig.getKeys(false)) {
            String permission = hatsConfig.getString(id + ".permission");
            if (permission == null || permission.isEmpty() || sender.hasPermission(permission)) {
                ids.add(id);
            }
        }

        // Add backbling IDs
        for (String id : backblingConfig.getKeys(false)) {
            String permission = backblingConfig.getString(id + ".permission");
            if (permission == null || permission.isEmpty() || sender.hasPermission(permission)) {
                ids.add(id);
            }
        }

        return ids;
    }
}