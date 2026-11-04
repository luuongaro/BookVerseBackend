package com.grupo3.BookVerse.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.time.Clock;
import java.util.concurrent.Executor;

@Configuration
public class PasswordRecoveryConfig {
    @Bean
    public Clock passwordRecoveryClock() {
        return Clock.systemUTC();
    }

    @Bean(name = "passwordRecoveryTaskExecutor")
    public Executor passwordRecoveryTaskExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("BookVerseRecovery-");
        executor.initialize();
        return executor;
    }
}
