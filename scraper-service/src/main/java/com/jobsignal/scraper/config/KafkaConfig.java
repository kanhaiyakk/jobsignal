package com.jobsignal.scraper.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.raw-listings}")
    private String rawListingsTopic;

    @Value("${kafka.topics.failed-listings}")
    private String failedListingsTopic;

    @Bean
    public NewTopic rawListingsTopic() {
        return TopicBuilder.name(rawListingsTopic).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic failedListingsTopic() {
        return TopicBuilder.name(failedListingsTopic).partitions(1).replicas(1).build();
    }
}
