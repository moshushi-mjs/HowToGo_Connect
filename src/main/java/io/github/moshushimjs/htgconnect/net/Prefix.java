package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class Prefix {

    private static final Style BRACKET_STYLE = Style.EMPTY.withColor(0xFF7FA8C9);
    private static final Style NAME_STYLE = Style.EMPTY.withColor(0xFF55CCFF);

    private Prefix() {
    }

    public static MutableComponent of(String key, Object... args) {
        MutableComponent prefix = Component.literal("[").withStyle(BRACKET_STYLE)
                .append(Component.translatable("htg_connect.chat.name").withStyle(NAME_STYLE))
                .append(Component.literal("]").withStyle(BRACKET_STYLE))
                .append(Component.literal(" "));

        return prefix.append(Component.translatable(key, args));
    }
}
