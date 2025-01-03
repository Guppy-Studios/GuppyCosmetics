package com.tenshiku.guppycosmetics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import java.util.List;
import java.util.stream.Collectors;

public class ChatUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component format(String message) {
        if (message == null) return Component.empty();
        return miniMessage.deserialize(message);
    }

    public static List<Component> formatList(List<String> messages) {
        return messages.stream()
                .map(ChatUtils::format)
                .collect(Collectors.toList());
    }
}