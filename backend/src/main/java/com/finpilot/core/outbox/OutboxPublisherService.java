package com.finpilot.core.outbox;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxRepository outboxRepository;
    private final org.springframework.beans.factory.ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Transactional
    public void publishEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .id("out-" + UUID.randomUUID().toString().substring(0, 8))
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status("PENDING")
                .retryCount(0)
                .createdAt(Instant.now())
                .build();

        outboxRepository.save(event);
        log.debug("Enqueued outbox event: aggregateType={}, eventType={}", aggregateType, eventType);
    }

    @Scheduled(fixedDelayString = "${finpilot.outbox.poll-interval-ms:5000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        if (pendingEvents.isEmpty()) {
            return;
        }

        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        for (OutboxEvent event : pendingEvents) {
            try {
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(event.getAggregateType().toLowerCase() + "-topic", event.getAggregateId(), event.getPayload());
                    event.setStatus("PUBLISHED");
                    event.setProcessedAt(Instant.now());
                    log.info("Dispatched outbox event {} to Kafka", event.getId());
                } else {
                    // Graceful fallback when Kafka broker is optional in local standalone mode
                    event.setStatus("PUBLISHED");
                    event.setProcessedAt(Instant.now());
                    log.debug("Processed outbox event {} in local standalone mode", event.getId());
                }
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                    log.error("Outbox event {} exceeded max retries. Marked as FAILED.", event.getId(), e);
                } else {
                    log.warn("Failed to publish outbox event {}. Will retry. Error: {}", event.getId(), e.getMessage());
                }
            }
            outboxRepository.save(event);
        }
    }
}
