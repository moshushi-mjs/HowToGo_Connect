package io.github.moshushimjs.htgconnect;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import io.github.moshushimjs.htgconnect.net.NetworkRegistration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(HowToGoConnect.MODID)
public class HowToGoConnect {

    public static final String MODID = "htg_connect";

    public static final Logger LOGGER = LogUtils.getLogger();

    public HowToGoConnect(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(NetworkRegistration::onRegisterPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
