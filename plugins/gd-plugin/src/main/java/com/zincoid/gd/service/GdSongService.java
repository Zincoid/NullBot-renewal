package com.zincoid.gd.service;

import com.zincoid.gd.GdException;
import com.zincoid.nullbot.module.http.HttpService;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * GD 歌曲服务：按 Newgrounds 音频 ID 查询几何冲刺自定义歌曲。
 * <p>GD song service: queries Geometry Dash custom songs by Newgrounds audio id.</p>
 */
public final class GdSongService {

    private static final String SONG_INFO_URL =
            "https://www.boomlings.com/database/getGJSongInfo.php";
    private static final String SONG_SECRET = "Wmfd2893gb7";
    private static final int MAX_SONG_INFO_BYTES = 64 * 1024;

    private final GdHttp http;

    /**
     * 创建歌曲服务。
     * <p>Creates the song service.</p>
     *
     * @param http the GD HTTP adapter
     */
    public GdSongService(GdHttp http) {
        this.http = http;
    }

    /**
     * 按歌曲 ID 查询。
     * <p>Queries a song by id.</p>
     *
     * @param id the Newgrounds audio id
     * @return the song, or null when not found
     */
    public GdSong song(long id) {
        HttpService.Response response = http.postForm(SONG_INFO_URL,
                Map.of("songID", Long.toString(id), "secret", SONG_SECRET),
                MAX_SONG_INFO_BYTES);
        if (!response.ok())
            throw new GdException("GD song info returned HTTP " + response.status());
        String body = response.text().trim();
        if (body.startsWith("-")) return null;

        Map<String, String> fields = parse(body);
        String name = fields.get("2");
        String encodedUrl = fields.get("10");
        if (name == null || name.isBlank() || encodedUrl == null || encodedUrl.isBlank())
            throw new GdException("Unexpected GD song info response");
        String url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8);
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("https://") && !lower.startsWith("http://"))
            throw new GdException("Unsupported song download url");
        String author = fields.getOrDefault("4", "");
        return new GdSong(id, name, author, url);
    }

    // ════ Helpers ═════════════════

    private static Map<String, String> parse(String body) {
        String[] parts = body.split("~\\|~", -1);
        Map<String, String> fields = new HashMap<>();
        for (int i = 0; i + 1 < parts.length; i += 2)
            fields.put(parts[i], parts[i + 1]);
        return fields;
    }

    /**
     * 歌曲信息。
     * <p>Song information.</p>
     *
     * @param id     the song id
     * @param name   the song name
     * @param author the author
     * @param url    the download url
     */
    public record GdSong(long id, String name, String author, String url) {
    }
}
