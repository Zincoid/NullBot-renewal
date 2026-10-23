package com.zincoid.nullbot.platform.util;

import java.net.URLConnection;
import java.util.Locale;

/**
 * MIME 类型识别工具：字节头嗅探、文件名推断与扩展名映射。
 * <p>MIME detection helpers: byte-header sniffing, filename inference, and extension mapping.</p>
 */
public final class MimeUtils {

    private MimeUtils() {
    }

    // ════ Detect ══════════════════

    /**
     * 根据字节头判断 MIME 类型。
     * <p>Detects the MIME type from the byte header.</p>
     *
     * @param bytes the file bytes
     * @return the MIME type, or null when unknown
     */
    public static String mime(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return null;
        // Images
        if (has(bytes, 0, "GIF")) return "image/gif";
        if (has(bytes, 0, "BM")) return "image/bmp";
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF)
            return "image/jpeg";
        if ((bytes[0] & 0xFF) == 0x89 && has(bytes, 1, "PNG")) return "image/png";
        // RIFF containers: WEBP / WAVE / AVI
        if (has(bytes, 0, "RIFF")) {
            if (has(bytes, 8, "WEBP")) return "image/webp";
            if (has(bytes, 8, "WAVE")) return "audio/wav";
            if (has(bytes, 8, "AVI ")) return "video/x-msvideo";
        }
        // ISO base media: ftyp at offset 4
        if (has(bytes, 4, "ftyp")) {
            if (has(bytes, 8, "M4A")) return "audio/mp4";
            if (has(bytes, 8, "qt")) return "video/quicktime";
            return "video/mp4";
        }
        // Audio
        if (has(bytes, 0, "ID3")) return "audio/mpeg";
        if ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xFF) == 0xFB
                || (bytes[1] & 0xFF) == 0xF3 || (bytes[1] & 0xFF) == 0xF2))
            return "audio/mpeg";
        if (has(bytes, 0, "OggS")) return "audio/ogg";
        if (has(bytes, 0, "fLaC")) return "audio/flac";
        // Video
        if ((bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45
                && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3)
            return "video/x-matroska";
        if (has(bytes, 0, "FLV")) return "video/x-flv";
        // Documents and archives
        if (has(bytes, 0, "%PDF")) return "application/pdf";
        if (has(bytes, 0, "PK") && bytes[2] == 0x03 && bytes[3] == 0x04) return "application/zip";
        if ((bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B) return "application/gzip";
        return null;
    }

    /**
     * 根据文件名推断 MIME 类型。
     * <p>Infers the MIME type from the filename.</p>
     *
     * @param name the filename
     * @return the MIME type, or null when unknown
     */
    public static String mime(String name) {
        if (name == null || name.isBlank()) return null;
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String known = switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "mp3" -> "audio/mpeg";
            case "ogg" -> "audio/ogg";
            case "wav" -> "audio/wav";
            case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "flv" -> "video/x-flv";
            case "pdf" -> "application/pdf";
            case "zip" -> "application/zip";
            case "gz" -> "application/gzip";
            default -> null;
        };
        if (known != null) return known;
        return URLConnection.guessContentTypeFromName(name);
    }

    // ════ Convert ═════════════════

    /**
     * 根据 MIME 类型推断文件扩展名。
     * <p>Infers the file extension from the MIME type.</p>
     *
     * @param mime the MIME type, nullable
     * @return the extension without a dot, or null when unknown
     */
    static String extension(String mime) {
        if (mime == null) return null;
        return switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            case "audio/wav" -> "wav";
            case "audio/flac" -> "flac";
            case "audio/mp4" -> "m4a";
            case "video/mp4" -> "mp4";
            case "video/quicktime" -> "mov";
            case "video/x-msvideo" -> "avi";
            case "video/x-matroska" -> "mkv";
            case "video/x-flv" -> "flv";
            case "application/pdf" -> "pdf";
            case "application/zip" -> "zip";
            case "application/gzip" -> "gz";
            default -> null;
        };
    }

    // ════ Helpers ═════════════════

    private static boolean has(byte[] bytes, int offset, String magic) {
        if (offset + magic.length() > bytes.length) return false;
        for (int i = 0; i < magic.length(); i++)
            if (bytes[offset + i] != (byte) magic.charAt(i)) return false;
        return true;
    }
}
