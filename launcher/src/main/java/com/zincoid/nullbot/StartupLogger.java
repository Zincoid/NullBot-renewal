package com.zincoid.nullbot;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/**
 * 启动时输出版本信息。
 * <p>Logs application version information during startup.</p>
 */
@Component
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private final ObjectProvider<BuildProperties> builds;

    /**
     * 创建启动版本日志组件。
     * <p>Creates the startup version logger.</p>
     *
     * @param builds the build properties provider
     */
    public StartupLogger(ObjectProvider<BuildProperties> builds) {
        this.builds = builds;
    }

    /**
     * 输出构建版本；未打包运行时回退为 dev。
     * <p>Logs the build version, falling back to dev when running unpackaged.</p>
     *
     * @param args application arguments
     */
    @Override
    public void run(@NonNull ApplicationArguments args) {
        BuildProperties build = builds.getIfAvailable();
        if (build == null || build.getVersion() == null) {
            log.info("NullBot renewal version: dev");
            return;
        }
        if (build.getTime() == null) {
            log.info("NullBot renewal version: {}", build.getVersion());
            return;
        }
        log.info("NullBot renewal version: {} (build: {})",
                build.getVersion(), build.getTime());
    }
}
