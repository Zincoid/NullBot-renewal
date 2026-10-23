package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件搜索工具：按业务类型与关键词搜索已存储的文件并返回文件 ID。
 * <p>File search tool: searches stored files by business type and keyword, returning file ids.</p>
 */
@Component
@RequiredArgsConstructor
public class Search implements Tool {

    private static final int DEFAULT_LIMIT = 20;
    private static final Set<String> TYPES = Arrays.stream(BizType.values())
            .map(BizType::getLabel)
            .collect(Collectors.toUnmodifiableSet());

    private final FileService fileService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("search", """
                        Searches stored files of a business type by keyword.
                        Returns file ids; use send with file=<id> to send one.""")
                .addEnum("type", "file business type", TYPES, true)
                .addString("keyword", "keyword matched against tag, optional")
                .addInteger("limit", "max results, default 20, max 50")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        BizType type = BizType.fromLabel(_args.type);
        if (type == null) return ToolRes.error("Unknown type: " + _args.type);
        int limit = _args.limit == null ? DEFAULT_LIMIT : _args.limit;
        List<File> files = fileService.search(type, _args.keyword, null, limit);
        if (files.isEmpty()) return ToolRes.of("No files found");
        StringBuilder text = new StringBuilder("Files: ");
        for (File file : files) {
            text.append("\n- id=").append(file.getId())
                    .append(", name=").append(file.getOriginal())
                    .append(", tag=").append(file.getTag());
        }
        return ToolRes.of(text.toString());
    }

    private record Args(String type, String keyword, Integer limit) {
    }
}
