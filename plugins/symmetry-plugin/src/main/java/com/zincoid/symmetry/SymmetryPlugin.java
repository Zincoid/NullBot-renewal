package com.zincoid.symmetry;

import com.zincoid.nullbot.bot.cmd.Cmd;
import com.zincoid.nullbot.bot.plugin.Plugin;
import com.zincoid.symmetry.cmd.SymmetryCmd;

import java.util.List;

/**
 * 对称插件：注册图片对称指令。
 * <p>Symmetry plugin registering the image symmetry command.</p>
 */
public class SymmetryPlugin implements Plugin {

    @Override
    public String id() {
        return "symmetry";
    }

    @Override
    public List<Cmd> cmds() {
        return List.of(new SymmetryCmd());
    }
}
