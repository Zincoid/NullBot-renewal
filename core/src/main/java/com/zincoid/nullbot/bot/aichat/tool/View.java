package com.zincoid.nullbot.bot.aichat.tool;

import com.zincoid.nullbot.module.ai.tool.Tool;
import com.zincoid.nullbot.module.ai.tool.ToolContext;
import com.zincoid.nullbot.module.ai.tool.ToolDefinition;
import com.zincoid.nullbot.module.ai.tool.ToolRes;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.module.message.AttachmentService;
import com.zincoid.nullbot.module.render.image.ImageData;
import com.zincoid.nullbot.platform.persistence.po.Attachment;
import com.zincoid.nullbot.platform.persistence.po.File;
import com.zincoid.nullbot.platform.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * 图片查看工具：按收藏文件 ID 或聊天附件 ID 读取图片并附给模型。
 * <p>Image tool: reads an image by collection file id or chat attachment id and attaches it to the model.</p>
 */
@Component
@RequiredArgsConstructor
public class View implements Tool {

    private static final long MAX_IMAGE_BYTES = 64L * 1024 * 1024;

    private final FileService fileService;
    private final AttachmentService attachmentService;

    @Override
    public ToolDefinition definition() {
        return ToolDefinition.builder("view", """
                        Attaches a stored image to the conversation so you can see it.
                        Use file for a collection file id from the search tool, or attachment for a chat attachment id from the read tool; provide exactly one.""")
                .addInteger("file", "collection file id from the search tool, optional")
                .addInteger("attachment", "chat attachment id from the read tool, optional")
                .build();
    }

    @Override
    public ToolRes run(ToolContext ctx, String args) {
        Args _args = JsonUtils.parse(args, Args.class);
        boolean hasFile = _args.file != null;
        boolean hasAttachment = _args.attachment != null;
        if (hasFile == hasAttachment)
            return ToolRes.error("Provide exactly one of file or attachment");
        if (hasFile) {
            File file = fileService.getById(_args.file);
            if (file == null)
                return ToolRes.error("File not found: " + _args.file);
            return attach(file.getOriginal(), file.getSize(),
                    () -> fileService.read(file));
        }
        Attachment attachment = attachmentService.getById(_args.attachment);
        if (attachment == null)
            return ToolRes.error("Attachment not found: " + _args.attachment);
        return attach(attachment.getOriginal(), attachment.getSize(),
                () -> attachmentService.read(attachment));
    }

    /**
     * 校验大小并把图片转成 data URI 返回。
     * <p>Checks the size and returns the image as a data URI.</p>
     *
     * @param original original filename
     * @param size     bytes size, nullable
     * @param reader   reads the file bytes lazily, after the size check
     * @return the tool result with the image data URI
     */
    private static ToolRes attach(String original, Long size, Supplier<byte[]> reader) {
        if (size != null && size > MAX_IMAGE_BYTES)
            return ToolRes.error("Image too large");
        String data = ImageData.imageDataUri(reader.get());
        if (data.isBlank())
            return ToolRes.error("Not an image: " + original);
        return ToolRes.of("Image attached: " + original, List.of(data));
    }

    private record Args(Long file, Long attachment) {
    }
}
