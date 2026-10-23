package com.zincoid.query.aa;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.cmd.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.bot.spi.event.PrivateMsgEvent;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.query.QueryException;
import com.zincoid.query.QueryPlugin;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;

/**
 * AA 指令：展示 Artificial Analysis 模型排行。
 * <p>AA command: shows Artificial Analysis model rankings.</p>
 */
@Slf4j
@CmdMapping({"AA", "模型查询"})
public class AaCmd implements Cmd {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 25;

    private final QueryPlugin plugin;

    public AaCmd(QueryPlugin plugin) {
        this.plugin = plugin;
    }

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    @Override
    public void run(CmdContext ctx, PrivateMsgEvent event, CmdArgs args) {
        handle(ctx, args);
    }

    private void handle(CmdContext ctx, CmdArgs args) {
        String option = args.next("智能");
        AaMetric metric = AaMetric.from(option);
        if (metric == null)
            throw new CmdWarnException(text(ctx,
                    "未知查询项：%s（可选：智能/历史/性价比/速度/编程/成本/定价）",
                    "Unknown metric: %s (expected Intelligence/History/Value/Speed/Coding/Cost/Pricing)")
                    .formatted(option));
        int limit = limit(ctx, args);
        if (plugin.config().aaApiKey().isBlank())
            throw new CmdWarnException(text(ctx,
                    "未配置 AA API Key", "AA API key is not configured"));
        List<AaModel> models;
        try {
            models = plugin.aa().top(metric, limit);
        } catch (QueryException e) {
            log.warn("AA query failed", e);
            throw new CmdWarnException(text(ctx,
                    "AA 查询失败，请检查 API Key 或网络",
                    "AA query failed; check the API key or network"));
        }
        if (models.isEmpty()) {
            ctx.send(text(ctx, "没有可用数据", "No data available"));
            return;
        }
        ctx.send(format(ctx, metric, models));
        log.info("AA sent: metric={}, rows={}", metric.name(), models.size());
    }

    // ════ Format ══════════════════

    private static String format(CmdContext ctx, AaMetric metric, List<AaModel> models) {
        StringBuilder text = new StringBuilder()
                .append("AA ").append(metric.label(ctx.language()))
                .append(" Top ").append(models.size()).append('\n');
        int index = 1;
        for (AaModel model : models) {
            text.append(index++).append(". ").append(model.name());
            if (!model.creator().isBlank()) text.append(" (").append(model.creator()).append(')');
            text.append(" — ").append(value(ctx, metric, model)).append('\n');
        }
        return text.toString().stripTrailing();
    }

    private static String value(CmdContext ctx, AaMetric metric, AaModel model) {
        boolean en = ctx.language() == Language.EN;
        return switch (metric) {
            case INTELLIGENCE -> (en ? "Intelligence " : "智能 ") + number(model.intelligence());
            case HISTORY -> model.releaseDate() + " · "
                    + (en ? "Intelligence " : "智能 ") + number(model.intelligence());
            case VALUE -> (en ? "Intelligence " : "智能 ") + number(model.intelligence())
                    + " · " + (en ? "Cost " : "成本 ") + moneyCost(model.costPerTask());
            case SPEED -> number(model.outputSpeed()) + " tok/s";
            case CODING -> (en ? "Coding " : "编程 ") + number(model.coding());
            case COST -> moneyCost(model.costPerTask()) + (en ? "/task" : "/任务");
            case PRICING -> (en ? "Input " : "输入 ") + money(model.inputPrice())
                    + " · " + (en ? "Output " : "输出 ") + money(model.outputPrice());
        };
    }

    private static String number(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "%.1f", value);
    }

    private static String money(Double value) {
        return value == null ? "-" : String.format(Locale.ROOT, "$%.2f", value);
    }

    private static String moneyCost(Double value) {
        if (value == null) return "-";
        if (Math.abs(value) < 0.001) return String.format(Locale.ROOT, "$%.6f", value);
        if (Math.abs(value) < 1) return String.format(Locale.ROOT, "$%.4f", value);
        return String.format(Locale.ROOT, "$%.2f", value);
    }

    // ════ Helpers ═════════════════

    private static int limit(CmdContext ctx, CmdArgs args) {
        if (!args.hasNext()) return DEFAULT_LIMIT;
        String raw = args.next();
        try {
            return Math.clamp(Integer.parseInt(raw), 1, MAX_LIMIT);
        } catch (NumberFormatException e) {
            throw new CmdWarnException(text(ctx,
                    "数量格式错误：%s", "Malformed count: %s").formatted(raw));
        }
    }

    private static String text(CmdContext ctx, String zh, String en) {
        return ctx.language() == Language.EN ? en : zh;
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return language == Language.EN ? """
                AA
                Shows Artificial Analysis model rankings.
                Usage: AA [metric] [count]
                Metrics: 智能/历史/性价比/速度/编程/成本/定价
                Count: 1..25, default 10
                Aliases: 模型查询
                Access: >=0(normal)
                Note: requires AA_API_KEY or aa.api-key in the query plugin config.""" : """
                AA
                查看 Artificial Analysis 模型排行。
                用法：AA [查询项] [数量]
                查询项：智能/历史/性价比/速度/编程/成本/定价
                数量：1~25，默认 10
                别名：模型查询
                权限：>=0(normal)
                说明：需要在 query 插件配置 AA_API_KEY 或 aa.api-key。""";
    }
}
