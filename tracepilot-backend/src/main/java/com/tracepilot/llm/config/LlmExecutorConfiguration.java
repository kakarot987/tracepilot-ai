
package com.tracepilot.llm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class LlmExecutorConfiguration {

    @Bean(name = "llmExecutor", destroyMethod = "shutdownNow")
    public ExecutorService llmExecutor() {

        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(4),
                r -> {
                    Thread t = new Thread(r, "tracepilot-llm");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
