package com.zincoid.nullbot.module.ai.memory;

import com.zincoid.nullbot.module.ai.context.ContextPacket;
import com.zincoid.nullbot.module.ai.context.TextPacket;
import com.zincoid.nullbot.module.memory.MemoryService;
import com.zincoid.nullbot.platform.persistence.po.Memory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 长期事实语义记忆：事实直接委托记忆服务，并格式化为上下文信息包。
 * <p>Long-term fact semantic memory: delegates facts to the memory service and formats them as context packets.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LongTermFactMemory implements SemanticMemory {

    private static final int CONTEXT_LIMIT = 16;

    private final MemoryService memoryService;

    // ════ Context ═════════════════

    @Override
    public List<ContextPacket> context(String chatId) {
        try {
            List<Memory> memories = memoryService.latestSemantic(chatId, CONTEXT_LIMIT);
            if (memories.isEmpty()) return List.of();
            StringBuilder text = new StringBuilder("[Semantic memory]");
            for (Memory memory : memories)
                text.append("\n- id=").append(memory.getId())
                        .append(": ").append(memory.getContent());
            return List.of(new TextPacket(text.toString()));
        } catch (RuntimeException e) {
            log.warn("Failed to load long-term memory: {}", e.getMessage());
            return List.of();
        }
    }

    // ════ Search ══════════════════

    @Override
    public List<Memory> search(String chatId, String keyword, int limit) {
        if (keyword == null || keyword.isBlank())
            return memoryService.latestSemantic(chatId, limit);
        return memoryService.searchSemantic(chatId, keyword, limit);
    }

    // ════ Mutate ══════════════════

    @Override
    public Memory add(String chatId, String content) {
        return memoryService.addSemantic(chatId, content);
    }

    @Override
    public boolean delete(String chatId, long id) {
        return memoryService.deleteSemantic(chatId, id);
    }
}
