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
 * 启动时输出项目 banner 与版本信息。
 * <p>Prints the project banner and application version information during startup.</p>
 */
@Component
public class StartupLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLogger.class);

    private static final String BANNER = """
            ███╗   ██╗██╗   ██╗██╗     ██╗     ██████╗  ██████╗ ████████╗
            ████╗  ██║██║   ██║██║     ██║     ██╔══██╗██╔═══██╗╚══██╔══╝
            ██╔██╗ ██║██║   ██║██║     ██║     ██████╔╝██║   ██║   ██║
            ██║╚██╗██║██║   ██║██║     ██║     ██╔══██╗██║   ██║   ██║
            ██║ ╚████║╚██████╔╝███████╗███████╗██████╔╝╚██████╔╝   ██║
            ╚═╝  ╚═══╝ ╚═════╝ ╚══════╝╚══════╝╚═════╝  ╚═════╝    ╚═╝
            """;

    private final ObjectProvider<BuildProperties> builds;

    public StartupLogger(ObjectProvider<BuildProperties> builds) {
        this.builds = builds;
    }

    /**
     * 输出构建版本。
     * <p>Logs the build version.</p>
     *
     * @param args application arguments
     */
    @Override
    public void run(@NonNull ApplicationArguments args) {
        BANNER.lines().forEach(log::info);
        BuildProperties build = builds.getIfAvailable();
        log.info("NullBot renewal v{} (build: {}) by Zincoid",
                build == null ? null : build.getVersion(),
                build == null ? null : build.getTime());
    }
}
