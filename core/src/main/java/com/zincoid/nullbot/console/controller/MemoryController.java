package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.bot.BotClients;
import com.zincoid.nullbot.bot.BotException;
import com.zincoid.nullbot.bot.BotHolder;
import com.zincoid.nullbot.bot.spi.port.BotClient;
import com.zincoid.nullbot.console.dto.MemoryChatView;
import com.zincoid.nullbot.console.dto.MemoryDetailView;
import com.zincoid.nullbot.console.dto.MemoryView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.GroupService;
import com.zincoid.nullbot.module.common.UserService;
import com.zincoid.nullbot.module.memory.MemoryService;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.po.Group;
import com.zincoid.nullbot.platform.persistence.po.Memory;
import com.zincoid.nullbot.platform.persistence.po.User;
import com.zincoid.nullbot.platform.util.ChatIds;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台记忆接口：按会话浏览 AI 情景摘要与长期事实，并支持删除。
 * <p>Console memory API: browses AI episodic summaries and long-term facts by chat, and supports deletion.</p>
 */
@RestController
@RequestMapping("/console/memories")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;
    private final GroupService groupService;
    private final UserService userService;
    private final BotHolder botHolder;

    // ════ Chats ═══════════════════

    /**
     * 列出有记忆的会话，按最近记忆创建时间倒序。
     * <p>Lists chats that have memories, newest memory creation first.</p>
     *
     * @return the memory chat views
     */
    @GetMapping("/chats")
    public List<MemoryChatView> chats() {
        List<MemoryService.ChatSummary> summaries = memoryService.chats();
        if (summaries.isEmpty()) return List.of();

        List<String> groupIds = rawIds(summaries, ChatType.GROUP);
        List<String> userIds = rawIds(summaries, ChatType.PRIVATE);

        Map<String, String> names = new HashMap<>();
        if (!groupIds.isEmpty()) {
            for (Group group : groupService.listByIds(groupIds))
                names.put(ChatIds.ofGroup(group.getId()), group.getName());
        }
        if (!userIds.isEmpty()) {
            for (User user : userService.listByIds(userIds))
                names.put(ChatIds.ofPrivate(user.getId()), user.getName());
        }

        BotClient client = clientOrNull();
        return summaries.stream()
                .map(summary -> {
                    ChatType type = ChatIds.type(summary.chatId());
                    String raw = ChatIds.rawId(summary.chatId());
                    String name = names.get(summary.chatId());
                    if (missing(name, raw)) name = liveName(client, type, raw);
                    if (name == null || name.isBlank()) name = raw;
                    if (name == null || name.isBlank()) name = summary.chatId();
                    return new MemoryChatView(
                            summary.chatId(),
                            name,
                            type == null ? "unknown" : type.getLabel(),
                            summary.episodic(),
                            summary.semantic(),
                            summary.lastAt());
                })
                .toList();
    }

    // ════ Memories ════════════════

    /**
     * 返回指定会话的情景摘要与长期事实。
     * <p>Returns the episodic summary and long-term facts of the given chat.</p>
     *
     * @param chatId composite chat id
     * @return the memory detail
     */
    @GetMapping
    public MemoryDetailView detail(@RequestParam(required = false) String chatId) {
        String id = ConsoleValidation.required(chatId, 64, "chatId");
        Memory episodic = memoryService.getEpisodic(id);
        List<Memory> semantic = memoryService.listSemantic(id);
        return new MemoryDetailView(
                id,
                episodic == null ? null : MemoryView.of(episodic),
                semantic.stream().map(MemoryView::of).toList());
    }

    /**
     * 清空指定会话的情景摘要。
     * <p>Clears the episodic summary of the given chat.</p>
     *
     * @param chatId composite chat id
     * @return an empty response
     */
    @DeleteMapping("/episodic")
    public ResponseEntity<Void> clearEpisodic(@RequestParam(required = false) String chatId) {
        memoryService.clearEpisodic(ConsoleValidation.required(chatId, 64, "chatId"));
        return ResponseEntity.noContent().build();
    }

    /**
     * 清空指定会话的全部长期事实。
     * <p>Clears every long-term fact of the given chat.</p>
     *
     * @param chatId composite chat id
     * @return an empty response
     */
    @DeleteMapping("/semantic")
    public ResponseEntity<Void> clearSemantic(@RequestParam(required = false) String chatId) {
        memoryService.clearSemantic(ConsoleValidation.required(chatId, 64, "chatId"));
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除指定会话的一条长期事实。
     * <p>Deletes one long-term fact of the given chat.</p>
     *
     * @param id     the memory id
     * @param chatId composite chat id
     * @return an empty response
     */
    @DeleteMapping("/semantic/{id}")
    public ResponseEntity<Void> deleteSemantic(@PathVariable long id,
                                               @RequestParam(required = false) String chatId) {
        String chat = ConsoleValidation.required(chatId, 64, "chatId");
        if (!memoryService.deleteSemantic(chat, id))
            throw new ConsoleException(HttpStatus.NOT_FOUND, "Memory not found: " + id);
        return ResponseEntity.noContent().build();
    }

    // ════ Helpers ═════════════════

    /**
     * 提取指定类型的会话原始 ID。
     * <p>Extracts raw ids of the given chat type.</p>
     *
     * @param summaries memory chat summaries
     * @param type      chat type
     * @return distinct non-blank raw ids
     */
    private static List<String> rawIds(List<MemoryService.ChatSummary> summaries, ChatType type) {
        return summaries.stream()
                .filter(summary -> ChatIds.type(summary.chatId()) == type)
                .map(summary -> ChatIds.rawId(summary.chatId()))
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
    }

    /**
     * 返回当前在线机器人客户端，离线时返回 null。
     * <p>Returns the online bot client, or null when the bot is offline.</p>
     *
     * @return the online client, or null
     */
    private BotClient clientOrNull() {
        try {
            return botHolder.getClient();
        } catch (BotException e) {
            return null;
        }
    }

    /**
     * 名称为空或只是原始 ID 时，尝试从平台查询显示名称，不写数据库。
     * <p>Looks up a display name from the platform when the stored name is missing or only the raw id, without writing it back.</p>
     *
     * @param client the online bot client, nullable
     * @param type   the chat type, nullable
     * @param rawId  the raw platform id, nullable
     * @return the resolved name, or null when unavailable
     */
    private static String liveName(BotClient client, ChatType type, String rawId) {
        if (client == null || type == null || rawId == null || rawId.isBlank()) return null;
        String name = type == ChatType.GROUP
                ? BotClients.groupName(client, rawId)
                : BotClients.userName(client, rawId);
        return missing(name, rawId) ? null : name;
    }

    /**
     * 判断名称是否缺失或只是原始 ID。
     * <p>Whether the name is missing or only the raw id.</p>
     *
     * @param name  the candidate name, nullable
     * @param rawId the raw platform id, nullable
     * @return true when the name is missing or equal to the raw id
     */
    private static boolean missing(String name, String rawId) {
        return name == null || name.isBlank() || name.equals(rawId);
    }
}
