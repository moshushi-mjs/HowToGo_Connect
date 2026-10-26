package io.github.moshushimjs.htgconnect;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue KICK_ON_DECLINE = BUILDER
            .comment("玩家拒绝服务器路网时：true = 直接踢出服务器，false = 只弹一个提示")
            .define("kickOnDecline", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static boolean kickOnDecline() {
        try {
            return KICK_ON_DECLINE.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
