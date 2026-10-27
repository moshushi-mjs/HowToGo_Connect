package io.github.moshushimjs.htgconnect.net;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class Prefix {

    private static final Style BRACKET_STYLE = Style.EMPTY.withColor(0xFF7FA8C9);
    private static final Style NAME_STYLE = Style.EMPTY.withColor(0xFF55CCFF);
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(0xFFFF5555);

    private Prefix() {
    }

    public static MutableComponent of(String key, Object... args) {
        return build(key, null, args);
    }

    public static MutableComponent error(String key, Object... args) {
        return build(key, ERROR_STYLE, args);
    }

    private static MutableComponent build(String key, Style bodyStyle, Object... args) {
        MutableComponent prefix = Component.literal("[").withStyle(BRACKET_STYLE)
                .append(Component.translatable("htg_connect.chat.name").withStyle(NAME_STYLE))
                .append(Component.literal("]").withStyle(BRACKET_STYLE))
                .append(Component.literal(" "));

        MutableComponent body = Component.translatable(key, args);
        if (bodyStyle != null) {
            body = body.withStyle(bodyStyle);
        }
        return prefix.append(body);
    }
}
