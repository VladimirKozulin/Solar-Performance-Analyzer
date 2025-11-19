package com.solarlab.benchmark;

import com.solarlab.core.CpuImageProcessor;
import com.solarlab.gpu.GpuImageProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for CPU vs GPU performance comparison.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class PerformanceBenchmark {
    
    private GpuImageProcessor gpuProcessor;
    private CpuImageProcessor cpuProcessor;
    private ByteBuf testData;

    @Setup
    public void setup() {
        gpuProcessor = new GpuImageProcessor();
        cpuProcessor = new CpuImageProcessor();
        
        // Create test image data (1024x1024 JPEG)
        byte[] dummyData = new byte[1024 * 1024];
        for (int i = 0; i < dummyData.length; i++) {
            dummyData[i] = (byte) (i % 256);
        }
        testData = Unpooled.wrappedBuffer(dummyData);
    }

    @Benchmark
    public byte[] benchmarkGpuProcessing() {
        return gpuProcessor.process(testData);
    }

    @Benchmark
    public byte[] benchmarkCpuProcessing() {
        return cpuProcessor.process(testData);
    }

    @TearDown
    public void tearDown() {
        gpuProcessor.shutdown();
        testData.release();
    }
}
