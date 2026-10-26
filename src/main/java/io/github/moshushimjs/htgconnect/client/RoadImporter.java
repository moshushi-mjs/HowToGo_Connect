package io.github.moshushimjs.htgconnect.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import bili.dongsz.howtogo.client.RoadStore;
import bili.dongsz.howtogo.road.RoadNetwork;
import bili.dongsz.howtogo.road.RoadStorage;
import io.github.moshushimjs.htgconnect.HowToGoConnect;
import net.minecraft.client.Minecraft;

public final class RoadImporter {

    private RoadImporter() {
    }

    public static boolean importFrom(Path file) {
        if (!Files.isRegularFile(file)) {
            HowToGoConnect.LOGGER.warn("路网文件不存在：{}", file);
            return false;
        }
        RoadNetwork incoming;
        try {
            incoming = RoadStorage.load(file);
        } catch (RuntimeException e) {
            HowToGoConnect.LOGGER.warn("路网文件解析失败：{}", e.toString());
            return false;
        }
        if (incoming == null) {
            return false;
        }
        apply(incoming);
        return true;
    }

    public static boolean importJson(String json) {
        try {
            Path temp = Files.createTempFile("htg_connect_", ".json");
            try {
                Files.writeString(temp, json, StandardCharsets.UTF_8);
                return importFrom(temp);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("临时文件写入失败：{}", e.toString());
            return false;
        }
    }

    public static void apply(RoadNetwork incoming) {
        RoadNetwork live = RoadStore.get();
        int before = live.nodeCount();

        live.replaceWith(incoming);
        RoadStore.markDirty();
        RoadSync.markSynced();

        HowToGoConnect.LOGGER.info("路网已替换：原来的 {} 个点被换成 {} 个点，{} 条线",
                before, live.nodeCount(), live.segmentCount());
    }

    public static void clear() {
        RoadNetwork live = RoadStore.get();
        live.replaceWith(new RoadNetwork());
        RoadStore.markDirty();
        HowToGoConnect.LOGGER.info("已清空本地路网");
    }

    public static int nodeCount() {
        return RoadStore.get().nodeCount();
    }

    public static int segmentCount() {
        return RoadStore.get().segmentCount();
    }

    public static Path localFile() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("htg_connect").resolve("server_network.json");
    }
}
