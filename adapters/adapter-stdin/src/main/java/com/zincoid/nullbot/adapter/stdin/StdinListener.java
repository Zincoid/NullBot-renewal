package com.zincoid.nullbot.adapter.stdin;

import com.zincoid.nullbot.bot.spi.event.*;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.port.BotServer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Stdin 入站适配器：归一化终端输入为 SPI 事件并交给 {@link BotServer}。
 * <p>Stdin inbound adapter: normalizes terminal input into SPI events and hands them to {@link BotServer}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StdinListener {

    private final BotServer server;
    private final StdinClient client = new StdinClient();

    // ════ Input ═══════════════════

    @PostConstruct
    void start() {
        Thread.ofVirtual().name("stdin-input").start(this::loop);
        log.info("Stdin adapter ready: selfId={} userId={} groupId={}",
                StdinClient.SELF_ID, StdinClient.USER_ID, StdinClient.GROUP_ID);
        log.info("""
                Cmd List:
                1. msg g|p <text>
                2. poke g|p
                3. recall g|p|ga <id>
                4. join
                5. leave [kick]""");
    }

    private void loop() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in)
        )) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                BotEvent event = parse(line.strip());
                if (event != null) server.handle(client, event);
            }
        } catch (IOException e) {
            log.warn("Input closed: {}", e.getMessage());
        }
    }

    private BotEvent parse(String line) {
        String[] parts = line.split("\\s+", 3);
        String cmd = parts[0];
        String arg1 = parts.length > 1 ? parts[1] : "";
        String arg2 = parts.length > 2 ? parts[2] : "";
        return switch (cmd) {
            case "msg" -> msg(arg1, arg2);
            case "poke" -> poke(arg1);
            case "recall" -> recall(arg1, arg2);
            case "join" -> join();
            case "leave" -> leave(arg1);
            default -> {
                log.warn("Unknown operation: {}", cmd);
                yield null;
            }
        };
    }

    // ════ Events ══════════════════

    private BotEvent msg(String scope, String text) {
        if (text.isEmpty()) return null;
        MsgContent content = StdinMapper.fromStdin(text);
        return switch (scope) {
            case "g" -> new GroupMsgEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    StdinClient.GROUP_ID,
                    client.record(StdinClient.USER_ID, content),
                    "tester",
                    content,
                    false,
                    false
            );
            case "p" -> new PrivateMsgEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    client.record(StdinClient.USER_ID, content),
                    "tester",
                    content,
                    false,
                    false
            );
            default -> {
                log.warn("Unknown scope: {}", scope);
                yield null;
            }
        };
    }

    private BotEvent poke(String scope) {
        return switch (scope) {
            case "g" -> new GroupPokeEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    StdinClient.SELF_ID,
                    StdinClient.GROUP_ID
            );
            case "p" -> new PrivatePokeEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    StdinClient.SELF_ID
            );
            default -> {
                log.warn("Unknown scope: {}", scope);
                yield null;
            }
        };
    }

    private BotEvent recall(String scope, String id) {
        if (id.isBlank()) return null;
        return switch (scope) {
            case "g", "ga" -> new GroupRecallEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    id,
                    StdinClient.GROUP_ID,
                    "ga".equals(scope)
                            ? StdinClient.ADMIN_ID
                            : StdinClient.USER_ID
            );
            case "p" -> new PrivateRecallEvent(
                    StdinClient.SELF_ID,
                    StdinClient.USER_ID,
                    id
            );
            default -> {
                log.warn("Unknown scope: {}", scope);
                yield null;
            }
        };
    }

    private JoinEvent join() {
        return new JoinEvent(
                StdinClient.SELF_ID,
                StdinClient.GROUP_ID,
                StdinClient.USER_ID,
                StdinClient.ADMIN_ID
        );
    }

    private LeaveEvent leave(String kick) {
        return new LeaveEvent(
                StdinClient.SELF_ID,
                StdinClient.GROUP_ID,
                StdinClient.USER_ID,
                "kick".equals(kick)
                        ? StdinClient.ADMIN_ID
                        : StdinClient.USER_ID,
                "kick".equals(kick)
        );
    }
}
