package com.zincoid.gallery;

import com.zincoid.nullbot.bot.cmd.CmdMapping;
import com.zincoid.nullbot.platform.persistence.enums.Language;

/**
 * 二次元图片指令：从图库插件数据目录的 anime 分类中随机发送一张图片。
 * <p>Anime command: sends a random image from the anime category in the gallery plugin data directory.</p>
 */
@CmdMapping({"Anime", "二次元"})
public class AnimeCmd extends GalleryImageCmd {

    public AnimeCmd(GalleryPlugin plugin) {
        super(plugin, "anime");
    }

    // ════ Cmd ═════════════════════

    @Override
    public String getHelp(Language language) {
        return GalleryText.ANIME_HELP.of(language);
    }
}
