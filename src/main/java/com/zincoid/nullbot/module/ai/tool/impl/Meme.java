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

import java.util.List;

/**
 * 表情包查询工具：按关键词搜索可用表情包并返回文件 ID。
 * <p>Meme search tool: searches available memes by keyword and returns file ids.</p>
 */
@Component
@RequiredArgsConstructor
public class Meme implements Tool {

    private static final int DEFAULT_LIMIT = 20;

    private final FileService fileService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("meme", """
                        Searches available memes by keyword.
                        Returns file ids; use send with file=<id> to send one.""")
                .addString("keyword", "keyword matched against description or filename, optional")
                .addInteger("limit", "max results, default 20, max 50")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        int limit = _args.limit == null ? DEFAULT_LIMIT : _args.limit;
        List<File> memes = fileService.search(BizType.MEME, _args.keyword, limit);
        if (memes.isEmpty()) return ToolRes.of("No memes found");
        StringBuilder text = new StringBuilder("Memes:");
        for (File meme : memes) {
            text.append("\n- file id=").append(meme.getId())
                    .append(" ,name=").append(meme.getOriginal())
                    .append(": ").append(meme.getDescription());
        }
        return ToolRes.of(text.toString());
    }

    private record Args(String keyword, Integer limit) {
    }
}
