package com.example.demo.runner;

import com.example.demo.consumer.ConsumerScalingService;
import com.example.demo.metrics.MetricsLogger;
import com.example.demo.producer.ProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ConsumerScalingTestRunner implements CommandLineRunner {

    private final ProducerService producerService;
    private final ConsumerScalingService consumerService;
    private final MetricsLogger metricsLogger;

    @Override
    public void run(String... args) throws Exception {
        int totalMessages = 1000;

        consumerService.clearLatencyMap();

        long start = System.currentTimeMillis();
        for (int i = 0; i < totalMessages; i++) {
            producerService.sendMessage("test-topic", i);
        }

        Thread.sleep(5000);

        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");
        double throughput = (double) (totalMessages * 1000) / (end - start);

        ConcurrentHashMap<Integer, Long> latencyMap = consumerService.getLatencyMap();
        metricsLogger.writeMetricsToCsv("results/test-consumer-scaling.csv", latencyMap, throughput);
    }
}