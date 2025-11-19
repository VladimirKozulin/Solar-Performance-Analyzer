package com.solarlab.benchmark;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lock-free performance metrics collector.
 */
public class PerformanceMetrics {
    private final LongAdder totalDownloads = new LongAdder();
    private final LongAdder totalBytes = new LongAdder();
    private final LongAdder totalProcessingTime = new LongAdder();
    private final LongAdder gpuProcessingTime = new LongAdder();
    private final LongAdder cpuProcessingTime = new LongAdder();
    private final LongAdder processedFrames = new LongAdder();
    
    private final AtomicLong minLatency = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxLatency = new AtomicLong(0);
    
    private volatile long startTime = System.currentTimeMillis();

    public void recordDownload(int bytes) {
        totalDownloads.increment();
        totalBytes.add(bytes);
    }

    public void recordProcessing(long nanos) {
        totalProcessingTime.add(nanos);
        processedFrames.increment();
        
        long millis = nanos / 1_000_000;
        updateLatency(millis);
    }

    public void recordGpuProcessing(long nanos) {
        gpuProcessingTime.add(nanos);
    }

    public void recordCpuProcessing(long nanos) {
        cpuProcessingTime.add(nanos);
    }

    private void updateLatency(long latency) {
        minLatency.updateAndGet(current -> Math.min(current, latency));
        maxLatency.updateAndGet(current -> Math.max(current, latency));
    }

    public double getAverageLatency() {
        long frames = processedFrames.sum();
        if (frames == 0) return 0;
        return (double) totalProcessingTime.sum() / frames / 1_000_000;
    }

    public double getGpuAverageLatency() {
        long frames = processedFrames.sum();
        if (frames == 0) return 0;
        return (double) gpuProcessingTime.sum() / frames / 1_000_000;
    }

    public double getCpuAverageLatency() {
        long frames = processedFrames.sum();
        if (frames == 0) return 0;
        return (double) cpuProcessingTime.sum() / frames / 1_000_000;
    }

    public double getSpeedup() {
        long gpu = gpuProcessingTime.sum();
        long cpu = cpuProcessingTime.sum();
        if (gpu == 0) return 1.0;
        return (double) cpu / gpu;
    }

    public double getThroughput() {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed == 0) return 0;
        return (double) processedFrames.sum() * 1000 / elapsed;
    }

    public long getMinLatency() {
        long min = minLatency.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxLatency() {
        return maxLatency.get();
    }

    public long getTotalFrames() {
        return processedFrames.sum();
    }

    public long getTotalBytes() {
        return totalBytes.sum();
    }

    public void reset() {
        totalDownloads.reset();
        totalBytes.reset();
        totalProcessingTime.reset();
        gpuProcessingTime.reset();
        cpuProcessingTime.reset();
        processedFrames.reset();
        minLatency.set(Long.MAX_VALUE);
        maxLatency.set(0);
        startTime = System.currentTimeMillis();
    }
}
