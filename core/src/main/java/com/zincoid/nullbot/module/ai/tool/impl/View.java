package com.zincoid.nullbot.module.ai.tool.impl;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.JsonUtils;
import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * 图片查看工具：按文件 ID 读取图片并附给模型。
 * <p>Image tool: reads a stored image by file id and attaches it to the model.</p>
 */
@Component
@RequiredArgsConstructor
public class View implements Tool {

    private final FileService fileService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("view", """
                        Attaches a stored image to the conversation so you can see it.""")
                .addString("id", "file id from the read tool", true)
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        File file = fileService.getById(_args.id);
        if (file == null)
            return ToolRes.error("File not found: " + _args.id);
        byte[] bytes = fileService.read(file);
        String mime = mime(file, bytes);
        if (mime == null || !mime.startsWith("image/"))
            return ToolRes.error("Not an image: " + file.getOriginal());
        String data = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        return ToolRes.of("Image attached: " + file.getOriginal(), List.of(data));
    }

    /**
     * 依次按字节、存储名与原始名推断 MIME 类型。
     * <p>Infers the MIME type from the bytes, the stored name, then the original name.</p>
     *
     * @param file  the file record
     * @param bytes the file bytes
     * @return the MIME type, or null when unknown
     */
    private static String mime(File file, byte[] bytes) {
        String mime = MimeUtils.mime(bytes);
        if (mime == null) mime = MimeUtils.mime(file.getStored());
        if (mime == null) mime = MimeUtils.mime(file.getOriginal());
        return mime;
    }

    private record Args(Long id) {
    }
}
