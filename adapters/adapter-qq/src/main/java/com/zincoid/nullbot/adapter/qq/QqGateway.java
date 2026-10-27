package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import com.zincoid.nullbot.bot.spi.port.BotLifecycle;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QQ 官方机器人 WebSocket 网关。
 * <p>QQ official bot WebSocket gateway.</p>
 * <p>负责 token、Identify/Resume、心跳、重连、事件映射和生命周期。</p>
 * <p>Handles tokens, Identify/Resume, heartbeats, reconnects, event mapping, and lifecycle.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QqGateway {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final QqApi api;
    private final QqProperties properties;
    private final QqMessageIndex index;
    private final BotServer server;
    private final BotLifecycle lifecycle;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final Object sendLock = new Object();
    private final AtomicInteger reconnectAttempt = new AtomicInteger();

    private volatile boolean shutdown;
    private volatile boolean fatal;
    private volatile boolean online;
    private volatile String selfId;
    private volatile String botName;
    private volatile String sessionId;
    private volatile Long lastSeq;
    private volatile String gatewayUrl;
    private volatile WebSocket socket;
    private volatile CompletableFuture<Void> closed;
    private volatile ScheduledExecutorService heartbeat;
    private volatile boolean awaitingHeartbeatAck;
    private volatile Thread thread;

    // ════ Lifecycle ══════════════

    /**
     * 启动网关线程；未配置凭据时跳过。
     * <p>Starts the gateway thread; skipped when credentials are missing.</p>
     */
    @PostConstruct
    void start() {
        if (!properties.configured()) {
            log.warn("QQ adapter disabled: configure nullbot.qq.app-id and nullbot.qq.app-secret");
            return;
        }
        shutdown = false;
        fatal = false;
        thread = Thread.ofVirtual().name("qq-gateway").start(this::run);
        log.info("QQ adapter starting: api={}, intents={}",
                properties.resolvedApiBase(), properties.getIntents());
    }

    /**
     * 停止网关并注销账号。
     * <p>Stops the gateway and unregisters the account.</p>
     */
    @PreDestroy
    void stop() {
        if (shutdown) return;
        shutdown = true;
        Thread current = thread;
        if (current != null) current.interrupt();
        closeCurrent();
        stopHeartbeat();
        offline();
        log.info("QQ adapter stopped");
    }

    // ════ Gateway loop ═══════════

    private void run() {
        while (!shutdown && !fatal) {
            try {
                connect();
            } catch (RuntimeException e) {
                log.warn("QQ gateway error: {}", e.getMessage());
            }
            if (shutdown || fatal) break;
            long delay = delay(reconnectAttempt.getAndIncrement());
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("QQ gateway stopped");
    }

    private void connect() {
        String url = gatewayUrl();
        String token = api.token();
        CompletableFuture<Void> done = new CompletableFuture<>();
        closed = done;
        Listener listener = new Listener(done);
        WebSocket ws;
        try {
            ws = http.newWebSocketBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .header("Authorization", "QQBot " + token)
                    .buildAsync(URI.create(url), listener)
                    .join();
        } catch (RuntimeException e) {
            closed = null;
            throw new QqException("Cannot connect QQ gateway", e);
        }
        socket = ws;
        try {
            done.join();
        } finally {
            cleanup();
        }
    }

    private String gatewayUrl() {
        String current = gatewayUrl;
        if (current != null && !current.isBlank()) return current;
        String url = api.gatewayUrl();
        gatewayUrl = url;
        return url;
    }

    private void cleanup() {
        stopHeartbeat();
        offline();
        socket = null;
        closed = null;
    }

    private static long delay(long attempt) {
        long seconds = Math.min(60, 1L << Math.min(attempt, 6));
        return seconds * 1000;
    }

    // ════ Payload ════════════════

    private void handlePayload(String json) {
        QqModels.Payload payload;
        try {
            payload = api.parsePayload(json);
        } catch (RuntimeException e) {
            log.warn("QQ gateway payload decode failed: {}", e.getMessage());
            return;
        }
        if (payload == null) return;
        if (payload.s() != null) lastSeq = payload.s();
        switch (payload.op()) {
            case 0 -> dispatch(payload);
            case 7 -> {
                log.info("QQ gateway reconnect requested");
                closeCurrent();
            }
            case 9 -> {
                log.warn("QQ session invalid");
                clearSession();
                closeCurrent();
            }
            case 10 -> {
                startHeartbeat(payload.d());
                sendIdentifyOrResume();
            }
            case 11 -> awaitingHeartbeatAck = false;
            default -> log.debug("QQ opcode ignored: {}", payload.op());
        }
    }

    private void dispatch(QqModels.Payload payload) {
        String type = payload.t();
        if (type == null) return;
        switch (type) {
            case "READY" -> ready(payload.d());
            case "RESUMED" -> {
                reconnectAttempt.set(0);
                online();
                log.info("QQ session resumed");
            }
            case "GROUP_AT_MESSAGE_CREATE" -> handleGroup(payload.d(), true);
            case "GROUP_MESSAGE_CREATE" -> handleGroup(payload.d(), false);
            case "C2C_MESSAGE_CREATE" -> handleC2c(payload.d());
            default -> log.debug("QQ event ignored: {}", type);
        }
    }

    private void ready(JsonNode node) {
        QqModels.ReadyData data = api.read(node, QqModels.ReadyData.class);
        if (data == null || data.user() == null || isBlank(data.user().id())) {
            log.warn("QQ READY missing bot user");
            return;
        }
        selfId = data.user().id();
        botName = data.user().username();
        sessionId = data.sessionId();
        api.setBotInfo(new QqModels.BotInfo(selfId, botName, data.user().avatar()));
        reconnectAttempt.set(0);
        awaitingHeartbeatAck = false;
        online();
        log.info("QQ gateway ready: bot={}", selfId);
    }

    // ════ Events ═════════════════

    private void handleGroup(JsonNode node, boolean atEvent) {
        if (selfId == null) return;
        QqModels.GroupMessage data = api.read(node, QqModels.GroupMessage.class);
        if (data == null || isBlank(data.id()) || isBlank(data.groupOpenid())) return;
        if (skip(data.author())) return;
        String shortId = QqIds.shortId(data.id());
        QqMessageIndex.Target target = new QqMessageIndex.Target(true, data.groupOpenid());
        GroupMsgEvent event = atEvent
                ? QqMapper.groupAt(data, selfId, shortId)
                : QqMapper.groupFull(data, selfId, shortId);
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        if (!index.remember(data.id(), target, detail)) return;
        server.handle(new QqClient(api, index, properties, selfId,
                new QqClient.ReplyContext(target, data.id(), receivedAt(data.timestamp()))), event);
    }

    private void handleC2c(JsonNode node) {
        if (selfId == null) return;
        QqModels.C2cMessage data = api.read(node, QqModels.C2cMessage.class);
        if (data == null || isBlank(data.id()) || data.author() == null) return;
        if (skip(data.author())) return;
        String userId = firstNonBlank(data.author().userOpenid(), data.author().id());
        if (isBlank(userId)) return;
        String shortId = QqIds.shortId(data.id());
        QqMessageIndex.Target target = new QqMessageIndex.Target(false, userId);
        PrivateMsgEvent event = QqMapper.c2c(data, selfId, shortId);
        if (event == null) return;
        rememberNames(data.author(), data.mentions());
        MsgDetail detail = new MsgDetail(shortId, event.userId(), event.nickname(), event.content());
        if (!index.remember(data.id(), target, detail)) return;
        server.handle(new QqClient(api, index, properties, selfId,
                new QqClient.ReplyContext(target, data.id(), receivedAt(data.timestamp()))), event);
    }

    private boolean skip(QqModels.Author author) {
        if (author == null || Boolean.TRUE.equals(author.bot())) return true;
        if (selfId != null
                && (selfId.equals(author.id()) || selfId.equals(author.memberOpenid())))
            return true;
        return botName != null && !botName.isBlank() && botName.equals(author.username());
    }

    private void rememberNames(QqModels.Author author, List<QqModels.Mention> mentions) {
        if (author != null) {
            index.rememberUser(author.id(), author.username());
            index.rememberUser(author.memberOpenid(), author.username());
            index.rememberUser(author.userOpenid(), author.username());
        }
        if (mentions != null) {
            for (QqModels.Mention mention : mentions) {
                if (mention == null) continue;
                index.rememberUser(mention.id(), mention.username());
                index.rememberUser(mention.memberOpenid(), mention.username());
                index.rememberUser(mention.userOpenid(), mention.username());
            }
        }
    }

    private static Instant receivedAt(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) return Instant.now();
        try {
            return OffsetDateTime.parse(timestamp).toInstant();
        } catch (RuntimeException e) {
            return Instant.now();
        }
    }

    // ════ Heartbeat ══════════════

    private void startHeartbeat(JsonNode data) {
        stopHeartbeat();
        int interval = data == null ? 45_000 : data.path("heartbeat_interval").asInt(45_000);
        if (interval < 1000) interval = 45_000;
        awaitingHeartbeatAck = false;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "qq-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat = scheduler;
        scheduler.scheduleAtFixedRate(this::heartbeatTick, interval, interval, TimeUnit.MILLISECONDS);
    }

    private void heartbeatTick() {
        if (shutdown) return;
        if (awaitingHeartbeatAck) {
            log.warn("QQ heartbeat ack missing");
            closeCurrent();
            return;
        }
        awaitingHeartbeatAck = true;
        String sequence = lastSeq == null ? "null" : lastSeq.toString();
        try {
            sendRaw("{\"op\":1,\"d\":" + sequence + "}");
        } catch (RuntimeException e) {
            log.warn("QQ heartbeat failed: {}", e.getMessage());
            closeCurrent();
        }
    }

    private void stopHeartbeat() {
        ScheduledExecutorService scheduler = heartbeat;
        heartbeat = null;
        if (scheduler != null) scheduler.shutdownNow();
    }

    private void sendIdentifyOrResume() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("token", "QQBot " + api.token());
            Map<String, Object> payload = new HashMap<>();
            if (sessionId != null && lastSeq != null) {
                data.put("session_id", sessionId);
                data.put("seq", lastSeq);
                payload.put("op", 6);
                payload.put("d", data);
                sendJson(payload);
                log.info("QQ gateway resume requested");
            } else {
                data.put("intents", properties.getIntents());
                data.put("shard", List.of(properties.getShard(), properties.resolvedShardCount()));
                data.put("properties", Map.of(
                        "$os", "linux",
                        "$browser", "nullbot",
                        "$device", "nullbot"));
                payload.put("op", 2);
                payload.put("d", data);
                sendJson(payload);
                log.info("QQ gateway identify sent");
            }
        } catch (RuntimeException e) {
            log.warn("QQ gateway identify failed: {}", e.getMessage());
            closeCurrent();
        }
    }

    private void sendJson(Object payload) {
        sendRaw(api.json(payload));
    }

    private void sendRaw(String json) {
        synchronized (sendLock) {
            WebSocket ws = socket;
            if (ws == null || ws.isOutputClosed())
                throw new QqException("QQ gateway is closed");
            try {
                ws.sendText(json, true).join();
            } catch (CompletionException e) {
                throw new QqException("QQ gateway send failed", e);
            }
        }
    }

    // ════ Account lifecycle ══════

    private void online() {
        if (online || selfId == null || shutdown) return;
        try {
            lifecycle.online(selfId, new QqClient(api, index, properties, selfId, null));
            online = true;
            log.info("Bot online: {}", selfId);
        } catch (RuntimeException e) {
            log.warn("QQ bot online failed: {}", selfId, e);
        }
    }

    private void offline() {
        if (!online || selfId == null) return;
        try {
            lifecycle.offline(selfId);
        } catch (RuntimeException e) {
            log.warn("QQ bot offline failed: {}", selfId, e);
        }
        online = false;
        log.info("Bot offline: {}", selfId);
    }

    // ════ Connection control ═════

    private void closeCurrent() {
        WebSocket ws = socket;
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "reconnect");
            } catch (RuntimeException ignored) {
                // The socket may already be closed.
            }
            try {
                ws.abort();
            } catch (RuntimeException ignored) {
                // The socket may already be closed.
            }
        }
        CompletableFuture<Void> done = closed;
        if (done != null) done.complete(null);
    }

    private void clearSession() {
        sessionId = null;
        lastSeq = null;
    }

    private void handleCloseCode(int code) {
        if (code == 4006 || code == 4007 || (code >= 4900 && code <= 4913)) clearSession();
        if (fatalClose(code)) {
            fatal = true;
            log.error("QQ gateway fatal close: code={}", code);
        }
    }

    private static boolean fatalClose(int code) {
        return code == 4001 || code == 4002
                || (code >= 4010 && code <= 4014)
                || code == 4914 || code == 4915;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    // ════ Listener ═══════════════

    private final class Listener implements WebSocket.Listener {

        private final CompletableFuture<Void> done;
        private final StringBuilder buffer = new StringBuilder();

        private Listener(CompletableFuture<Void> done) {
            this.done = done;
        }

        private static void request(WebSocket webSocket) {
            try {
                webSocket.request(1);
            } catch (RuntimeException ignored) {
                // The socket may already be closed.
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            QqGateway.this.socket = webSocket;
            request(webSocket);
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket,
                                                             CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String json = buffer.toString();
                buffer.setLength(0);
                try {
                    handlePayload(json);
                } catch (RuntimeException e) {
                    log.warn("QQ gateway payload failed: {}", e.getMessage());
                }
            }
            request(webSocket);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(WebSocket webSocket,
                                                              int statusCode, String reason) {
            log.info("QQ gateway closed: code={}, reason={}", statusCode, reason);
            handleCloseCode(statusCode);
            offline();
            done.complete(null);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.warn("QQ gateway error: {}", error.getMessage());
            offline();
            done.complete(null);
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onPing(WebSocket webSocket,
                                                             java.nio.ByteBuffer message) {
            try {
                webSocket.sendPong(message);
            } catch (RuntimeException ignored) {
                // The socket may already be closed.
            }
            request(webSocket);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onPong(WebSocket webSocket,
                                                             java.nio.ByteBuffer message) {
            request(webSocket);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onBinary(WebSocket webSocket,
                                                               java.nio.ByteBuffer data,
                                                               boolean last) {
            request(webSocket);
            return null;
        }
    }
}
