package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * QQ 消息场景索引读取。
 * <p>Reads QQ message scene indexes.</p>
 */
final class QqScenes {

    private QqScenes() {
    }

    /**
     * 返回被引用消息索引。
     * <p>Returns the quoted message index.</p>
     *
     * @param scene the message scene
     * @return the quoted message index, or null when absent
     */
    static String refIndex(JsonNode scene) {
        return index(scene, "ref_msg_idx");
    }

    /**
     * 返回当前消息索引。
     * <p>Returns the current message index.</p>
     *
     * @param scene the message scene
     * @return the current message index, or null when absent
     */
    static String selfIndex(JsonNode scene) {
        return index(scene, "msg_idx");
    }

    private static String index(JsonNode scene, String key) {
        if (scene == null) return null;
        JsonNode direct = scene.get(key);
        if (direct != null && direct.isTextual()) return direct.asText();
        JsonNode ext = scene.get("ext");
        if (ext == null || !ext.isArray()) return null;
        String prefix = key + "=";
        for (JsonNode item : ext) {
            String value = item.asText("");
            if (value.startsWith(prefix)) return value.substring(prefix.length());
        }
        return null;
    }
}
