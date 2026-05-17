package com.jobsignal.scraper.service;

import com.jobsignal.scraper.config.RateLimiterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScraperRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;

    /**
     * Returns true if the scrape for the given source is allowed under the rate limit.
     * Key pattern: ratelimit:scraper:<source>
     */
    public boolean tryAcquire(String source) {
        String key = "ratelimit:scraper:" + source;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(properties.windowSeconds()));
            }
            boolean allowed = count <= properties.maxRequestsPerWindow();
            if (!allowed) {
                log.warn("Rate limit exceeded source={} count={} limit={} windowSeconds={}",
                        source, count, properties.maxRequestsPerWindow(), properties.windowSeconds());
            }
            return allowed;
        } catch (Exception e) {
            // Redis unavailable — allow the request and log a warning
            log.warn("Redis unavailable for rate limiter source={}: {}", source, e.getMessage());
            return true;
        }
    }
}
