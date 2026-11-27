package com.halofuel.fuelbridge.platform.ordering.infrastructure.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/** Enables the scheduled outbox relay and provides the clock used for its timestamps. */
@Configuration
@EnableScheduling
public class OutboxConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
