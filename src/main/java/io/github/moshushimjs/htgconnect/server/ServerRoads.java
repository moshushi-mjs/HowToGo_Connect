package io.github.moshushimjs.htgconnect.server;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.moshushimjs.htgconnect.HowToGoConnect;
import io.github.moshushimjs.htgconnect.net.ClientPayloadHooks;
import io.github.moshushimjs.htgconnect.net.Dimensions;
import io.github.moshushimjs.htgconnect.net.RoadOfferPayload;

import net.minecraft.server.MinecraftServer;

public final class ServerRoads {

    private static final String LEGACY_FILE = "server_network.json";
    private static final String FILE_PREFIX = "server_network.";
    private static final String FILE_SUFFIX = ".json";
    private static final String INDEX_NAME = "dimensions.json";
    private static final String BACKUP_SUFFIX = ".bak";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Snapshot> BY_DIMENSION = new LinkedHashMap<>();
    private static final Map<String, String> FILE_OF = new LinkedHashMap<>();
    private static final Map<String, Long> STAMP = new LinkedHashMap<>();

    private ServerRoads() {
    }

    public record Snapshot(String dimension, String text, int version, int nodes, int segments, int bytes) {
    }

    public enum UploadResult {
        OK,
        EMPTY,
        UNCHANGED,
        BAD_FORMAT,
        WRITE_FAILED
    }

    public static Path directory(Path gameDirectory) {
        return gameDirectory.resolve("config").resolve("htg_connect");
    }

    public static void reset() {
        BY_DIMENSION.clear();
        FILE_OF.clear();
        STAMP.clear();
    }

    public static boolean refresh(MinecraftServer server) {
        if (server.isDedicatedServer()) {
            return refreshFromFiles(server.getServerDirectory());
        }
        return refreshFromLocal();
    }

    private static boolean refreshFromFiles(Path gameDirectory) {
        Path dir = directory(gameDirectory);
        loadIndex(dir);

        boolean changed = false;
        Set<String> known = new LinkedHashSet<>(FILE_OF.keySet());

        for (String dimension : known) {
            String fileName = FILE_OF.get(dimension);
            Path path = dir.resolve(fileName);
            if (!Files.isRegularFile(path)) {
                changed |= drop(dimension, "文件不存在");
                continue;
            }
            String text;
            try {
                text = Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                HowToGoConnect.LOGGER.warn("路网文件读取失败（{}）：{}", fileName, e.toString());
                continue;
            }
            changed |= apply(dimension, text, "文件");
        }

        Set<String> stale = new LinkedHashSet<>(BY_DIMENSION.keySet());
        stale.removeAll(known);
        for (String dimension : stale) {
            changed |= drop(dimension, "索引里没有这个维度");
        }

        return changed;
    }

    private static boolean refreshFromLocal() {
        List<String> dimensions = ClientPayloadHooks.localRoadDimensions();
        Set<String> present = new LinkedHashSet<>(dimensions);

        boolean changed = false;
        for (String dimension : new ArrayList<>(BY_DIMENSION.keySet())) {
            if (!present.contains(dimension)) {
                STAMP.remove(dimension);
                changed |= drop(dimension, "房主那边已经没有这个维度");
            }
        }

        for (String dimension : dimensions) {
            long stamp = ClientPayloadHooks.localRoadStamp(dimension);
            Long previous = STAMP.get(dimension);
            if (previous != null && previous == stamp && BY_DIMENSION.containsKey(dimension)) {
                continue;
            }
            String text = ClientPayloadHooks.localRoadText(dimension);
            if (text == null) {
                continue;
            }
            STAMP.put(dimension, stamp);
            changed |= apply(dimension, text, "房主本机");
        }
        return changed;
    }

    private static boolean apply(String dimension, String text, String source) {
        if (text == null || text.isBlank()) {
            return drop(dimension, "内容为空");
        }
        Counts counts = countOrNull(text);
        if (counts == null) {
            return drop(dimension, "解析失败");
        }
        if (counts.nodes == 0) {
            return drop(dimension, "没有点");
        }

        Snapshot old = BY_DIMENSION.get(dimension);
        if (old != null && text.equals(old.text())) {
            return false;
        }

        dropSameStem(dimension);

        int version = contentVersion(text);
        int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        BY_DIMENSION.put(dimension, new Snapshot(dimension, text, version, counts.nodes, counts.segments, bytes));

        HowToGoConnect.LOGGER.info("路网来源【{}】{}：{} 个点，{} 条线（{} 字节）",
                source, dimension, counts.nodes, counts.segments, bytes);
        return true;
    }

    private static int contentVersion(String text) {
        int hash = text.hashCode() & 0x7fffffff;
        return hash == 0 ? 1 : hash;
    }

    private static void dropSameStem(String dimension) {
        String stem = Dimensions.fileStem(dimension);
        for (String key : new ArrayList<>(BY_DIMENSION.keySet())) {
            if (!key.equals(dimension) && Dimensions.fileStem(key).equals(stem)) {
                BY_DIMENSION.remove(key);
                HowToGoConnect.LOGGER.info("路网键合并（{} 与 {} 是同一个维度）", key, dimension);
            }
        }
    }

    private static boolean drop(String dimension, String reason) {
        if (BY_DIMENSION.remove(dimension) == null) {
            return false;
        }
        HowToGoConnect.LOGGER.info("路网移除（{}）：{}", dimension, reason);
        return true;
    }

    public static boolean available() {
        return !BY_DIMENSION.isEmpty();
    }

