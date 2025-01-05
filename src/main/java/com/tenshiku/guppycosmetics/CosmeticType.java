package com.tenshiku.guppycosmetics;

public enum CosmeticType {
    HAT("hat"),
    BALLOON("balloon"),
    BACKBLING("backbling"),
    ITEM("item");

    private final String identifier;

    CosmeticType(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public static CosmeticType fromString(String text) {
        for (CosmeticType type : CosmeticType.values()) {
            if (type.identifier.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}