package com.zincoid.nullbot.bot.cmd.input;

import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 分页选择器：发送分页内容并等待用户输入翻页、选择或结束。
 * <p>Page selector: sends pages and waits for user input to navigate, select, or end.</p>
 *
 * @param <T> the item type
 */
public final class PageSelector<T> {

    private static final String INPUT_PATTERN = "[1-9]\\d*|(?i:up|down|end)";
    private static final long DEFAULT_TIMEOUT = 30;
    private static final int DEFAULT_SIZE = 10;

    private final InputManager inputManager;
    private final String userId;
    private final String title;
    private final List<T> values;
    private final Function<? super T, String> renderer;
    private final Consumer<? super T> action;
    private final Language language;
    private final Consumer<String> sender;
    private final int total;
    private final int size;
    private final int pages;
    private final long timeout;
    private final boolean continuous;
    private final boolean coverable;

    private int current;

    private PageSelector(Builder<T> builder, int total, int pages, int current) {
        this.inputManager = builder.inputManager;
        this.userId = builder.userId;
        this.title = builder.title;
        this.values = builder.values;
        this.renderer = builder.renderer;
        this.action = builder.action;
        this.language = builder.language == null ? Language.ZH : builder.language;
        this.sender = builder.sender;
        this.total = total;
        this.size = builder.size;
        this.pages = pages;
        this.timeout = builder.timeout;
        this.continuous = builder.continuous;
        this.coverable = builder.coverable;
        this.current = current;
    }

    /**
     * 创建分页选择器构建器。
     * <p>Creates a page selector builder.</p>
     *
     * @param title    the page title
     * @param values   the values to page through
     * @param renderer the list item renderer
     * @param <T>      the item type
     * @return the builder
     */
    public static <T> Builder<T> builder(String title, List<T> values,
                                         Function<? super T, String> renderer) {
        return new Builder<>(title, values, renderer);
    }

    /**
     * 发送第一页并开始等待用户操作，直到结束或超时。
     * <p>Sends the first page and waits for user actions until ended or timed out.</p>
     */
    public void start() {
        if (!coverable && inputManager.isWaiting(InputMode.PS, userId))
            throw new InputException("Input wait already active: " + userId);
        sendPage();
        while (true) {
            List<InputResult> inputs = inputManager.request(
                    InputMode.PS, userId, INPUT_PATTERN, timeout, coverable);
            if (inputs.isEmpty()) {
                sender.accept(Text.PAGE_TIMEOUT.of(language));
                return;
            }
            String input = inputs.getFirst().message().trim();
            if (input.equalsIgnoreCase("end")) {
                sender.accept(Text.PAGE_END.of(language));
                return;
            }
            if (input.equalsIgnoreCase("up")) {
                if (current > 1) {
                    current--;
                    sendPage();
                } else {
                    sender.accept(Text.PAGE_TOP.of(language));
                }
                continue;
            }
            if (input.equalsIgnoreCase("down")) {
                if (current < pages) {
                    current++;
                    sendPage();
                } else {
                    sender.accept(Text.PAGE_BOTTOM.of(language));
                }
                continue;
            }
            int index;
            try {
                index = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                sender.accept(Text.PAGE_INVALID.of(language, input));
                continue;
            }
            if (index > total) {
                sender.accept(Text.PAGE_INVALID.of(language, input));
                continue;
            }
            if (action != null) action.accept(values.get(index - 1));
            if (!continuous) {
                sender.accept(Text.PAGE_END.of(language));
                return;
            }
        }
    }

    // ════ Helpers ═════════════════

    private void sendPage() {
        int from = (current - 1) * size;
        int to = Math.min(from + size, total);
        StringBuilder body = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (i > from) body.append('\n');
            body.append(i + 1).append(". ").append(renderer.apply(values.get(i)));
        }
        sender.accept(Text.PAGE_HEADER.of(language, title, total)
                + "\n" + body
                + "\n\n" + Text.PAGE_FOOTER.of(language, current, pages, size));
    }

    /**
     * 分页选择器构建器。
     * <p>Builder of a page selector.</p>
     *
     * @param <T> the item type
     */
    public static final class Builder<T> {

        private final String title;
        private final List<T> values;
        private final Function<? super T, String> renderer;

        private InputManager inputManager;
        private String userId;
        private Consumer<? super T> action;
        private Language language = Language.ZH;
        private Consumer<String> sender;
        private int size = DEFAULT_SIZE;
        private int initialPage = Integer.MAX_VALUE;
        private long timeout = DEFAULT_TIMEOUT;
        private boolean continuous;
        private boolean coverable;

        private Builder(String title, List<T> values, Function<? super T, String> renderer) {
            this.title = title;
            this.values = values;
            this.renderer = renderer;
        }

        public Builder<T> inputManager(InputManager inputManager) {
            this.inputManager = inputManager;
            return this;
        }

        public Builder<T> userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder<T> action(Consumer<? super T> action) {
            this.action = action;
            return this;
        }

        public Builder<T> language(Language language) {
            this.language = language;
            return this;
        }

        public Builder<T> sender(Consumer<String> sender) {
            this.sender = sender;
            return this;
        }

        public Builder<T> size(int size) {
            this.size = size;
            return this;
        }

        public Builder<T> initialPage(int initialPage) {
            this.initialPage = initialPage;
            return this;
        }

        public Builder<T> timeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder<T> continuous(boolean continuous) {
            this.continuous = continuous;
            return this;
        }

        public Builder<T> coverable(boolean coverable) {
            this.coverable = coverable;
            return this;
        }

        /**
         * 构建分页选择器。
         * <p>Builds the page selector.</p>
         *
         * @return the page selector
         */
        public PageSelector<T> build() {
            if (title == null) throw new InputException("Missing key: title");
            if (values == null || values.isEmpty()) throw new InputException("Missing key: values");
            if (renderer == null) throw new InputException("Missing key: renderer");
            if (inputManager == null) throw new InputException("Missing key: inputManager");
            if (userId == null || userId.isBlank()) throw new InputException("Missing key: userId");
            if (sender == null) throw new InputException("Missing key: sender");
            if (size <= 0) throw new InputException("Malformed size: " + size);
            if (timeout <= 0) throw new InputException("Malformed timeout: " + timeout);
            int total = values.size();
            int pages = (total + size - 1) / size;
            int current = Math.clamp(initialPage, 1, pages);
            return new PageSelector<>(this, total, pages, current);
        }
    }
}
