package com.stocktracker.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            // Existing portfolio cache (2 minutes)
            new CaffeineCache("portfolio",
                Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(2))
                    .maximumSize(1000)
                    .build()),

            // NEW: Historical data cache (30 minutes - data doesn't change often)
            new CaffeineCache("historicalData",
                Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .maximumSize(500)
                    .build()),

            // NEW: Performance history cache (10 minutes)
            new CaffeineCache("performanceHistory",
                Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .maximumSize(200)
                    .build())
        ));

        return cacheManager;
    }
}
