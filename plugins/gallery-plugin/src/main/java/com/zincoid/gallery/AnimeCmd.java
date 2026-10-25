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
        return language == Language.EN ? """
                Anime
                Sends a random anime image.
                Usage: Anime
                Aliases: 二次元
                Access: >=0(normal)""" : """
                Anime
                随机发送一张二次元图片。
                用法：Anime
                别名：二次元
                权限：>=0(normal)""";
    }
}
