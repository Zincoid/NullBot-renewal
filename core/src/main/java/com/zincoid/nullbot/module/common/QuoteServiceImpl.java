package com.zincoid.nullbot.module.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.QuoteMapper;
import com.zincoid.nullbot.platform.persistence.po.Quote;
import com.zincoid.nullbot.platform.util.PageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * {@link QuoteService} 的默认实现。
 * <p>Default implementation of {@link QuoteService}.</p>
 */
@Slf4j
@Service
public class QuoteServiceImpl extends ServiceImpl<QuoteMapper, Quote> implements QuoteService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern PATTERN = Pattern.compile(
            "^\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}]\\[No\\.\\d+][\\s\\S]*");

    // ════ Add ═════════════════════

    @Override
    public Quote add(String userId, String nickname, String text) {
        if (userId == null || userId.isBlank()) throw new CommonException("Missing key: userId");
        if (nickname == null) throw new CommonException("Missing key: nickname");
        if (text == null || text.isBlank()) throw new CommonException("Missing key: text");
        Quote quote = Quote.builder()
                .time(LocalDateTime.now())
                .userId(userId)
                .nickname(nickname)
                .text(text)
                .build();
        save(quote);
        log.info("Quote added: {}", quote.getId());
        return quote;
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(Long id) {
        if (id == null) throw new CommonException("Missing key: id");
        if (!removeById(id)) throw new CommonException("Quote not found: " + id);
        log.info("Quote deleted: {}", id);
    }

    // ════ Random ══════════════════

    @Override
    public Quote random() {
        long count = count();
        if (count == 0) return null;
        long offset = ThreadLocalRandom.current().nextLong(count);
        return lambdaQuery().last("LIMIT 1 OFFSET " + offset).one();
    }

    @Override
    public Quote random(String userId) {
        if (userId == null || userId.isBlank()) throw new CommonException("Missing key: userId");
        long count = lambdaQuery().eq(Quote::getUserId, userId).count();
        if (count == 0) return null;
        long offset = ThreadLocalRandom.current().nextLong(count);
        return lambdaQuery()
                .eq(Quote::getUserId, userId)
                .last("LIMIT 1 OFFSET " + offset)
                .one();
    }

    // ════ Page ════════════════════

    // ════ Format ══════════════════

    @Override
    public String format(Quote quote) {
        String time = quote.getTime() == null ? "" : quote.getTime().format(FORMATTER);
        return """
                [%s][No.%d]
                %s
                \t—— %s(%s)""".formatted(
                time, quote.getId(), quote.getText(), quote.getNickname(), quote.getUserId());
    }

    @Override
    public boolean isFormatted(String text) {
        return PATTERN.matcher(text).matches();
    }

    // ════ Page ════════════════════

    @Override
    public Page<Quote> page(String keyword, long page, long size) {
        String key = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return lambdaQuery()
                .and(key != null, q -> q.like(Quote::getText, key)
                        .or().like(Quote::getNickname, key)
                        .or().like(Quote::getUserId, key))
                .orderByDesc(Quote::getTime)
                .orderByDesc(Quote::getId)
                .page(PageUtils.of(page, size));
    }
}
