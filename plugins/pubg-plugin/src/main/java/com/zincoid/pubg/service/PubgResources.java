package com.zincoid.pubg.service;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PUBG 资源索引：地图名称、拼音首字母别名、随插件 JAR 分发的图片文件与插件数据目录。
 * <p>PUBG resource index: map names, pinyin-initial aliases, image files shipped in the plugin JAR,
 * and the plugin data directory.</p>
 */
public final class PubgResources {

    private static final Map<String, String> MAPS = createMaps();

    private final Path root;

    /**
     * 创建资源索引。
     * <p>Creates the resource index.</p>
     *
     * @param root the plugin data directory
     */
    public PubgResources(Path root) {
        this.root = root;
    }

    /**
     * 返回地图名称或拼音首字母到图片文件的映射。
     * <p>Returns the map from map names or pinyin initials to image files.</p>
     *
     * @return the unmodifiable map, in display order
     */
    public Map<String, String> maps() {
        return MAPS;
    }

    /**
     * 返回随 JAR 分发的全部图片文件，按地图顺序去重。
     * <p>Returns all image files shipped in the JAR, deduplicated in map order.</p>
     *
     * @return the image file names
     */
    public Set<String> files() {
        return new LinkedHashSet<>(MAPS.values());
    }

    /**
     * 按地图名称或拼音首字母查找图片路径，大小写不敏感。
     * <p>Finds the image path by map name or pinyin initials, case-insensitively.</p>
     *
     * @param name the map name or pinyin initials
     * @return the image path, or null when the map is unsupported
     */
    public Path path(String name) {
        String file = name == null ? null : MAPS.get(name.toLowerCase(Locale.ROOT));
        return file == null ? null : root.resolve(file);
    }

    // ════ Helpers ═════════════════

    private static Map<String, String> createMaps() {
        Map<String, String> maps = new LinkedHashMap<>();
        addMap(maps, "艾伦格", "alg", "Erangel.png");
        addMap(maps, "米拉玛", "mlm", "Miramar.png");
        addMap(maps, "维寒迪", "whd", "Vikendi.png");
        addMap(maps, "帝斯顿", "dsd", "Deston.png");
        addMap(maps, "荣都", "rd", "Rondo.png");
        addMap(maps, "泰戈", "tg", "Tiger.png");
        return Collections.unmodifiableMap(maps);
    }

    private static void addMap(Map<String, String> maps, String name, String alias, String file) {
        maps.put(name, file);
        maps.put(alias, file);
    }
}
