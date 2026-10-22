package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.PageView;
import com.zincoid.nullbot.console.dto.QuoteView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import com.zincoid.nullbot.module.common.QuoteService;
import com.zincoid.nullbot.platform.persistence.po.Quote;
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

/**
 * 控制台语录接口：分页查询与删除。
 * <p>Console quote API: paged queries and deletion.</p>
 */
@RestController
@RequestMapping("/console/quotes")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteService quoteService;

    // ════ Query ══════════════════

    /**
     * 分页查询语录。
     * <p>Pages quotes.</p>
     *
     * @param keyword keyword matched against text, nickname, or user id
     * @param page    one-based page number
     * @param size    page size
     * @return the quote page
     */
    @GetMapping
    public PageView<QuoteView> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return PageView.of(quoteService.page(keyword,
                ConsoleValidation.page(page), ConsoleValidation.size(size)), QuoteView::of);
    }

    // ════ Delete ═════════════════

    /**
     * 删除语录。
     * <p>Deletes a quote.</p>
     *
     * @param id quote id
     * @return an empty response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Quote quote = quoteService.getById(id);
        if (quote == null) throw new ConsoleException(HttpStatus.NOT_FOUND, "Quote not found: " + id);
        quoteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
