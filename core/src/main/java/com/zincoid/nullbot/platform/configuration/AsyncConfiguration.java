package com.zincoid.nullbot.platform.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步执行配置：为入站机器人事件提供虚拟线程执行器。
 * <p>Async configuration: provides a virtual-thread executor for inbound bot events.</p>
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * 注册机器人事件执行器，每个事件使用一个虚拟线程。
     * <p>Registers the bot event executor, using one virtual thread per event.</p>
     *
     * @return the virtual-thread executor
     */
    @Bean(name = "AsyncRunner", destroyMethod = "shutdown")
    public ExecutorService asyncRunner() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
