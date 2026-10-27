package com.zincoid.nullbot.adapter.qq;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 平台消息 ID 与内部短 ID 的转换。
 * <p>Converts platform message ids to stable internal short ids.</p>
 * <p>QQ 官方消息 ID 可能超过数据库的 64 字符限制，短 ID 由平台 ID 的 SHA-256 生成。</p>
 * <p>QQ official message ids may exceed the database's 64-character limit; short ids are derived from their SHA-256 digest.</p>
 */
public final class QqIds {

    private QqIds() {
    }

    /**
     * 生成稳定的内部消息 ID。
     * <p>Builds a stable internal message id.</p>
     *
     * @param messageId the platform message id
     * @return the short message id, at most 64 characters
     */
    public static String shortId(String messageId) {
        if (messageId == null || messageId.isBlank())
            throw new QqException("Missing QQ message id");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(messageId.getBytes(StandardCharsets.UTF_8));
            return "q" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
