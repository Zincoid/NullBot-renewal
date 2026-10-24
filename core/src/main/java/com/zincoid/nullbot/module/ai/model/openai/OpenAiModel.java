package com.zincoid.nullbot.module.ai.model.openai;

import com.zincoid.nullbot.module.ai.AiException;
import com.zincoid.nullbot.module.ai.model.Model;
import com.zincoid.nullbot.module.ai.model.ModelReq;
import com.zincoid.nullbot.module.ai.model.ModelRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * OpenAI 兼容模型实现：调用 {@code /chat/completions}。
 * <p>OpenAI-compatible model implementation: calls {@code /chat/completions}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiModel implements Model {

    private final OpenAiProvider openAiProvider;
    private final RestClient client = buildClient();

    @Override
    public ModelRes invoke(ModelReq req) {
        OpenAiProvider.Provider provider = openAiProvider.current();
        OpenAiReq apiReq = OpenAiReq.from(req, provider.model());
        OpenAiRes apiRes;
        try {
            apiRes = client.post()
                    .uri(resolveUrl(provider.apiUrl()))
                    .header("Authorization", "Bearer " + provider.apiKey())
                    .body(apiReq)
                    .retrieve()
                    .body(OpenAiRes.class);
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (e.getStatusCode().is4xxClientError() && insufficient(body)) {
                log.warn("AI insufficient balance: status={}", e.getStatusCode().value());
                throw new AiException("AI insufficient balance");
            }
            throw e;
        }
        if (apiRes == null)
            throw new AiException("AI empty response");
        return apiRes.toModelRes();
    }

    /**
     * 构建共享的 HTTP 客户端。
     * <p>Builds the shared HTTP client.</p>
     *
     * @return the shared REST client
     */
    private static RestClient buildClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(180_000);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String resolveUrl(String apiUrl) {
        if (apiUrl.endsWith("/chat/completions")) return apiUrl;
        return (apiUrl.endsWith("/") ? apiUrl : apiUrl + "/") + "chat/completions";
    }

    private static boolean insufficient(String body) {
        return body != null && (body.contains("Insufficient Balance")
                || body.contains("InsufficientBalance")
                || body.contains("insufficient_balance")
                || body.contains("insufficient_quota")
                || body.contains("quota")
                || body.contains("余额"));
    }
}
