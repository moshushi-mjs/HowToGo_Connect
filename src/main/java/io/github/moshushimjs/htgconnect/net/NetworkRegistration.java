package io.github.moshushimjs.htgconnect.net;

import io.github.moshushimjs.htgconnect.Config;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.server.ServerEvents;
import io.github.moshushimjs.htgconnect.server.ServerRoads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NetworkRegistration {
    private NetworkRegistration() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");

        registrar.playToServer(RequestRoadPayload.TYPE, RequestRoadPayload.STREAM_CODEC,
                NetworkRegistration::handleRoadRequest);

        registrar.playToServer(DeclineRoadPayload.TYPE, DeclineRoadPayload.STREAM_CODEC,
                NetworkRegistration::handleDecline);

        registrar.playToServer(UploadRoadPayload.TYPE, UploadRoadPayload.STREAM_CODEC,
                NetworkRegistration::handleUpload);

        registrar.playToServer(RequestCatalogPayload.TYPE, RequestCatalogPayload.STREAM_CODEC,
                NetworkRegistration::handleCatalogRequest);

        registrar.playToClient(RoadOfferPayload.TYPE, RoadOfferPayload.STREAM_CODEC,
                ClientPayloadHooks::onRoadOffer);

        registrar.playToClient(RoadNetworkPayload.TYPE, RoadNetworkPayload.STREAM_CODEC,
                ClientPayloadHooks::onRoadNetwork);

        registrar.playToClient(CatalogPayload.TYPE, CatalogPayload.STREAM_CODEC,
                ClientPayloadHooks::onCatalog);
    }

    private static void handleCatalogRequest(RequestCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            HowToGoConnect.LOGGER.info("{} 查看了路网清单（{} 个维度）",
                    player.getName().getString(), ServerRoads.catalog().size());
            PacketDistributor.sendToPlayer(player, new CatalogPayload(ServerRoads.catalog()));
        });
    }

    private static void handleDecline(DeclineRoadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            HowToGoConnect.LOGGER.info("{} 拒绝了服务器路网（配置：{}）",
                    player.getName().getString(),
                    Config.kickOnDecline() ? "踢出" : "只提示");

            if (Config.kickOnDecline()) {
                player.connection.disconnect(Component.literal("你拒绝接收服务器路网，无法在此服务器游玩"));
            }
        });
    }

    private static void handleUpload(UploadRoadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.server.isDedicatedServer() || !player.hasPermissions(4)) {
                HowToGoConnect.LOGGER.warn("{} 尝试上传路网，但没有权限或在集成服务端",
                        player.getName().getString());
                player.sendSystemMessage(Prefix.error("htg_connect.message.invalid"));
                return;
            }

            ServerRoads.UploadResult result =
                    ServerRoads.acceptUpload(player.server, payload.dimension(), payload.json());

            switch (result) {
                case OK -> {
                    Component broadcast = Dimensions.isOverworld(payload.dimension())
                            ? Prefix.of("htg_connect.message.upload.broadcast",
                                    player.getName().getString())
                            : Prefix.of("htg_connect.message.upload.broadcast.dimension",
                                    player.getName().getString(),
                                    Dimensions.name(payload.dimension()));

                    int count = ServerEvents.pushCatalog(player.server, broadcast, player);
                    HowToGoConnect.LOGGER.info("{} 上传了路网（{}），推给 {} 个在线玩家",
                            player.getName().getString(), payload.dimension(), count);
                    player.sendSystemMessage(
                            Prefix.of("htg_connect.message.upload.ok", count));
                }
                case EMPTY -> player.sendSystemMessage(
                        Prefix.error("htg_connect.message.upload.empty"));
                case UNCHANGED -> player.sendSystemMessage(
                        Prefix.error("htg_connect.message.upload.unchanged"));
                case BAD_FORMAT -> player.sendSystemMessage(
                        Prefix.error("htg_connect.message.upload.bad"));
                case WRITE_FAILED -> player.sendSystemMessage(
                        Prefix.error("htg_connect.message.upload.write_failed"));
            }
        });
    }

    private static void handleRoadRequest(RequestRoadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            String dimension = payload.dimension();
            String text = ServerRoads.text(dimension);
            if (text == null) {
                HowToGoConnect.LOGGER.info("{} 请求 {} 的路网，但服务端没有",
                        player.getName().getString(), dimension);
                return;
            }

            int version = ServerRoads.version(dimension);
            if (payload.knownVersion() == version) {
                return;
            }

            HowToGoConnect.LOGGER.info("{} 请求路网（{}），发给他", player.getName().getString(), dimension);
            PacketDistributor.sendToPlayer(player,
                    new RoadNetworkPayload(dimension, version, text));
        });
    }
}
