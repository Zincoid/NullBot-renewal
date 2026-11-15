package com.zincoid.nullbot.module.memory;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.module.setting.UserSettingService;
import com.zincoid.nullbot.platform.persistence.enums.ChatType;
import com.zincoid.nullbot.platform.persistence.enums.MemoryType;
import com.zincoid.nullbot.platform.persistence.mapper.MemoryMapper;
import com.zincoid.nullbot.platform.persistence.po.GroupSetting;
import com.zincoid.nullbot.platform.persistence.po.Memory;
import com.zincoid.nullbot.platform.persistence.po.UserSetting;
import com.zincoid.nullbot.platform.util.ChatIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@link MemoryService} 的默认实现。
 * <p>Default implementation of {@link MemoryService}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl extends ServiceImpl<MemoryMapper, Memory> implements MemoryService {

    private static final int MAX_SEMANTIC_CONTENT = 512;
    private static final int MAX_SEMANTIC_LIMIT = 64;
    private static final int CONSOLE_SEMANTIC_LIMIT = 500;

    private final GroupSettingService groupSettingService;
    private final UserSettingService userSettingService;

    // ════ Policy ═════════════════

    @Override
    public boolean isEpisodicEnabled(String chatId) {
        return flags(chatId).episodic();
    }

    @Override
    public boolean isSemanticEnabled(String chatId) {
        return flags(chatId).semantic();
    }

    // ════ Episodic ════════════════

    @Override
    public Memory getEpisodic(String chatId) {
        requireChatId(chatId);
        return lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.EPISODIC)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public void putEpisodic(String chatId, String content) {
        requireChatId(chatId);
        if (content == null || content.isBlank())
            throw new MemoryException("Missing content");
        LocalDateTime now = LocalDateTime.now();
        Memory existing = lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.EPISODIC)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            existing.setContent(content);
            existing.setUpdatedAt(now);
            if (!updateById(existing))
                throw new MemoryException("Failed to update episodic memory");
            log.info("Episodic memory updated: chat={}, id={}", chatId, existing.getId());
            return;
        }
        Memory memory = Memory.builder()
                .chatId(chatId)
                .type(MemoryType.EPISODIC)
                .content(content)
                .createdAt(now)
                .updatedAt(now)
                .build();
        if (!save(memory))
            throw new MemoryException("Failed to save episodic memory");
        log.info("Episodic memory saved: chat={}, id={}", chatId, memory.getId());
    }

    @Override
    public void clearEpisodic(String chatId) {
        clearByType(chatId, MemoryType.EPISODIC);
    }

    // ════ Semantic ════════════════

    @Override
    public List<Memory> latestSemantic(String chatId, int limit) {
        requireChatId(chatId);
        int size = Math.clamp(limit, 1, MAX_SEMANTIC_LIMIT);
        return lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.SEMANTIC)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT " + size)
                .list();
    }

    @Override
    public List<Memory> listSemantic(String chatId) {
        requireChatId(chatId);
        return lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.SEMANTIC)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT " + CONSOLE_SEMANTIC_LIMIT)
                .list();
    }

    @Override
    public List<Memory> searchSemantic(String chatId, String keyword, int limit) {
        requireChatId(chatId);
        if (keyword == null || keyword.isBlank())
            return latestSemantic(chatId, limit);
        int size = Math.clamp(limit, 1, MAX_SEMANTIC_LIMIT);
        return lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.SEMANTIC)
                .like(Memory::getContent, keyword)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT " + size)
                .list();
    }

    @Override
    public Memory addSemantic(String chatId, String content) {
        requireChatId(chatId);
        if (content == null || content.isBlank())
            throw new MemoryException("Missing content");
        String normalized = content.strip().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_SEMANTIC_CONTENT)
            throw new MemoryException("Content too long: " + normalized.length());
        Memory existing = lambdaQuery()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.SEMANTIC)
                .eq(Memory::getContent, normalized)
                .last("LIMIT 1")
                .one();
        if (existing != null) return existing;
        LocalDateTime now = LocalDateTime.now();
        Memory memory = Memory.builder()
                .chatId(chatId)
                .type(MemoryType.SEMANTIC)
                .content(normalized)
                .createdAt(now)
                .updatedAt(now)
                .build();
        if (!save(memory))
            throw new MemoryException("Failed to save memory");
        log.info("Memory saved: chat={}, id={}", chatId, memory.getId());
        return memory;
    }

    @Override
    public boolean deleteSemantic(String chatId, long id) {
        requireChatId(chatId);
        boolean removed = lambdaUpdate()
                .eq(Memory::getId, id)
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, MemoryType.SEMANTIC)
                .remove();
        if (removed)
            log.info("Memory deleted: chat={}, id={}", chatId, id);
        return removed;
    }

    @Override
    public void clearSemantic(String chatId) {
        clearByType(chatId, MemoryType.SEMANTIC);
    }

    // ════ Chats ═══════════════════

    @Override
    public List<ChatSummary> chats() {
        return baseMapper.chats().stream()
                .map(row -> new ChatSummary(
                        row.getChatId(),
                        count(row.getEpisodic()),
                        count(row.getSemantic()),
                        row.getLastAt()))
                .toList();
    }

    // ════ Helpers ═════════════════

    private Flags flags(String chatId) {
        ChatType type = ChatIds.type(chatId);
        if (type == null) return Flags.DISABLED;
        String targetId = ChatIds.rawId(chatId);
        if (targetId == null || targetId.isBlank()) return Flags.DISABLED;
        try {
            return switch (type) {
                case GROUP -> {
                    GroupSetting setting = groupSettingService.get(targetId);
                    yield new Flags(setting.getAiEpisodicMemoryEnabled(),
                            setting.getAiSemanticMemoryEnabled());
                }
                case PRIVATE -> {
                    UserSetting setting = userSettingService.get(targetId);
                    yield new Flags(setting.getPrivateAiEpisodicMemoryEnabled(),
                            setting.getPrivateAiSemanticMemoryEnabled());
                }
            };
        } catch (RuntimeException e) {
            log.warn("Failed to read memory settings: chat={}, {}", chatId, e.getMessage());
            return Flags.DISABLED;
        }
    }

    private record Flags(boolean episodic, boolean semantic) {

        private static final Flags DISABLED = new Flags(false, false);
    }

    private static long count(Long value) {
        return value == null ? 0 : value;
    }

    private void clearByType(String chatId, MemoryType type) {
        requireChatId(chatId);
        boolean removed = lambdaUpdate()
                .eq(Memory::getChatId, chatId)
                .eq(Memory::getType, type)
                .remove();
        if (removed)
            log.info("AI memory cleared: chat={}, type={}", chatId, type);
    }

    private static void requireChatId(String chatId) {
        if (chatId == null || chatId.isBlank())
            throw new MemoryException("Missing key: chatId");
    }
}
