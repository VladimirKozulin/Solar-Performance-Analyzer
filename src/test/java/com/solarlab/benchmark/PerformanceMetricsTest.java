package com.solarlab.benchmark;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PerformanceMetrics.
 */
class PerformanceMetricsTest {
    
    private PerformanceMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new PerformanceMetrics();
    }

    @Test
    void testInitialState() {
        assertEquals(0, metrics.getTotalFrames());
        assertEquals(0, metrics.getTotalBytes());
        assertEquals(0.0, metrics.getAverageLatency());
        assertEquals(0.0, metrics.getThroughput());
    }

    @Test
    void testRecordDownload() {
        metrics.recordDownload(1024);
        metrics.recordDownload(2048);
        
        assertEquals(3072, metrics.getTotalBytes());
    }

    @Test
    void testRecordProcessing() {
        metrics.recordProcessing(1_000_000_000L); // 1 second in nanos
        metrics.recordProcessing(2_000_000_000L); // 2 seconds in nanos
        
        assertEquals(2, metrics.getTotalFrames());
        assertEquals(1500.0, metrics.getAverageLatency(), 0.1);
    }

    @Test
    void testGpuCpuComparison() {
        metrics.recordGpuProcessing(10_000_000L); // 10ms
        metrics.recordCpuProcessing(100_000_000L); // 100ms
        metrics.recordProcessing(110_000_000L);
        
        assertTrue(metrics.getSpeedup() > 1.0);
    }

    @Test
    void testReset() {
        metrics.recordDownload(1024);
        metrics.recordProcessing(1_000_000_000L);
        
        metrics.reset();
        
        assertEquals(0, metrics.getTotalFrames());
        assertEquals(0, metrics.getTotalBytes());
    }

    @Test
    void testLatencyTracking() {
        metrics.recordProcessing(5_000_000L);   // 5ms
        metrics.recordProcessing(10_000_000L);  // 10ms
        metrics.recordProcessing(15_000_000L);  // 15ms
        
        assertEquals(5, metrics.getMinLatency());
        assertEquals(15, metrics.getMaxLatency());
    }
}
