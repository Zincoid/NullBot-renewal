package com.zincoid.nullbot.module.config;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.zincoid.nullbot.platform.persistence.mapper.ConfigMapper;
import com.zincoid.nullbot.platform.persistence.po.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * {@link ConfigService} 的默认实现。
 * <p>Default implementation of {@link ConfigService}.</p>
 */
@Slf4j
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

    // ════ Get ═════════════════════

    @Override
    public String get(Conf conf) {
        if (conf == null) throw new ConfigException("Missing key: conf");
        Config config = lambdaQuery().eq(Config::getKey, conf.getKey()).one();
        return config == null || config.getValue() == null
                ? conf.getDefaultValue()
                : config.getValue();
    }

    // ════ Update ══════════════════

    @Override
    public void update(Conf conf, String value) {
        if (conf == null) throw new ConfigException("Missing key: conf");
        Config config = lambdaQuery().eq(Config::getKey, conf.getKey()).one();
        if (config == null) throw new ConfigException("Config not found: " + conf.getKey());
        if (Objects.equals(value, config.getValue())) return;
        lambdaUpdate()
                .eq(Config::getKey, conf.getKey())
                .set(Config::getValue, value)
                .set(Config::getUpdatedAt, LocalDateTime.now())
                .update();
        log.info("Config updated: {}", conf.getKey());
    }

    // ════ Delete ══════════════════

    @Override
    public void delete(Conf conf) {
        if (conf == null) throw new ConfigException("Missing key: conf");
        Config config = lambdaQuery().eq(Config::getKey, conf.getKey()).one();
        if (config == null) throw new ConfigException("Config not found: " + conf.getKey());
        removeById(config.getId());
        log.info("Config deleted: {}", conf.getKey());
    }
}
