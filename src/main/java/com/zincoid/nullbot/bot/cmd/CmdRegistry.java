package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.platform.configuration.CmdProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Registry of {@link Cmd} beans indexed by command names.
 * <p>按指令名索引 {@link Cmd} 的注册表。</p>
 */
@Component
public class CmdRegistry {

    private final List<Cmd> cmds;
    private final Map<String, Cmd> cmdMap;

    public CmdRegistry(CmdProperties cmdProperties, List<Cmd> cmds) {
        this.cmds = cmds;
        this.cmdMap = cmdProperties.isIgnoreCase()
                ? new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                : new HashMap<>();
        for (Cmd cmd : cmds) {
            CmdMapping mapping = cmd.getClass().getAnnotation(CmdMapping.class);
            if (mapping == null) continue;
            for (String cmdName : mapping.value())
                cmdMap.put(cmdName, cmd);
        }
    }

    /**
     * Looks up a command by name.
     * <p>按名称查找指令。</p>
     *
     * @param name the command name
     * @return the command, or null when unknown
     */
    public Cmd get(String name) {
        return cmdMap.get(name);
    }

    /**
     * Collects the AI help docs of the given commands.
     * <p>汇总指定指令的 AI 帮助文档。</p>
     *
     * @param types the command types to include
     * @return the joined help docs
     */
    public String getAiDoc(Set<Class<? extends Cmd>> types) {
        String body = cmds.stream()
                .filter(cmd -> types.stream().anyMatch(t -> t.isInstance(cmd)))
                .map(Cmd::getAiHelp)
                .collect(Collectors.joining("\n\n"))
                .trim();
        return body.isEmpty() ? "No Cmd Docs." : "Cmd Docs:\n\n" + body;
    }
}
