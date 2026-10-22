package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.ConfigUpdate;
import com.zincoid.nullbot.console.dto.ConfigView;
import com.zincoid.nullbot.console.exception.ConsoleException;
import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import com.zincoid.nullbot.platform.persistence.po.Config;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 控制台配置接口：查询、更新与重置机器人配置。
 * <p>Console config API: queries, updates and resets bot configuration.</p>
 */
@RestController
@RequestMapping("/console/config")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    // ════ Query ══════════════════

    /**
     * 列出所有配置项。
     * <p>Lists all configuration entries.</p>
     *
     * @return the configuration entries
     */
    @GetMapping
    public List<ConfigView> list() {
        Map<String, Config> stored = configService.list().stream()
                .collect(Collectors.toMap(Config::getKey, Function.identity()));
        return Arrays.stream(Conf.values())
                .map(conf -> ConfigView.of(conf, stored.get(conf.getKey())))
                .toList();
    }

    /**
     * 查询单个配置项。
     * <p>Returns one configuration entry.</p>
     *
     * @param key config key or enum name
     * @return the configuration entry
     */
    @GetMapping("/{key}")
    public ConfigView get(@PathVariable String key) {
        return view(conf(key));
    }

    // ════ Update ═════════════════

    /**
     * 更新配置项的值。
     * <p>Updates the value of a configuration entry.</p>
     *
     * @param key  config key or enum name
     * @param body the new value
     * @return the updated configuration entry
     */
    @PutMapping("/{key}")
    public ConfigView update(@PathVariable String key, @RequestBody ConfigUpdate body) {
        Conf conf = conf(key);
        configService.update(conf, validate(conf, body.value()));
        return view(conf);
    }

    /**
     * 恢复配置项的默认值。
     * <p>Restores the default value of a configuration entry.</p>
     *
     * @param key config key or enum name
     * @return the restored configuration entry
     */
    @PostMapping("/{key}/reset")
    public ConfigView reset(@PathVariable String key) {
        Conf conf = conf(key);
        configService.update(conf, conf.getDefaultValue());
        return view(conf);
    }

    // ════ Helpers ═════════════════

    private ConfigView view(Conf conf) {
        return ConfigView.of(conf, configService.get(conf));
    }

    private static Conf conf(String raw) {
        for (Conf conf : Conf.values())
            if (conf.getKey().equalsIgnoreCase(raw) || conf.name().equalsIgnoreCase(raw))
                return conf;
        throw new ConsoleException(HttpStatus.NOT_FOUND, "Unknown config key: " + raw);
    }

    private static String validate(Conf conf, String value) {
        if (value == null)
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Missing value");
        if (conf == Conf.DEFAULT_LANGUAGE) {
            Language language = Language.fromLabel(value);
            if (language != Language.ZH && language != Language.EN)
                throw new ConsoleException(HttpStatus.BAD_REQUEST,
                        "Invalid default_language: " + value);
            return language.getLabel();
        }
        if (conf == Conf.MESSAGE_HISTORY_MAX_LENGTH
                || conf == Conf.AI_MAX_TOKENS
                || conf == Conf.AI_MAX_TOOL_CALLS)
            return integer(value);
        return value;
    }

    private static String integer(String value) {
        try {
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException e) {
            throw new ConsoleException(HttpStatus.BAD_REQUEST, "Invalid integer: " + value);
        }
    }
}
