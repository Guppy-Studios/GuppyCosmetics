package com.tenshiku.guppycosmetics;

import net.md_5.bungee.api.ChatColor;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    public static String colorize(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group();
            matcher.appendReplacement(buffer, ChatColor.of(hexCode).toString());
        }
        matcher.appendTail(buffer);

        // Handle standard color codes after hex codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> colorizeList(List<String> messages) {
        return messages.stream()
                .map(ChatUtils::colorize)
                .collect(Collectors.toList());
    }
}