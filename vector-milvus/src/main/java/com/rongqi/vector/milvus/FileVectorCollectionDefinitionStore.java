package com.rongqi.vector.milvus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rongqi.vector.core.VectorErrorCode;
import com.rongqi.vector.core.VectorException;
import com.rongqi.vector.core.schema.VectorCollectionDefinition;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * 基于本地 JSON 文件的 Collection schema 存储实现。
 *
 * <p>该实现适合开发、单机部署和容器挂载卷场景；多实例生产环境建议替换为数据库实现。</p>
 */
public class FileVectorCollectionDefinitionStore implements VectorCollectionDefinitionStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path storageDir;

    /**
     * 创建文件存储实现。
     *
     * @param storageDir schema 文件存储目录，为空时不读写磁盘
     */
    public FileVectorCollectionDefinitionStore(Path storageDir) {
        this.storageDir = storageDir;
    }

    @Override
    public List<VectorCollectionDefinition> loadAll() {
        if (storageDir == null || !Files.isDirectory(storageDir)) {
            return Collections.emptyList();
        }
        List<VectorCollectionDefinition> definitions = new ArrayList<>();
        try (Stream<Path> paths = Files.list(storageDir)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::loadOne)
                    .filter(definition -> definition != null)
                    .forEach(definitions::add);
        } catch (IOException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "加载 collection schema 目录失败: " + storageDir, exception);
        }
        return definitions;
    }

    @Override
    public void save(VectorCollectionDefinition definition) {
        if (storageDir == null) {
            return;
        }
        try {
            Files.createDirectories(storageDir);
            Path target = storageDir.resolve(safeFileName(definition.getCollection()) + ".json");
            try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                GSON.toJson(definition, writer);
            }
        } catch (IOException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "保存 collection schema 失败: " + definition.getCollection(), exception);
        }
    }

    private VectorCollectionDefinition loadOne(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, VectorCollectionDefinition.class);
        } catch (IOException exception) {
            throw new VectorException(VectorErrorCode.VECTOR_CONFIG_INVALID,
                    "加载 collection schema 文件失败: " + path, exception);
        }
    }

    private String safeFileName(String collection) {
        return collection.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
