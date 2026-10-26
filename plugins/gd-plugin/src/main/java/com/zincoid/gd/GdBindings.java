package com.zincoid.gd;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * GDDL 用户绑定存储：保存在插件数据目录中。
 * <p>GDDL user binding storage persisted in the plugin data directory.</p>
 */
@Slf4j
final class GdBindings {

    private static final String ID_SUFFIX = ".id";
    private static final String NAME_SUFFIX = ".name";
    private static final String AVATAR_SUFFIX = ".avatar";

    private final Path file;
    private final Properties properties = new Properties();

    private GdBindings(Path file) {
        this.file = file;
    }

    /**
     * 从插件数据目录加载绑定记录。
     * <p>Loads bindings from the plugin data directory.</p>
     *
     * @param dataDir the plugin data directory
     * @return the binding store
     */
    static GdBindings load(Path dataDir) {
        GdBindings bindings = new GdBindings(dataDir.resolve("gddl-bindings.properties"));
        if (Files.isRegularFile(bindings.file)) {
            try (Reader reader = Files.newBufferedReader(bindings.file, StandardCharsets.UTF_8)) {
                bindings.properties.load(reader);
            } catch (IOException | IllegalArgumentException e) {
                log.warn("Cannot read GDDL bindings: {}", bindings.file, e);
            }
        }
        return bindings;
    }

    /**
     * 获取聊天用户绑定的 GDDL 用户。
     * <p>Gets the GDDL user bound to the chat user.</p>
     *
     * @param userId the platform user id
     * @return the binding, or null when not bound
     */
    synchronized Binding get(String userId) {
        String id = properties.getProperty(userId + ID_SUFFIX);
        String name = properties.getProperty(userId + NAME_SUFFIX);
        if (id == null || name == null) return null;
        try {
            return new Binding(Long.parseLong(id), name,
                    properties.getProperty(userId + AVATAR_SUFFIX));
        } catch (NumberFormatException e) {
            log.warn("Invalid GDDL binding for user {}: {}", userId, id);
            return null;
        }
    }

    /**
     * 绑定 GDDL 用户；已有绑定时返回原绑定，不覆盖。
     * <p>Binds a GDDL user; when already bound, returns the existing binding without overwriting it.</p>
     *
     * @param userId the platform user id
     * @param gddlId the GDDL user id
     * @param name   the GDDL user name
     * @param avatar the GDDL avatar uuid, nullable
     * @return the existing binding, or null when the new binding is saved
     */
    synchronized Binding bind(String userId, long gddlId, String name, String avatar) {
        Binding existing = get(userId);
        if (existing != null) return existing;
        properties.setProperty(userId + ID_SUFFIX, Long.toString(gddlId));
        properties.setProperty(userId + NAME_SUFFIX, name);
        if (avatar != null && !avatar.isBlank())
            properties.setProperty(userId + AVATAR_SUFFIX, avatar);
        try {
            save();
        } catch (IOException e) {
            properties.remove(userId + ID_SUFFIX);
            properties.remove(userId + NAME_SUFFIX);
            properties.remove(userId + AVATAR_SUFFIX);
            throw new GdException("Failed to save GDDL binding", e);
        }
        return null;
    }

    /**
     * 解绑并返回原绑定。
     * <p>Unbinds and returns the previous binding.</p>
     *
     * @param userId the platform user id
     * @return the previous binding, or null when not bound
     */
    synchronized Binding unbind(String userId) {
        Binding existing = get(userId);
        if (existing == null) return null;
        properties.remove(userId + ID_SUFFIX);
        properties.remove(userId + NAME_SUFFIX);
        properties.remove(userId + AVATAR_SUFFIX);
        try {
            save();
        } catch (IOException e) {
            properties.setProperty(userId + ID_SUFFIX, Long.toString(existing.id()));
            properties.setProperty(userId + NAME_SUFFIX, existing.name());
            if (existing.avatar() != null && !existing.avatar().isBlank())
                properties.setProperty(userId + AVATAR_SUFFIX, existing.avatar());
            throw new GdException("Failed to save GDDL binding", e);
        }
        return existing;
    }

    private void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            properties.store(writer, "GDDL user bindings");
        }
    }

    /**
     * 一条 GDDL 用户绑定。
     * <p>A GDDL user binding.</p>
     *
     * @param id     the GDDL user id
     * @param name   the GDDL user name
     * @param avatar the GDDL avatar uuid, nullable
     */
    record Binding(long id, String name, String avatar) {
    }
}
