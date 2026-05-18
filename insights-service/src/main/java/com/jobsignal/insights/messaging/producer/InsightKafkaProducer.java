package com.jobsignal.insights.messaging.producer;

import com.jobsignal.shared.event.InsightGeneratedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InsightKafkaProducer implements InsightEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public InsightKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${kafka.topics.insights-generated}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(InsightGeneratedEvent event) {
        kafkaTemplate.send(topic, event.eventId().toString(), event);
        log.debug("Published InsightGeneratedEvent reportId={}", event.payload().reportId());
    }
}
