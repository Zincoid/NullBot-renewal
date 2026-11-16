package com.zincoid.nullbot.adapter.qq;

import com.fasterxml.jackson.databind.JsonNode;
import com.zincoid.nullbot.adapter.qq.api.QqHttpClient;
import com.zincoid.nullbot.adapter.qq.model.QqModels;
import com.zincoid.nullbot.bot.spi.model.MsgContent;
import com.zincoid.nullbot.bot.spi.model.MsgDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * QQ 引用消息解析：优先回查消息索引中的原始详情，缺失时解析事件内联的 msg_elements。
 * <p>Resolves QQ quoted messages from the message index first, falling back to inline msg_elements.</p>
 */
@Component
@RequiredArgsConstructor
public class QqQuoteResolver {

    private static final int QUOTE_MESSAGE_TYPE = 103;
    private static final int MAX_DEPTH = 4;
    private static final String UNKNOWN_USER = "quoted";

    private final QqHttpClient http;
    private final QqMessageIndex index;

    // ════ Resolve ════════════════

    /**
     * 解析一条被引用消息；内联解析的结果会写入索引。
     * <p>Resolves a quoted message; inline results are recorded in the index.</p>
     *
     * @param scene       the message scene
     * @param elements    the inline message elements
     * @param messageType the event message type
     * @param currentId   the current platform message id
     * @param target      the target conversation
     * @param selfId      the bot id
     * @return the quoted message, or null when the event has no quote
     */
    public QqQuote resolve(JsonNode scene, JsonNode elements, Integer messageType,
                           String currentId, QqTarget target, String selfId) {
        String ref = QqScenes.refIndex(scene);
        if (ref == null && (messageType == null || messageType != QUOTE_MESSAGE_TYPE)) return null;
        if (!QqText.isBlank(ref) && ref.equals(QqScenes.selfIndex(scene))) return null;
        MsgDetail cached = index.detailByIndex(ref, target);
        if (cached != null) return QqQuote.of(ref, cached);
        QqQuote quote = parse(elements, ref, currentId, selfId);
        if (quote == null) return null;
        index.rememberQuoted(quote.actualId(), target, quote.detail());
        String userId = quote.detail().userId();
        if (!QqText.isBlank(userId) && !UNKNOWN_USER.equals(userId))
            index.rememberUser(userId, quote.detail().nickname());
        return quote;
    }

    // ════ Parse ══════════════════

    private QqQuote parse(JsonNode elements, String ref, String currentId, String selfId) {
        if (elements == null || !elements.isArray() || elements.isEmpty()) return null;
        JsonNode element = quoteElement(elements, ref);
        if (element == null) return null;
        String elementIdx = element.path("msg_idx").asText(null);
        String actualId = QqText.isBlank(ref)
                ? (QqText.isBlank(elementIdx) ? currentId + ":quote" : elementIdx)
                : ref;
        String text = element.path("content").asText("");
        List<QqModels.Attachment> attachments = attachmentsOf(element.get("attachments"));
        MsgContent content = QqMapper.quotedContent(text, attachments, selfId);
        if (content.segments().isEmpty()) return null;
        QqModels.Author author = http.read(element.get("author"), QqModels.Author.class);
        if (author == null) {
            JsonNode container = quoteContainer(elements, 0);
            if (container != null)
                author = http.read(container.get("author"), QqModels.Author.class);
        }
        String userId = author == null ? "" : QqText.firstNonBlank(
                author.memberOpenid(), QqText.firstNonBlank(author.userOpenid(), author.id()));
        if (QqText.isBlank(userId)) userId = UNKNOWN_USER;
        String nickname = author == null ? userId : QqText.firstNonBlank(author.username(), userId);
        return QqQuote.of(actualId, new MsgDetail(null, userId, nickname, content));
    }

    // ════ Elements ═══════════════

    // 引用的消息可能嵌在 message_type=103 容器里，不能直接取第一个元素。
    // The quoted message may be nested in a message_type=103 container; never take the first element blindly.
    private static JsonNode quoteElement(JsonNode elements, String ref) {
        JsonNode matched = byIndex(elements, ref, 0);
        if (matched != null) return matched;
        JsonNode quote = quoteContainer(elements, 0);
        if (quote == null) return null;
        JsonNode nested = byIndex(quote.get("msg_elements"), ref, 0);
        if (hasContent(nested)) return nested;
        JsonNode content = firstContent(quote.get("msg_elements"), 0);
        return content != null ? content : (hasContent(quote) ? quote : null);
    }

    private static JsonNode byIndex(JsonNode elements, String ref, int depth) {
        if (QqText.isBlank(ref) || elements == null || !elements.isArray() || depth >= MAX_DEPTH)
            return null;
        for (JsonNode element : elements) {
            if (ref.equals(element.path("msg_idx").asText(null))) return element;
            JsonNode nested = byIndex(element.get("msg_elements"), ref, depth + 1);
            if (nested != null) return nested;
        }
        return null;
    }

    private static JsonNode quoteContainer(JsonNode elements, int depth) {
        if (elements == null || !elements.isArray() || depth >= MAX_DEPTH) return null;
        for (JsonNode element : elements) {
            if (element.path("message_type").asInt(0) == QUOTE_MESSAGE_TYPE) return element;
            JsonNode nested = quoteContainer(element.get("msg_elements"), depth + 1);
            if (nested != null) return nested;
        }
        return null;
    }

    private static JsonNode firstContent(JsonNode elements, int depth) {
        if (elements == null || !elements.isArray() || depth >= MAX_DEPTH) return null;
        for (JsonNode element : elements) {
            if (hasContent(element)) return element;
            JsonNode nested = firstContent(element.get("msg_elements"), depth + 1);
            if (nested != null) return nested;
        }
        return null;
    }

    private static boolean hasContent(JsonNode element) {
        if (element == null) return false;
        if (!element.path("content").asText("").isBlank()) return true;
        JsonNode attachments = element.get("attachments");
        return attachments != null && attachments.isArray() && !attachments.isEmpty();
    }

    private List<QqModels.Attachment> attachmentsOf(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<QqModels.Attachment> list = new ArrayList<>();
        for (JsonNode item : node) {
            QqModels.Attachment attachment = http.read(item, QqModels.Attachment.class);
            if (attachment != null) list.add(attachment);
        }
        return list;
    }
}
