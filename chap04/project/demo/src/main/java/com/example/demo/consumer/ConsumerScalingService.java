package com.example.demo.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Service
@RequiredArgsConstructor
public class ConsumerScalingService {
    private final ConcurrentHashMap<Integer, Long> latencyMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "test-topic", groupId = "load-test-group", concurrency = "5") // concurrency 값 조절
    public void listen(ConsumerRecord<String, String> record) {
        String[] parts = record.value().split(",");
        int id = Integer.parseInt(parts[0]);
        long sendTime = Long.parseLong(parts[1]);
        long receiveTime = Instant.now().toEpochMilli();
        long latency = receiveTime - sendTime;

        latencyMap.put(id, latency);
    }

    public void clearLatencyMap() {
        latencyMap.clear();
    }
}
