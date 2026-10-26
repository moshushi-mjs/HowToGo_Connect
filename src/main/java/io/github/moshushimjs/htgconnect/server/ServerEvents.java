package io.github.moshushimjs.htgconnect.server;

import java.util.List;

import io.github.moshushimjs.htgconnect.Config;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HowToGoConnect.MODID)
public final class ServerEvents {

    private static final int CHECK_INTERVAL_TICKS = 100;

    private static int tickCounter = 0;

    private ServerEvents() {
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        tickCounter = 0;
        ServerRoads.reset();
    }

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.server;
        ServerRoads.refresh(server);

        if (!ServerRoads.available()) {
            HowToGoConnect.LOGGER.info("{} 加入了，但服务端还没有路网",
                    player.getName().getString());
            return;
        }

        HowToGoConnect.LOGGER.info("告诉 {}：服务端有 {} 个维度的路网",
                player.getName().getString(), ServerRoads.catalog().size());
        sendCatalog(player);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        boolean changed = ServerRoads.refresh(server);
        if (!changed || !ServerRoads.available()) {
            return;
        }

        int count = pushCatalog(server, null, null);
        HowToGoConnect.LOGGER.info("服务端路网变了，推给 {} 个在线玩家", count);
    }

    private static void sendCatalog(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player,
                new RoadOfferPayload(ServerRoads.catalog(), Config.kickOnDecline()));
    }

    public static int pushCatalog(MinecraftServer server, Component broadcast, ServerPlayer skip) {
        List<RoadOfferPayload.Entry> entries = ServerRoads.catalog();
        if (entries.isEmpty()) {
            return 0;
        }
        RoadOfferPayload payload = new RoadOfferPayload(entries, Config.kickOnDecline());

        int count = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (broadcast != null) {
                player.sendSystemMessage(broadcast);
            }
            if (player == skip) {
                continue;
            }
            PacketDistributor.sendToPlayer(player, payload);
            count++;
        }
        return count;
    }
}
