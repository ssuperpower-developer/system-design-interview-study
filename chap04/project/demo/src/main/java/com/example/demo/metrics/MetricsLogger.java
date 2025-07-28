package com.example.demo.metrics;

import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Component
public class MetricsLogger {
    public void writeMetricsToCsv(String filePath, Map<Integer, Long> latencyMap, double throughput) throws IOException {
        long totalLatency = 0L;
        long minLatency = Long.MAX_VALUE;
        long maxLatency = Long.MIN_VALUE;

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write("id,latency_ms\n");
            for (Map.Entry<Integer, Long> entry : latencyMap.entrySet()) {
                int id = entry.getKey();
                long latency = entry.getValue();
                fileWriter.write(id + "," + latency + "\n");

                totalLatency += latency;
                maxLatency = Math.max(maxLatency, latency);
                minLatency = Math.min(minLatency, latency);
            }

            long avgLatency = latencyMap.isEmpty() ? 0 : totalLatency / latencyMap.size();
            fileWriter.write("\nSUMMARY\n");
            fileWriter.write("count: " + latencyMap.size() + "\n");
            fileWriter.write("throughput: " + throughput + "\n");
            fileWriter.write("avg: " + avgLatency + "\n");
            fileWriter.write("min: " + minLatency + "\n");
            fileWriter.write("max: " + maxLatency + "\n");
        }
    }
}
