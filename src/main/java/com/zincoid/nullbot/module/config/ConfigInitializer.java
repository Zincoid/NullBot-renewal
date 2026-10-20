package com.zincoid.nullbot.module.config;

import com.zincoid.nullbot.platform.persistence.po.Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds missing configuration entries with their default values at startup.
 * <p>启动时为缺失的配置项写入默认值。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigInitializer implements ApplicationRunner {

    private final ConfigService configService;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        for (Conf conf : Conf.values()) {
            if (configService.lambdaQuery().eq(Config::getKey, conf.getKey()).exists()) continue;
            configService.save(Config.builder()
                    .key(conf.getKey())
                    .value(conf.getDefaultValue())
                    .description(conf.getDescription())
                    .updatedAt(LocalDateTime.now())
                    .build());
            log.info("Config seeded: {}", conf.getKey());
        }
    }
}
