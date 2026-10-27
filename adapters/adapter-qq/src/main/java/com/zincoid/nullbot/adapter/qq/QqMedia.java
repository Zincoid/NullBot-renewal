package com.zincoid.nullbot.adapter.qq;

import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * QQ 富媒体解析与上传。
 * <p>QQ rich-media resolution and upload.</p>
 * <p>负责 URL、file、base64 媒体解析、文件名补全、大小限制与上传失败回退。</p>
 * <p>Handles URL, file, and base64 media resolution, filename completion, size limits, and upload fallbacks.</p>
 */
@Slf4j
@Component
public class QqMedia {

    private static final int MAX_LOCAL_MEDIA_BYTES = 5 * 1024 * 1024;

    private final QqApi api;
    private final QqHttpClient http;

    /**
     * 创建媒体发送器。
     * <p>Creates the media sender.</p>
     *
     * @param api  the API client
     * @param http the HTTP client
     */
    public QqMedia(QqApi api, QqHttpClient http) {
        this.api = api;
        this.http = http;
    }

    /**
     * 上传并直接发送一条媒体消息。
     * <p>Uploads and sends one media message directly.</p>
     *
     * @param media  the media segment
     * @param target the target conversation
     * @return the platform message id
     */
    public String send(MsgSegment.Media media, QqTarget target) {
        QqModels.UploadResponse response = upload(media, target);
        String id = response == null ? null : response.id();
        if (QqText.isBlank(id)) throw new QqException("Missing QQ media message id");
        return id;
    }

    private QqModels.UploadResponse upload(MsgSegment.Media media, QqTarget target) {
        int fileType = fileType(media);
        String defaultName = switch (fileType) {
            case 1 -> "image.png";
            case 2 -> "video.mp4";
            default -> "voice.silk";
        };
        String url = media.url();
        if (QqText.isBlank(url)) throw new QqException("Missing QQ media url");
        String sourceName = media.file();
        String fileName = fileName(sourceName, defaultName);
        if (QqText.isBlank(sourceName) && isHttp(url))
            fileName = fileName(url, defaultName);
        if (isHttp(url)) {
            try {
                return uploadUrl(fileType, url, fileName, target);
            } catch (QqException e) {
                if (!canRetryUpload(e)) throw e;
                log.debug("QQ url upload failed, retrying as bytes: {}", e.getMessage());
                byte[] bytes = http.download(url, MAX_LOCAL_MEDIA_BYTES);
                return uploadBytes(fileType, bytes, fileName, target);
            }
        }
        if (url.startsWith(MsgContent.BASE64_PREFIX)) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(url.substring(MsgContent.BASE64_PREFIX.length()));
            } catch (IllegalArgumentException e) {
                throw new QqException("Malformed QQ base64 media", e);
            }
            return uploadBytes(fileType, bytes, fileName, target);
        }
        if (url.startsWith("file://")) {
            try {
                Path path = Path.of(URI.create(url));
                if (Files.size(path) > MAX_LOCAL_MEDIA_BYTES)
                    throw new QqException("QQ local media exceeds 5 MB");
                if (QqText.isBlank(sourceName))
                    fileName = fileName(path.getFileName().toString(), defaultName);
                return uploadBytes(fileType, Files.readAllBytes(path), fileName, target);
            } catch (QqException e) {
                throw e;
            } catch (IOException | RuntimeException e) {
                throw new QqException("Cannot read QQ media file", e);
            }
        }
        throw new QqException("Unsupported QQ media url: " + url);
    }

    private QqModels.UploadResponse uploadUrl(int fileType, String url, String fileName,
                                              QqTarget target) {
        return api.upload(target,
                new QqModels.UploadRequest(fileType, url, null, fileName, true));
    }

    private QqModels.UploadResponse uploadBytes(int fileType, byte[] bytes, String fileName,
                                                QqTarget target) {
        if (bytes.length > MAX_LOCAL_MEDIA_BYTES)
            throw new QqException("QQ local media exceeds 5 MB");
        return api.upload(target, new QqModels.UploadRequest(
                fileType, null, Base64.getEncoder().encodeToString(bytes), fileName, true));
    }

    private static int fileType(MsgSegment.Media media) {
        if (media instanceof MsgSegment.Image) return 1;
        if (media instanceof MsgSegment.Video) return 2;
        return 3;
    }

    private static String fileName(String name, String defaultName) {
        String value = QqText.isBlank(name) ? defaultName : name;
        int query = value.indexOf('?');
        if (query >= 0) value = value.substring(0, query);
        int fragment = value.indexOf('#');
        if (fragment >= 0) value = value.substring(0, fragment);
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slash >= 0) value = value.substring(slash + 1);
        if (QqText.isBlank(value)) value = defaultName;
        if (!value.contains(".")) {
            int dot = defaultName.lastIndexOf('.');
            value += dot >= 0 ? defaultName.substring(dot) : "";
        }
        return value;
    }

    private static boolean isHttp(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private static boolean canRetryUpload(QqException e) {
        return switch (e.status()) {
            case 400, 403, 415, 422 -> true;
            default -> false;
        };
    }
}
