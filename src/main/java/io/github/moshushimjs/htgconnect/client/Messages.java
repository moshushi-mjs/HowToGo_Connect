package io.github.moshushimjs.htgconnect.client;

import io.github.moshushimjs.htgconnect.net.Prefix;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Messages {

    private Messages() {
    }

    public static Component of(String key, Object... args) {
        return Prefix.of(key, args);
    }

    public static void send(String key, Object... args) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(of(key, args), false);
        }
    }
}
