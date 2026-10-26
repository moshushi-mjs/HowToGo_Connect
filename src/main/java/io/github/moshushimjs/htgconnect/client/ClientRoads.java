package io.github.moshushimjs.htgconnect.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ClientRoads {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Entry> BY_SERVER = new HashMap<>();
    private static boolean loaded = false;

    private ClientRoads() {
    }

    public static class Entry {
        public String decision = "";
        public Map<String, Integer> versions = new HashMap<>();
    }

    private static class Store {
        Map<String, Entry> servers = new HashMap<>();
    }

    public static String currentDimension() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.dimension().location().toString();
    }

    private static Path statePath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("htg_connect").resolve("client_roads.json");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = statePath();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Store store = GSON.fromJson(reader, Store.class);
            if (store != null && store.servers != null) {
                BY_SERVER.putAll(store.servers);
            }
        } catch (IOException | RuntimeException e) {
            HowToGoConnect.LOGGER.warn("路网状态读取失败：{}", e.toString());
        }
    }

    private static void save() {
        Path path = statePath();
        try {
            Files.createDirectories(path.getParent());
            Store store = new Store();
            store.servers = new HashMap<>(BY_SERVER);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(store, writer);
            }
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("路网状态保存失败：{}", e.toString());
        }
    }

    public static Entry entry() {
        ensureLoaded();
        return BY_SERVER.computeIfAbsent(serverKey(), key -> new Entry());
    }

    public static boolean consented() {
        return "accept".equals(entry().decision);
    }

    public static boolean decided() {
        return !entry().decision.isEmpty();
    }

    public static void consent() {
        entry().decision = "accept";
        save();
    }

    public static void decline() {
        entry().decision = "decline";
        save();
    }

    public static void forget() {
        entry().decision = "";
        entry().versions.clear();
        save();
    }

    public static int versionOf(String dimension) {
        Integer version = entry().versions.get(Dimensions.fileStem(dimension));
        return version == null ? 0 : version;
    }

    public static void setVersion(String dimension, int version) {
        entry().versions.put(Dimensions.fileStem(dimension), version);
        save();
    }

    private static String serverKey() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer()) {
            return "singleplayer";
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank()) {
            return server.ip;
        }
        return "unknown";
    }
}
