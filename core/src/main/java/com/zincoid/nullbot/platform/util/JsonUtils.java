package com.zincoid.nullbot.platform.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zincoid.nullbot.platform.exception.PlatformException;

/**
 * JSON 解析工具。
 * <p>JSON parsing helper.</p>
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    /**
     * 将 JSON 解析为指定类型。
     * <p>Parses JSON into the given type.</p>
     *
     * @param json the raw JSON text
     * @param type the target type
     * @param <T>  the target type
     * @return the parsed value
     */
    public static <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new PlatformException("Malformed JSON: " + json, e);
        }
    }
}
