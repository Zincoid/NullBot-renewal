package com.zincoid.nullbot.adapter.qq;

import org.springframework.stereotype.Component;

/**
 * QQ 出站客户端工厂。
 * <p>QQ outbound client factory.</p>
 * <p>统一创建携带事件上下文或生命周期上下文的 {@link QqClient}。</p>
 * <p>Creates {@link QqClient} instances with event or lifecycle context.</p>
 */
@Component
public class QqClientFactory {

    private final QqApi api;
    private final QqMessageIndex index;
    private final QqProperties properties;
    private final QqMedia media;

    /**
     * 创建客户端工厂。
     * <p>Creates the client factory.</p>
     *
     * @param api        the API client
     * @param index      the message index
     * @param properties the adapter properties
     * @param media      the media sender
     */
    public QqClientFactory(QqApi api, QqMessageIndex index, QqProperties properties,
                           QqMedia media) {
        this.api = api;
        this.index = index;
        this.properties = properties;
        this.media = media;
    }

    /**
     * 创建出站客户端。
     * <p>Creates an outbound client.</p>
     *
     * @param selfId      the bot id
     * @param eventTarget the inbound event target, nullable for the lifecycle client
     * @return the client
     */
    public QqClient create(String selfId, QqTarget eventTarget) {
        return new QqClient(api, index, properties, selfId, eventTarget, media);
    }
}
