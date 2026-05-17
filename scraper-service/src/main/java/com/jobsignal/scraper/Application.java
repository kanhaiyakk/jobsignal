package com.jobsignal.scraper;

import com.jobsignal.scraper.config.RateLimiterProperties;
import com.jobsignal.scraper.config.RemoteOkProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RemoteOkProperties.class, RateLimiterProperties.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
