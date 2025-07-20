package com.example.demo.runner;

import com.example.demo.consumer.ConsumerService;
import com.example.demo.metrics.MetricsLogger;
import com.example.demo.producer.ProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TestRunner implements CommandLineRunner {
    private final ProducerService producerService;
    private final ConsumerService consumerService;
    private final MetricsLogger metricsLogger;

    @Override
    public void run(String... args) throws Exception {
        String topic = "test-topic";
        int messageCount = 1000;
        String resultFile = "results/test-result.csv";
        Set<Integer> sentMessageIds = new HashSet<>();

        System.out.println("메시지 전송중...");
        long start = System.currentTimeMillis();
        producerService.sendMessage(topic, messageCount);

        Thread.sleep(5000);

        System.out.println("메시지 분석중...");
        long end = System.currentTimeMillis();

        double throughput = (double) (messageCount * 1000) / (end - start);
        metricsLogger.writeMetricsToCsv(resultFile, consumerService.getLatencyMap(), throughput);

        System.out.println("테스트 완료");
    }
}