    public static List<RoadOfferPayload.Entry> catalog() {
        List<RoadOfferPayload.Entry> entries = new ArrayList<>();
        for (Snapshot snapshot : BY_DIMENSION.values()) {
            entries.add(new RoadOfferPayload.Entry(
                    snapshot.dimension(), snapshot.version(), snapshot.bytes()));
        }
        return entries;
    }

    public static String text(String dimension) {
        Snapshot snapshot = find(dimension);
        return snapshot == null ? null : snapshot.text();
    }

    public static int version(String dimension) {
        Snapshot snapshot = find(dimension);
        return snapshot == null ? 0 : snapshot.version();
    }

    private static Snapshot find(String dimension) {
        Snapshot exact = BY_DIMENSION.get(dimension);
        if (exact != null) {
            return exact;
        }
        for (Snapshot snapshot : BY_DIMENSION.values()) {
            if (Dimensions.same(snapshot.dimension(), dimension)) {
                return snapshot;
            }
        }
        return null;
    }

    public static UploadResult acceptUpload(MinecraftServer server, String dimension, String text) {
        if (text == null || text.isBlank()) {
            return UploadResult.EMPTY;
        }
        Counts counts = countOrNull(text);
        if (counts == null) {
            HowToGoConnect.LOGGER.warn("上传的路网无法解析（{}）", dimension);
            return UploadResult.BAD_FORMAT;
        }
        if (counts.nodes == 0) {
            HowToGoConnect.LOGGER.warn("上传的路网是空的（{}）", dimension);
            return UploadResult.EMPTY;
        }

        Snapshot old = find(dimension);
        if (old != null && text.equals(old.text())) {
            return UploadResult.UNCHANGED;
        }

        Path dir = directory(server.getServerDirectory());
        loadIndex(dir);
        String fileName = fileNameFor(dimension);
        Path path = dir.resolve(fileName);

        try {
            Files.createDirectories(dir);
            if (Files.isRegularFile(path)) {
                Path backup = dir.resolve(fileName + BACKUP_SUFFIX);
                Files.deleteIfExists(backup);
                Files.copy(path, backup);
            }
            writeAtomically(path, text);
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("服务端路网写入失败：{}", e.toString());
            return UploadResult.WRITE_FAILED;
        }

        boolean indexChanged = !fileName.equals(FILE_OF.get(dimension));
        if (indexChanged) {
            FILE_OF.put(dimension, fileName);
            saveIndex(dir);
        }

        int version = contentVersion(text);
        int bytes = text.getBytes(StandardCharsets.UTF_8).length;
        BY_DIMENSION.put(dimension, new Snapshot(dimension, text, version, counts.nodes, counts.segments, bytes));
        STAMP.remove(dimension);

        HowToGoConnect.LOGGER.info("路网来源【上传】{}：{} 个点，{} 条线（{} 字节）→ {}",
                dimension, counts.nodes, counts.segments, bytes, fileName);
        return UploadResult.OK;
    }

    private static String fileNameFor(String dimension) {
        String existing = FILE_OF.get(dimension);
        if (existing != null) {
            return existing;
        }
        if (Dimensions.isOverworld(dimension)) {
            return LEGACY_FILE;
        }

        String stem = Dimensions.fileStem(dimension);
        String candidate = FILE_PREFIX + stem + FILE_SUFFIX;
        int attempt = 1;
        while (takenByOther(candidate, dimension) && attempt < 100) {
            attempt++;
            candidate = FILE_PREFIX + stem + "-" + attempt + FILE_SUFFIX;
        }
        return candidate;
    }

    private static boolean takenByOther(String fileName, String dimension) {
        for (Map.Entry<String, String> entry : FILE_OF.entrySet()) {
            if (entry.getValue().equals(fileName) && !entry.getKey().equals(dimension)) {
                return true;
            }
        }
        return false;
    }

    private static void writeAtomically(Path target, String text) throws IOException {
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, text, StandardCharsets.UTF_8);
        try {
            Files.move(temp, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void loadIndex(Path dir) {
        Map<String, String> loaded = new LinkedHashMap<>();

        Path indexPath = dir.resolve(INDEX_NAME);
        if (Files.isRegularFile(indexPath)) {
            try (Reader reader = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
                IndexFile file = GSON.fromJson(reader, IndexFile.class);
                if (file != null && file.dimensions != null) {
                    loaded.putAll(file.dimensions);
                }
            } catch (IOException | RuntimeException e) {
                HowToGoConnect.LOGGER.warn("维度索引读取失败：{}", e.toString());
            }
        }

        if (loaded.isEmpty() && Files.isRegularFile(dir.resolve(LEGACY_FILE))) {
            loaded.put(Dimensions.OVERWORLD, LEGACY_FILE);
        }

        FILE_OF.clear();
        FILE_OF.putAll(loaded);
    }

    private static void saveIndex(Path dir) {
        Path indexPath = dir.resolve(INDEX_NAME);
        try {
            Files.createDirectories(dir);
            IndexFile file = new IndexFile();
            file.dimensions = new LinkedHashMap<>(FILE_OF);
            writeAtomically(indexPath, GSON.toJson(file));
        } catch (IOException e) {
            HowToGoConnect.LOGGER.warn("维度索引写入失败：{}", e.toString());
        }
    }

    private static final class IndexFile {
        Map<String, String> dimensions = new LinkedHashMap<>();
    }

    private record Counts(int nodes, int segments) {
    }

    private static Counts countOrNull(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return new Counts(sizeOf(root, "nodes"), sizeOf(root, "segments"));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static int sizeOf(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || !element.isJsonArray()) {
            return 0;
        }
        return element.getAsJsonArray().size();
    }
}
