package com.zincoid.gd.store;

import com.zincoid.gd.GdException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * GD 本地进度存储：插件数据目录中的 SQLite 数据库。
 * <p>GD local progress store: a SQLite database in the plugin data directory.</p>
 */
@Slf4j
public final class GdStore implements AutoCloseable {

    private static final int SCHEMA_VERSION = 1;
    private static final int QUERY_CHUNK = 500;

    private static final String CREATE_USER = """
            CREATE TABLE IF NOT EXISTS gd_user (
                user_id    TEXT    NOT NULL PRIMARY KEY,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """;

    private static final String CREATE_PROGRESS = """
            CREATE TABLE IF NOT EXISTS gd_progress (
                user_id    TEXT    NOT NULL,
                level_id   INTEGER NOT NULL CHECK (level_id > 0),
                progress   INTEGER NOT NULL CHECK (progress BETWEEN 1 AND 100),
                level_name TEXT    NOT NULL,
                author     TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, level_id),
                FOREIGN KEY (user_id) REFERENCES gd_user(user_id) ON DELETE CASCADE
            )
            """;

    private static final String CREATE_PROGRESS_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_gd_progress_level
                ON gd_progress(level_id)
            """;

    private static final String CREATE_LEVEL_CACHE = """
            CREATE TABLE IF NOT EXISTS gd_level_cache (
                level_id   INTEGER NOT NULL PRIMARY KEY,
                rating     REAL,
                tier       INTEGER,
                fetched_at INTEGER NOT NULL
            )
            """;

    private final Path file;
    private final Connection connection;
    private boolean closed;

    private GdStore(Path file, Connection connection) {
        this.file = file;
        this.connection = connection;
    }

    /**
     * 打开插件数据目录中的 {@code gd.db}，不存在时自动建表。
     * <p>Opens {@code gd.db} in the plugin data directory and creates its schema when absent.</p>
     *
     * @param dataDir the plugin data directory
     * @return the opened store
     */
    public static GdStore open(Path dataDir) {
        Path file = dataDir.resolve("gd.db").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dataDir);
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + file.toString().replace('\\', '/');
            Connection connection = DriverManager.getConnection(url);
            GdStore store = new GdStore(file, connection);
            try {
                store.initialize();
            } catch (RuntimeException e) {
                store.close();
                throw e;
            }
            log.info("GD progress database opened: {}", file);
            return store;
        } catch (GdException e) {
            throw e;
        } catch (Exception e) {
            throw new GdException("Failed to open GD progress database: " + file, e);
        }
    }

    // ════ User ═══════════════════

    /**
     * 用户是否已注册。
     * <p>Whether the user is registered.</p>
     *
     * @param userId the platform user id
     * @return true when registered
     */
    public synchronized boolean isRegistered(String userId) {
        requireOpen();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM gd_user WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw error("Failed to read GD user", e);
        }
    }

    /**
     * 注册用户，已注册时返回 false。
     * <p>Registers a user and returns false when already registered.</p>
     *
     * @param userId the platform user id
     * @return true when a new row was inserted
     */
    public synchronized boolean register(String userId) {
        requireOpen();
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO gd_user(user_id, created_at, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(user_id) DO NOTHING
                """)) {
            ps.setString(1, userId);
            ps.setLong(2, now);
            ps.setLong(3, now);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw error("Failed to register GD user", e);
        }
    }

    /**
     * 注销用户并删除其全部进度。
     * <p>Unregisters a user and deletes all of their progress.</p>
     *
     * @param userId the platform user id
     * @return the number of deleted progress rows
     */
    public synchronized int unregister(String userId) {
        requireOpen();
        try {
            connection.setAutoCommit(false);
            int count;
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM gd_progress WHERE user_id = ?")) {
                ps.setString(1, userId);
                count = ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM gd_user WHERE user_id = ?")) {
                ps.setString(1, userId);
                ps.executeUpdate();
            }
            connection.commit();
            return count;
        } catch (SQLException e) {
            rollback();
            throw error("Failed to unregister GD user", e);
        } finally {
            autoCommit();
        }
    }

    // ════ Progress ═══════════════

    /**
     * 新增或覆盖一条进度记录，并刷新用户的更新时间。
     * <p>Inserts or replaces a progress record and refreshes the user timestamp.</p>
     *
     * @param userId    the platform user id
     * @param levelId   the GD level id
     * @param progress  the completion percentage, 1-100
     * @param levelName the level name snapshot
     * @param author    the level author snapshot, nullable
     */
    public synchronized void upsertProgress(String userId, long levelId, int progress,
                                            String levelName, String author) {
        requireOpen();
        long now = System.currentTimeMillis();
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO gd_progress(
                        user_id, level_id, progress, level_name, author, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(user_id, level_id) DO UPDATE SET
                        progress = excluded.progress,
                        level_name = excluded.level_name,
                        author = excluded.author,
                        updated_at = excluded.updated_at
                    """)) {
                ps.setString(1, userId);
                ps.setLong(2, levelId);
                ps.setInt(3, progress);
                ps.setString(4, levelName);
                ps.setString(5, author);
                ps.setLong(6, now);
                ps.setLong(7, now);
                ps.executeUpdate();
            }
            touchUser(userId, now);
            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw error("Failed to save GD progress", e);
        } finally {
            autoCommit();
        }
    }

    /**
     * 删除一条进度记录，并刷新用户的更新时间。
     * <p>Deletes a progress record and refreshes the user timestamp.</p>
     *
     * @param userId  the platform user id
     * @param levelId the GD level id
     * @return true when a row was deleted
     */
    public synchronized boolean removeProgress(String userId, long levelId) {
        requireOpen();
        long now = System.currentTimeMillis();
        try {
            connection.setAutoCommit(false);
            int count;
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM gd_progress WHERE user_id = ? AND level_id = ?")) {
                ps.setString(1, userId);
                ps.setLong(2, levelId);
                count = ps.executeUpdate();
            }
            if (count > 0) touchUser(userId, now);
            connection.commit();
            return count > 0;
        } catch (SQLException e) {
            rollback();
            throw error("Failed to remove GD progress", e);
        } finally {
            autoCommit();
        }
    }

    /**
     * 返回用户的全部进度记录。
     * <p>Returns all progress records of a user.</p>
     *
     * @param userId the platform user id
     * @return the progress records
     */
    public synchronized List<ProgressRecord> progress(String userId) {
        requireOpen();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT level_id, progress, level_name, author, created_at, updated_at
                FROM gd_progress
                WHERE user_id = ?
                ORDER BY updated_at DESC, level_id
                """)) {
            ps.setString(1, userId);
            List<ProgressRecord> records = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(new ProgressRecord(
                            rs.getLong("level_id"),
                            rs.getInt("progress"),
                            rs.getString("level_name"),
                            rs.getString("author"),
                            rs.getLong("created_at"),
                            rs.getLong("updated_at")
                    ));
                }
            }
            return records;
        } catch (SQLException e) {
            throw error("Failed to read GD progress", e);
        }
    }

    /**
     * 返回用户的进度记录数量。
     * <p>Returns the number of progress records of a user.</p>
     *
     * @param userId the platform user id
     * @return the record count
     */
    public synchronized int progressCount(String userId) {
        requireOpen();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM gd_progress WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw error("Failed to count GD progress", e);
        }
    }

    // ════ Level cache ════════════

    /**
     * 读取指定关卡的 GDDL 评级缓存。
     * <p>Reads the GDDL rating cache for the given levels.</p>
     *
     * @param levelIds the level ids
     * @return the cached entries by level id
     */
    public synchronized Map<Long, CachedTier> cachedTiers(Collection<Long> levelIds) {
        requireOpen();
        if (levelIds.isEmpty()) return Map.of();
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(levelIds));
        Map<Long, CachedTier> result = new LinkedHashMap<>();
        for (int start = 0; start < ids.size(); start += QUERY_CHUNK) {
            List<Long> chunk = ids.subList(start, Math.min(start + QUERY_CHUNK, ids.size()));
            String placeholders = String.join(", ", java.util.Collections.nCopies(chunk.size(), "?"));
            String sql = "SELECT level_id, rating, tier, fetched_at FROM gd_level_cache "
                    + "WHERE level_id IN (" + placeholders + ")";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < chunk.size(); i++) ps.setLong(i + 1, chunk.get(i));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long levelId = rs.getLong("level_id");
                        double ratingValue = rs.getDouble("rating");
                        Double rating = rs.wasNull() ? null : ratingValue;
                        int tierValue = rs.getInt("tier");
                        Integer tier = rs.wasNull() ? null : tierValue;
                        result.put(levelId, new CachedTier(
                                levelId, rating, tier, rs.getLong("fetched_at")));
                    }
                }
            } catch (SQLException e) {
                throw error("Failed to read GD level cache", e);
            }
        }
        return result;
    }

    /**
     * 批量写入 GDDL 评级缓存。
     * <p>Writes the GDDL rating cache in one batch.</p>
     *
     * @param tiers the cached entries
     */
    public synchronized void saveTiers(Collection<CachedTier> tiers) {
        requireOpen();
        if (tiers.isEmpty()) return;
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO gd_level_cache(level_id, rating, tier, fetched_at)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(level_id) DO UPDATE SET
                        rating = excluded.rating,
                        tier = excluded.tier,
                        fetched_at = excluded.fetched_at
                    """)) {
                for (CachedTier tier : tiers) {
                    ps.setLong(1, tier.levelId());
                    if (tier.rating() == null) ps.setNull(2, Types.REAL);
                    else ps.setDouble(2, tier.rating());
                    if (tier.tier() == null) ps.setNull(3, Types.INTEGER);
                    else ps.setInt(3, tier.tier());
                    ps.setLong(4, tier.fetchedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            rollback();
            throw error("Failed to save GD level cache", e);
        } finally {
            autoCommit();
        }
    }

    // ════ Lifecycle ══════════════

    /**
     * 关闭数据库连接。
     * <p>Closes the database connection.</p>
     */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(TRUNCATE)");
        } catch (SQLException e) {
            log.warn("GD database checkpoint failed: {}", file, e);
        }
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("GD database close failed: {}", file, e);
        }
    }

    // ════ Helpers ═════════════════

    private void initialize() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            throw error("Failed to configure GD database", e);
        }

        int version = userVersion();
        if (version > SCHEMA_VERSION) {
            throw new GdException("GD database schema is newer than this plugin: "
                    + version + " > " + SCHEMA_VERSION);
        }
        if (version < 1) {
            createSchema();
            setUserVersion(SCHEMA_VERSION);
        }
    }

    private void createSchema() {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_USER);
            statement.execute(CREATE_PROGRESS);
            statement.execute(CREATE_PROGRESS_INDEX);
            statement.execute(CREATE_LEVEL_CACHE);
        } catch (SQLException e) {
            throw error("Failed to create GD database schema", e);
        }
    }

    private int userVersion() {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw error("Failed to read GD database version", e);
        }
    }

    private void setUserVersion(int version) {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        } catch (SQLException e) {
            throw error("Failed to write GD database version", e);
        }
    }

    private void touchUser(String userId, long now) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE gd_user SET updated_at = ? WHERE user_id = ?")) {
            ps.setLong(1, now);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    private void requireOpen() {
        if (closed) throw new GdException("GD progress database is closed");
    }

    private void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            log.warn("GD database rollback failed", e);
        }
    }

    private void autoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            log.warn("GD database auto-commit restore failed", e);
        }
    }

    private GdException error(String message, SQLException e) {
        return new GdException(message + ": " + e.getMessage(), e);
    }
}
