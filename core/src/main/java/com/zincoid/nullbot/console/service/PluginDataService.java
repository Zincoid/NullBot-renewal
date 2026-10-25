package com.zincoid.nullbot.console.service;

import com.zincoid.nullbot.bot.plugin.PluginException;
import com.zincoid.nullbot.bot.plugin.loader.PluginLoader;
import com.zincoid.nullbot.console.dto.PluginDataResult;
import com.zincoid.nullbot.console.dto.PluginDataView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.platform.util.AtomicFiles;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 插件数据目录服务：浏览、新建文件夹、上传、删除和下载插件数据文件。
 * <p>Plugin data service: browses, creates folders, uploads, deletes, and downloads plugin data files.</p>
 */
@Service
@RequiredArgsConstructor
public class PluginDataService {

    private final PluginLoader pluginLoader;

    // ════ Query ══════════════════

    /**
     * 列出插件数据目录中的条目。
     * <p>Lists the entries in a plugin data directory.</p>
     *
     * @param jar  the plugin jar filename
     * @param path the directory path relative to the data root, empty for the root
     * @return the directory view
     */
    public PluginDataView list(String jar, String path) {
        Path root = root(jar);
        Path dir = directory(root, path);
        List<PluginDataView.Entry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(item -> {
                try {
                    entries.add(entry(item));
                } catch (NoSuchFileException e) {
                    // The entry vanished between listing and reading its attributes.
                } catch (IOException e) {
                    throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Cannot read plugin data entry: " + e.getMessage(), e);
                }
            });
        } catch (NoSuchFileException e) {
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Directory not found: " + path);
        } catch (IOException e) {
            throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot list plugin data directory: " + e.getMessage(), e);
        }
        entries.sort(Comparator.comparing((PluginDataView.Entry value) -> !"dir".equals(value.type()))
                .thenComparing(PluginDataView.Entry::name, String.CASE_INSENSITIVE_ORDER));
        return new PluginDataView(relative(root, dir), entries);
    }

    // ════ Modify ═════════════════

    /**
     * 在插件数据目录中新建文件夹。
     * <p>Creates a folder in a plugin data directory.</p>
     *
     * @param jar  the plugin jar filename
     * @param path the parent path relative to the data root, empty for the root
     * @param name the new folder name
     * @return the created entry
     */
    public PluginDataView.Entry createFolder(String jar, String path, String name) {
        Path root = root(jar);
        Path parent = directory(root, path);
        String folder = childName(name, true);
        Path target = parent.resolve(folder);
        try {
            Files.createDirectory(target);
            return entry(target);
        } catch (FileAlreadyExistsException e) {
            throw new ConsoleException(HttpStatus.CONFLICT, "Folder already exists: " + folder);
        } catch (IOException e) {
            throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot create folder: " + e.getMessage(), e);
        }
    }

    /**
     * 批量上传文件，同名文件直接覆盖。
     * <p>Uploads files in batch, overwriting files with the same name.</p>
     *
     * @param jar   the plugin jar filename
     * @param path  the target directory path relative to the data root, empty for the root
     * @param files the uploaded files
     * @return the per-file results
     */
    public PluginDataResult upload(String jar, String path, List<MultipartFile> files) {
        if (files == null || files.isEmpty())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "No files");
        Path root = root(jar);
        Path dir = directory(root, path);
        List<PluginDataResult.Item> items = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null) continue;
            String original = file.getOriginalFilename();
            String name = FilenameUtils.getName(original);
            String label = name == null || name.isBlank() ? "?" : name;
            try {
                String targetName = childName(name, false);
                Path target = dir.resolve(targetName);
                if (Files.isDirectory(target))
                    throw new IOException("Target is a directory");
                write(file, dir, target);
                items.add(new PluginDataResult.Item(targetName, true, null));
            } catch (IOException | ConsoleException e) {
                items.add(failed(label, e));
            }
        }
        return new PluginDataResult(items);
    }

    // ════ Delete ═════════════════

    /**
     * 批量删除插件数据条目，文件夹递归删除。
     * <p>Deletes plugin data entries in batch, removing folders recursively.</p>
     *
     * @param jar   the plugin jar filename
     * @param path  the parent path relative to the data root, empty for the root
     * @param names the entry names to delete
     * @return the per-item results
     */
    public PluginDataResult delete(String jar, String path, List<String> names) {
        if (names == null || names.isEmpty())
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Names are required");
        Path root = root(jar);
        Path dir = directory(root, path);
        List<PluginDataResult.Item> items = new ArrayList<>();
        for (String name : names) {
            String label = name == null ? "?" : name;
            try {
                String targetName = childName(name, false);
                Path target = dir.resolve(targetName);
                if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Not found");
                deleteRecursively(root, target, targetName);
                items.add(new PluginDataResult.Item(targetName, true, null));
            } catch (IOException | ConsoleException e) {
                items.add(failed(label, e));
            }
        }
        return new PluginDataResult(items);
    }

    // ════ Download ═══════════════

    /**
     * 返回可下载的插件数据文件路径。
     * <p>Returns the path of a downloadable plugin data file.</p>
     *
     * @param jar  the plugin jar filename
     * @param path the file path relative to the data root
     * @return the file path
     */
    public Path file(String jar, String path) {
        Path root = root(jar);
        Path target = resolve(root, path);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "File not found: " + path);
        Path real = real(root, target, path);
        if (!Files.isRegularFile(real))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "File not found: " + path);
        return target;
    }

    // ════ Helpers ════════════════

    private Path root(String jar) {
        Path root;
        try {
            root = pluginLoader.dataDir(jar).orElseThrow(() ->
                    new ConsoleException(HttpStatus.NOT_FOUND, "Plugin data unavailable: " + jar));
        } catch (PluginException e) {
            throw new ConsoleException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        try {
            Files.createDirectories(root);
            return root.toRealPath();
        } catch (IOException e) {
            throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot access plugin data directory: " + e.getMessage(), e);
        }
    }

    private static Path resolve(Path root, String relative) {
        if (relative == null || relative.isEmpty()) return root;
        if (relative.indexOf('\0') >= 0 || relative.indexOf('\\') >= 0)
            throw invalidPath(relative);
        Path target = root;
        for (String segment : relative.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
                    || segment.chars().anyMatch(Character::isISOControl))
                throw invalidPath(relative);
            if (File.separatorChar == '\\' && segment.indexOf(':') >= 0)
                throw invalidPath(relative);
            target = target.resolve(segment);
        }
        target = target.normalize();
        if (!target.startsWith(root))
            throw invalidPath(relative);
        return target;
    }

    private static Path directory(Path root, String relative) {
        Path path = resolve(root, relative);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Directory not found: " + relative);
        Path real = real(root, path, relative);
        if (!Files.isDirectory(real))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Directory not found: " + relative);
        return path;
    }

    private static Path real(Path root, Path target, String relative) {
        try {
            Path value = target.toRealPath();
            if (!value.startsWith(root))
                throw invalidPath(relative);
            return value;
        } catch (NoSuchFileException e) {
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Path not found: " + relative);
        } catch (IOException e) {
            throw new ConsoleException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot resolve path: " + e.getMessage(), e);
        }
    }

    private static String childName(String raw, boolean trim) {
        String value = trim && raw != null ? raw.trim() : raw;
        if (value == null || value.isEmpty()
                || ".".equals(value) || "..".equals(value)
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.indexOf('\0') >= 0
                || value.chars().anyMatch(Character::isISOControl)
                || (File.separatorChar == '\\' && value.indexOf(':') >= 0))
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid name: " + raw);
        if (value.length() > 255)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Name too long");
        return value;
    }

    private static PluginDataView.Entry entry(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        boolean directory = !attrs.isSymbolicLink() && attrs.isDirectory();
        return new PluginDataView.Entry(path.getFileName().toString(),
                directory ? "dir" : "file",
                directory ? 0 : attrs.size(),
                LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault())
                        .toString());
    }

    private static String relative(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static PluginDataResult.Item failed(String name, Exception e) {
        String message = e.getMessage();
        return new PluginDataResult.Item(name, false,
                message == null || message.isBlank() ? e.getClass().getSimpleName() : message);
    }

    private static void write(MultipartFile file, Path dir, Path target) throws IOException {
        Path temp = Files.createTempFile(dir, ".upload-", ".tmp");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            AtomicFiles.moveReplace(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void deleteRecursively(Path root, Path target, String name) throws IOException {
        if (Files.isSymbolicLink(target)
                || !Files.isDirectory(target, LinkOption.NOFOLLOW_LINKS)) {
            Files.delete(target);
            return;
        }
        Path real = target.toRealPath();
        if (!real.startsWith(root))
            throw invalidPath(name);
        Files.walkFileTree(target, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static ConsoleException invalidPath(String relative) {
        return new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid path: " + relative);
    }
}
