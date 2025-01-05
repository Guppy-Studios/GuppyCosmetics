package com.tenshiku.guppycosmetics;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

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
        }
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("guppycosmetics.give")) {
                // Return online player names for 'give' command
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
            }
            else if (args[0].equalsIgnoreCase("spawn") && sender.hasPermission("guppycosmetics.spawn")) {
                // Return cosmetic types for 'spawn' command
                return Arrays.stream(CosmeticType.values())
                        .map(type -> type.getIdentifier().toLowerCase())
                        .collect(Collectors.toList());
            }
        }
        else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") && sender.hasPermission("guppycosmetics.give")) {
                // Return cosmetic types for 'give' command
                return Arrays.stream(CosmeticType.values())
                        .map(type -> type.getIdentifier().toLowerCase())
                        .collect(Collectors.toList());
            }
            else if (args[0].equalsIgnoreCase("spawn") && sender.hasPermission("guppycosmetics.spawn")) {
                // Return available cosmetic IDs for the selected type
                CosmeticType type = CosmeticType.fromString(args[1]);
                if (type != null) {
                    return getAvailableCosmeticIds(sender, type);
                }
            }
        }
        else if (args.length == 4 && args[0].equalsIgnoreCase("give") && sender.hasPermission("guppycosmetics.give")) {
            // Return available cosmetic IDs for the selected type
            CosmeticType type = CosmeticType.fromString(args[2]);
            if (type != null) {
                return getAvailableCosmeticIds(sender, type);
            }
        }

        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getAvailableCosmeticIds(CommandSender sender, CosmeticType type) {
        switch (type) {
            case HAT:
                return getConfigSectionKeys(configManager.getHatsConfig(), sender);
            case BACKBLING:
                return getConfigSectionKeys(configManager.getBackblingConfig(), sender);
            case BALLOON:
                return getConfigSectionKeys(configManager.getBalloonsConfig(), sender);
            case ITEM:
                return getConfigSectionKeys(configManager.getItemsConfig(), sender);
            default:
                return new ArrayList<>();
        }
    }

    private List<String> getConfigSectionKeys(org.bukkit.configuration.file.FileConfiguration config, CommandSender sender) {
        List<String> ids = new ArrayList<>();

        for (String id : config.getKeys(false)) {
            String permission = config.getString(id + ".permission");
            if (permission == null || permission.isEmpty() || sender.hasPermission(permission)) {
                ids.add(id);
            }
        }

        return ids;
    }
}