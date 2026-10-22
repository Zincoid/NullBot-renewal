package com.zincoid.nullbot.bot.cmd.impl.video;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.cmd.CmdArgs;
import com.zincoid.nullbot.bot.cmd.CmdContext;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.bot.exception.CmdWarnException;
import com.zincoid.nullbot.bot.spi.event.GroupMsgEvent;
import com.zincoid.nullbot.module.file.FileException;
import com.zincoid.nullbot.module.file.FileService;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.BizType;
import com.zincoid.nullbot.platform.persistence.po.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 删除视频指令：按文件 ID 删除已收藏的视频。
 * <p>Delete video command: deletes a collected video by file id.</p>
 */
@Slf4j
@CmdMapping({"VideoDel", "删除视频"})
@Component
@RequiredArgsConstructor
public class VideoDelCmd implements Cmd {

    private final FileService fileService;

    // ════ Run ═════════════════════

    @Override
    public void run(CmdContext ctx, GroupMsgEvent event, CmdArgs args) {
        long id = args.nextLong();
        File file = fileService.getById(id);
        if (file == null)
            throw new CmdWarnException("File not found: " + id);
        if (file.getType() != BizType.VID_COLL)
            throw new CmdWarnException("Not a video: " + id);
        try {
            fileService.delete(id);
        } catch (FileException e) {
            throw new CmdWarnException(e.getMessage());
        }
        log.info("Video deleted: {}", id);
        ctx.send("⚠️ Video deleted: " + id);
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.MANAGER;
    }

    @Override
    public String getHelp() {
        return """
                VideoDel
                Deletes a collected video.
                Usage: VideoDel [file id]
                Aliases: 删除视频
                Access: >=1(manager)""";
    }
}
