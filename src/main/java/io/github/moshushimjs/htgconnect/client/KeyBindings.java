package io.github.moshushimjs.htgconnect.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = HowToGoConnect.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KeyBindings {

    public static final String CATEGORY = "key.categories.htg_connect";

    public static final KeyMapping OPEN_INFO = new KeyMapping(
            "key.htg_connect.info",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY);

    private KeyBindings() {
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_INFO);
    }
}
