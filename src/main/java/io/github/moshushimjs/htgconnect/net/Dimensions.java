package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.chat.Component;

public final class Dimensions {

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    private Dimensions() {
    }

    public static String fileStem(String dimension) {
        if (dimension == null || dimension.isBlank()) {
            return "unknown";
        }
        return dimension.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    public static boolean same(String left, String right) {
        return fileStem(left).equals(fileStem(right));
    }

    public static boolean isOverworld(String dimension) {
        return same(OVERWORLD, dimension);
    }

    public static Component name(String dimension) {
        String stem = fileStem(dimension);
        if (stem.equals(fileStem(OVERWORLD))) {
            return Component.translatable("htg_connect.dimension.overworld");
        }
        if (stem.equals(fileStem(NETHER))) {
            return Component.translatable("htg_connect.dimension.nether");
        }
        if (stem.equals(fileStem(END))) {
            return Component.translatable("htg_connect.dimension.end");
        }
        if (dimension == null || dimension.isBlank()) {
            return Component.literal("unknown");
        }
        return Component.literal(dimension);
    }
}
