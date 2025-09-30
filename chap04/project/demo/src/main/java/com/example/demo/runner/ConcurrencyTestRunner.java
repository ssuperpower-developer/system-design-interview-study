package com.example.demo.runner;

import com.example.demo.consumer.ConsumerService;
import com.example.demo.metrics.MetricsLogger;
import com.example.demo.producer.ProducerService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class ConcurrencyTestRunner implements CommandLineRunner {
    private final ProducerService producerService;
    private final ConsumerService consumerService;
    private final MetricsLogger metricsLogger;

    @Override
    public void run(String... args) throws Exception {
        int numProducers = 5;
        int messagePerProducer = 1000;

        Map<Integer, Long> latencyMap = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numProducers);

        CountDownLatch latch = new CountDownLatch(numProducers);

        long start = System.currentTimeMillis();

        for(int i = 0; i < numProducers; i++) {
            final int producerId = i;
            executorService.submit(() -> {
                try {
                    for(int j = 0; j < messagePerProducer; j++) {
                        int messageId = producerId * messagePerProducer + j;
                        long startTime = System.currentTimeMillis();
                        producerService.sendMessage("load-test-topic", messageId);
                        long latency = System.currentTimeMillis() - startTime;
                        latencyMap.put(messageId, latency);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("Total time: " + (end - start) + "ms");

        double throughput = (double) (numProducers * messagePerProducer * 1000.0) / (end - start);

        executorService.shutdown();

        metricsLogger.writeMetricsToCsv("results/test-concurrent-result.csv", latencyMap, throughput);
    }
}
