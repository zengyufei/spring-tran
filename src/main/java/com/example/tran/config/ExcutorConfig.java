package com.example.tran.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ExcutorConfig {
    @Lazy
    @Bean(
            name = {"applicationTaskExecutor", "taskExecutor"}
    )
    public Executor applicationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(6000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("taskExecutor-");
        executor.setRejectedExecutionHandler((r, e) -> {
            try {
                // 将任务添加到队列中,阻塞直到有可用线程
                // 这里不会成为问题点，如果你的线程任务内没有阻塞点，即使抛异常，这里也都不会成为问题
                e.getQueue().put(r);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        return executor;
    }

    @Lazy
    @Bean(
            name = {"myTask"}
    )
    public Executor myTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(6000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("myTaskExecutor-");
        executor.setRejectedExecutionHandler((r, e) -> {
            try {
                // 将任务添加到队列中,阻塞直到有可用线程
                // 这里不会成为问题点，如果你的线程任务内没有阻塞点，即使抛异常，这里也都不会成为问题
                e.getQueue().put(r);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        return executor;
    }

}
