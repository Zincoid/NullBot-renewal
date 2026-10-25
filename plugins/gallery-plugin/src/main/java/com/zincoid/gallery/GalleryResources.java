package com.zincoid.gallery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * 图库资源索引：读取插件数据目录下各分类中的图片。
 * <p>Gallery resource index reading images from categories under the plugin data directory.</p>
 */
final class GalleryResources {

    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private final Path root;

    /**
     * 创建图库资源索引。
     * <p>Creates the gallery resource index.</p>
     *
     * @param root the plugin data directory
     */
    GalleryResources(Path root) {
        this.root = root;
    }

    /**
     * 从指定分类目录中随机选取一张图片，包含子目录中的图片。
     * <p>Picks a random image from the given category directory, including images in subdirectories.</p>
     *
     * @param category the category directory name
     * @return the image path, or null when the category has no images
     */
    Path randomImage(String category) {
        Path directory = root.resolve(category);
        if (!Files.isDirectory(directory)) return null;
        List<Path> images = new ArrayList<>();
        collect(directory, images);
        if (images.isEmpty()) return null;
        return images.get(ThreadLocalRandom.current().nextInt(images.size()));
    }

    /**
     * 递归收集目录中的图片，跳过隐藏条目。
     * <p>Collects images recursively, skipping hidden entries.</p>
     *
     * @param directory the directory to scan
     * @param images    the image sink
     */
    private static void collect(Path directory, List<Path> images) {
        try (Stream<Path> entries = Files.list(directory)) {
            entries.forEach(entry -> {
                if (hidden(entry)) return;
                if (Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) collect(entry, images);
                else if (Files.isRegularFile(entry) && isImage(entry)) images.add(entry);
            });
        } catch (IOException ignored) {
            // Skip an unreadable directory.
        }
    }

    /**
     * 判断文件是否为支持的图片扩展名。
     * <p>Checks whether the file has a supported image extension.</p>
     *
     * @param path the file path
     * @return true when the extension is supported
     */
    private static boolean isImage(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
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
