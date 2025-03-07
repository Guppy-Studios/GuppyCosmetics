package com.tenshiku.guppycosmetics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.List;
import java.util.stream.Collectors;

public class ChatUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    public static Component format(String message) {
        if (message == null) return Component.empty();
        return miniMessage.deserialize(message);
    }

    public static List<Component> formatList(List<String> messages) {
        return messages.stream()
                .map(ChatUtils::format)
                .collect(Collectors.toList());
    }

    /**
     * Convert a Component to plain text
     * @param component The Component to convert
     * @return The plain text representation
     */
    public static String toPlainText(Component component) {
        if (component == null) return "";
        return plainSerializer.serialize(component);
    }

    /**
     * Format a MiniMessage string and convert it to plain text
     * @param message The MiniMessage string to format
     * @return The plain text representation
     */
    public static String formatToPlainText(String message) {
        if (message == null) return "";
        return toPlainText(format(message));
    }
}