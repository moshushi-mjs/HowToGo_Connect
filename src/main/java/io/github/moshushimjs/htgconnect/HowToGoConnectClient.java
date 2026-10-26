package io.github.moshushimjs.htgconnect;

import com.mojang.brigadier.CommandDispatcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.moshushimjs.htgconnect.client.ClientPayloads;
import io.github.moshushimjs.htgconnect.client.ClientRoads;
import io.github.moshushimjs.htgconnect.client.Messages;
import io.github.moshushimjs.htgconnect.client.RoadImporter;
import io.github.moshushimjs.htgconnect.client.RoadSync;
import io.github.moshushimjs.htgconnect.net.ClientPayloadHooks;
import io.github.moshushimjs.htgconnect.net.RequestRoadPayload;
import io.github.moshushimjs.htgconnect.net.UploadRoadPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = HowToGoConnect.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HowToGoConnect.MODID, value = Dist.CLIENT)
public class HowToGoConnectClient {

    public HowToGoConnectClient(IEventBus modEventBus) {
        ClientPayloads.install();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        RoadSync.tick();
    }

    @SubscribeEvent
    static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        RoadSync.reset();
    }

    @SubscribeEvent
    static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("htg_connect")
                .then(Commands.literal("accept")
                        .executes(ctx -> {
                            if (Minecraft.getInstance().hasSingleplayerServer()) {
                                ctx.getSource().sendFailure(Messages.of("htg_connect.message.own"));
                                return 0;
                            }
                            if (ClientRoads.consented()) {
                                ctx.getSource().sendSuccess(
                                        () -> Messages.of("htg_connect.message.already"), false);
                                return 0;
                            }
                            ClientRoads.consent();
                            String dimension = ClientRoads.currentDimension();
                            if (dimension != null) {
                                PacketDistributor.sendToServer(new RequestRoadPayload(dimension, 0));
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Messages.of("htg_connect.message.accepted"), false);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> {
                            ClientRoads.forget();
                            ctx.getSource().sendSuccess(
                                    () -> Messages.of("htg_connect.message.reset"), false);
                            return 1;
                        }))
                .then(Commands.literal("upload")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> upload(ctx.getSource()))));
    }

    private static int upload(CommandSourceStack source) {
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            source.sendFailure(Messages.of("htg_connect.message.invalid"));
            return 0;
        }

        String dimension = ClientRoads.currentDimension();
        if (dimension == null) {
            source.sendFailure(Messages.of("htg_connect.message.invalid"));
            return 0;
        }

        String json = ClientPayloadHooks.localRoadText(dimension);
        if (json == null || json.isBlank()) {
            source.sendFailure(Messages.of("htg_connect.message.upload.empty"));
            return 0;
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > UploadRoadPayload.MAX_BYTES) {
            Path saved = RoadImporter.localFile();
            try {
                Files.createDirectories(saved.getParent());
                Files.write(saved, bytes);
            } catch (IOException e) {
                HowToGoConnect.LOGGER.warn("客户端路网文件写入失败：{}", e.toString());
            }
            source.sendFailure(Messages.of("htg_connect.message.upload.too_big", saved.toString()));
            return 0;
        }

        PacketDistributor.sendToServer(new UploadRoadPayload(dimension, json));
        HowToGoConnect.LOGGER.info("上传 {} 的路网：{} 字节", dimension, bytes.length);
        return 1;
    }
}
