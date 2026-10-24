package com.zincoid.game;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * 游戏资源索引：扫描插件数据目录下的分类、角色与变体图片。
 * <p>Game resource index scanning category, character, and variant images under the plugin data directory.</p>
 */
final class GameResources {

    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private final Path root;

    /**
     * 创建资源索引。
     * <p>Creates the resource index.</p>
     *
     * @param root the plugin data directory
     */
    GameResources(Path root) {
        this.root = root;
    }

    /**
     * 一条游戏图片资源。
     * <p>One game image resource.</p>
     *
     * @param category  the category directory name
     * @param character the character name
     * @param variant   the variant label, empty when absent
     * @param path      the image path
     */
    record Resource(String category, String character, String variant, Path path) {
    }

    /**
     * 随机选取一个匹配分类或角色名的资源，关键词为空时从全部资源中选取。
     * <p>Picks a random resource matching the category or character name; a blank keyword picks from all resources.</p>
     *
     * @param keyword the exact category or character name, nullable
     * @return the resource, or null when none matches
     */
    Resource random(String keyword) {
        List<Resource> matches = all().stream()
                .filter(resource -> keyword == null || keyword.isBlank()
                        || resource.category().equals(keyword)
                        || resource.character().equals(keyword))
                .toList();
        if (matches.isEmpty()) return null;
        return matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
    }

    /**
     * 扫描全部资源，每次调用重新读取目录。
     * <p>Scans all resources, re-reading the directory on every call.</p>
     *
     * @return the resources
     */
    List<Resource> all() {
        List<Resource> resources = new ArrayList<>();
        if (!Files.isDirectory(root)) return resources;
        try (Stream<Path> categories = Files.list(root)) {
            categories.filter(Files::isDirectory)
                    .filter(path -> !hidden(path))
                    .forEach(path -> scanCategory(path, resources));
        } catch (IOException ignored) {
            // Keep the resources found so far.
        }
        return resources;
    }

    /**
     * 扫描一个分类目录。
     * <p>Scans one category directory.</p>
     *
     * @param category  the category directory
     * @param resources the resource sink
     */
    private void scanCategory(Path category, List<Resource> resources) {
        String name = category.getFileName().toString();
        try (Stream<Path> entries = Files.list(category)) {
            entries.filter(path -> !hidden(path)).forEach(entry -> {
                if (Files.isDirectory(entry)) scanCharacter(name, entry, resources);
                else if (isImage(entry)) addFile(name, entry, resources);
            });
        } catch (IOException ignored) {
            // Skip an unreadable category.
        }
    }

    /**
     * 扫描一个角色目录，目录下的图片都是该角色的变体。
     * <p>Scans one character directory; images inside are variants of that character.</p>
     *
     * @param category  the category name
     * @param directory the character directory
     * @param resources the resource sink
     */
    private void scanCharacter(String category, Path directory, List<Resource> resources) {
        String character = directory.getFileName().toString();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isImage)
                    .forEach(file -> resources.add(
                            new Resource(category, character, baseName(file), file)));
        } catch (IOException ignored) {
            // Skip an unreadable character directory.
        }
    }

    /**
     * 添加分类目录下的单个图片，文件名可用 {@code 角色_变体} 表示变体。
     * <p>Adds one image directly under a category; a {@code character_variant} filename denotes a variant.</p>
     *
     * @param category  the category name
     * @param file      the image file
     * @param resources the resource sink
     */
    private void addFile(String category, Path file, List<Resource> resources) {
        String name = baseName(file);
        String character = name;
        String variant = "";
        int split = name.lastIndexOf('_');
        if (split > 0 && split < name.length() - 1) {
            character = name.substring(0, split);
            variant = name.substring(split + 1);
        }
        if (character.isBlank()) return;
        resources.add(new Resource(category, character, variant, file));
    }

    /**
     * 判断文件是否为支持的图片扩展名。
     * <p>Checks whether the file has a supported image extension.</p>
     *
     * @param path the file path
     * @return true when the extension is supported
     */
    private boolean isImage(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * 返回不含扩展名的文件名。
     * <p>Returns the filename without its extension.</p>
     *
     * @param path the file path
     * @return the base name
     */
    private static String baseName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    /**
     * 判断是否为隐藏文件或目录。
     * <p>Checks whether the path is a hidden file or directory.</p>
     *
     * @param path the path
     * @return true when the name starts with a dot
     */
    private static boolean hidden(Path path) {
        return path.getFileName().toString().startsWith(".");
    }
}
