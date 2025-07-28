package com.example.demo.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Getter
public class ConsumerService {
    private final ConcurrentHashMap<Integer, Long> latencyMap = new ConcurrentHashMap<>();
    private final List<Integer> receivedMessageIds = Collections.synchronizedList(new ArrayList<>());

    @KafkaListener(topics = "test-topic", groupId = "load-test-group")
    public void listen(ConsumerRecord<String, String> record) {
        String[] parts = record.value().split(",");
        int id = Integer.parseInt(parts[0]);
        receivedMessageIds.add(id);
        long sendTime = Long.parseLong(parts[1]);
        long receiveTime = Instant.now().toEpochMilli();
        long latency = receiveTime - sendTime;

        latencyMap.put(id, latency);

        System.out.println("id : " + id + " latency : " + latency);
    }
}
