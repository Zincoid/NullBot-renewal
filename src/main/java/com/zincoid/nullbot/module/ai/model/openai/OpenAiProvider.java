package com.zincoid.nullbot.module.ai.model.openai;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容提供方：从数据库配置读取当前生效的地址、密钥与模型。
 * <p>OpenAI-compatible provider: reads the active URL, key, and model from the database configuration.</p>
 */
@Component
@RequiredArgsConstructor
public class OpenAiProvider {

    private final ConfigService configService;

    /**
     * 读取当前提供方配置。
     * <p>Reads the current provider configuration.</p>
     *
     * @return the provider configuration
     */
    public Provider current() {
        String apiUrl = configService.get(Conf.AI_PROVIDER_API_URL);
        String apiKey = configService.get(Conf.AI_PROVIDER_API_KEY);
        String model = configService.get(Conf.AI_PROVIDER_MODEL);
        if (blank(apiUrl) || blank(apiKey) || blank(model))
            throw new AiException("AI provider not configured");
        return new Provider(apiUrl, apiKey, model);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 提供方配置。
     * <p>Provider configuration.</p>
     *
     * @param apiUrl the OpenAI-compatible base URL
     * @param apiKey the bearer token
     * @param model  the model name
     */
    public record Provider(String apiUrl, String apiKey, String model) {
    }
}
