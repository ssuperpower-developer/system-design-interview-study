package com.example.demo.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProducerService {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, int messageCount) {
        for (int i = 0; i < messageCount; i++) {
            long timestamp = Instant.now().toEpochMilli();
            String message = i + "," + timestamp;
            kafkaTemplate.send(topic, Integer.toString(i), message);
        }
    }
}
