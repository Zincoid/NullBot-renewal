package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.platform.configuration.CmdProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按指令名索引 {@link Cmd} 的注册表。
 * <p>Registry of {@link Cmd} beans indexed by command names.</p>
 */
@Slf4j
@Component
public class CmdRegistry {

    private final Map<String, Cmd> cmdMap = new ConcurrentHashMap<>();
    private final CmdProperties cmdProperties;

    public CmdRegistry(CmdProperties cmdProperties, List<Cmd> cmds) {
        this.cmdProperties = cmdProperties;
        if (!register(cmds))
            throw new CmdException("Built-in command registration failed");
        log.info("Commands registered: {}", cmdMap.size());
    }

    // ════ Get ═════════════════════

    /**
     * 按名称查找指令。
     * <p>Looks up a command by name.</p>
     *
     * @param name the command name
     * @return the command, or null when unknown
     */
    public Cmd get(String name) {
        return name == null ? null : cmdMap.get(key(name));
    }

    // ════ Register ════════════════

    /**
     * 原子注册一组指令，任一名称冲突时整组不注册。
     * <p>Registers a group of cmds atomically; the whole group is rejected when any name conflicts.</p>
     *
     * @param cmds the cmds to register
     * @return true when all cmds were registered
     */
    public synchronized boolean register(Collection<Cmd> cmds) {
        if (cmds == null || cmds.isEmpty()) return true;
        Map<String, Cmd> pending = new LinkedHashMap<>();
        for (Cmd cmd : cmds) {
            if (cmd == null) return false;
            CmdMapping mapping = cmd.getClass().getAnnotation(CmdMapping.class);
            if (mapping == null || mapping.value().length == 0) return false;
            for (String name : mapping.value()) {
                if (name == null || name.isBlank()) return false;
                String key = key(name);
                if (cmdMap.containsKey(key) || pending.containsKey(key)) return false;
                pending.put(key, cmd);
            }
        }
        cmdMap.putAll(pending);
        return true;
    }

    private String key(String name) {
        return cmdProperties.isIgnoreCase() ? name.toLowerCase(Locale.ROOT) : name;
    }
}
