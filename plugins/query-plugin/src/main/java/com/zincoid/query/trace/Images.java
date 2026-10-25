package com.zincoid.query.trace;

import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.query.Http;
import com.zincoid.query.QueryException;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 图片读取工具：支持 base64、data URI、HTTP 与本地文件。
 * <p>Image loading helper supporting base64, data URIs, HTTP, and local files.</p>
 */
final class Images {

    private Images() {
    }

    /**
     * 读取图片字节。
     * <p>Reads the image bytes.</p>
     *
     * @param source    image source
     * @param userAgent HTTP user agent
     * @return the image bytes
     */
    static byte[] load(String source, String userAgent) {
        if (source == null || source.isBlank())
            throw new QueryException("Missing image source");
        try {
            if (source.startsWith(MsgContent.BASE64_PREFIX))
                return decode(source.substring(MsgContent.BASE64_PREFIX.length()));
            if (source.startsWith("data:")) {
                int comma = source.indexOf(',');
                if (comma < 0) throw new QueryException("Malformed data URI");
                return decode(source.substring(comma + 1));
            }
            if (source.startsWith("http://") || source.startsWith("https://"))
                return Http.getBytes(source, userAgent);
            if (source.startsWith("file:"))
                return Files.readAllBytes(Path.of(URI.create(source)));
            return Files.readAllBytes(Path.of(source));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new QueryException("Image download interrupted", e);
        } catch (IOException e) {
            throw new QueryException("Image download failed", e);
        }
    }

    private static byte[] decode(String value) {
        String payload = value.replaceAll("\\s+", "");
        try {
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            return Base64.getUrlDecoder().decode(payload);
        }
    }
}
