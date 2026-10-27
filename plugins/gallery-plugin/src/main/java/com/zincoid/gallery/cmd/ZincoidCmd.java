package com.zincoid.gallery.cmd;

import com.zincoid.gallery.GalleryPlugin;
import com.zincoid.gallery.GalleryText;
import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.platform.persistence.enums.Access;
import com.zincoid.nullbot.platform.persistence.enums.Language;

/**
 * Zincoid 图片指令：从图库插件数据目录的 zincoid 分类中随机发送一张图片。
 * <p>Zincoid command: sends a random image from the zincoid category in the gallery plugin data directory.</p>
 */
@CmdMapping({"Zincoid", "zn", "锌"})
public class ZincoidCmd extends GalleryImageCmd {

    public ZincoidCmd(GalleryPlugin plugin) {
        super(plugin, "zincoid");
    }

    // ════ Cmd ═════════════════════

    @Override
    public Access getAccess() {
        return Access.ADMIN;
    }

    @Override
    public boolean isAiCallable() {
        return true;
    }

    @Override
    public String getHelp(Language language) {
        return GalleryText.ZINCOID_HELP.of(language);
    }

    @Override
    public String getAiHelp() {
        return """
                Zincoid
                Sends a random Zincoid's crossdress photo.
                Usage: Zincoid""";
    }
}
