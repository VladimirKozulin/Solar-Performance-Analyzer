package com.solarlab.core;

/**
 * Container for processed image data with performance metrics.
 */
public class ProcessedImage {
    private final byte[] gpuData;
    private final byte[] cpuData;
    private final long gpuProcessingTime;
    private final long cpuProcessingTime;
    private final int originalSize;

    public ProcessedImage(byte[] gpuData, byte[] cpuData, 
                         long gpuProcessingTime, long cpuProcessingTime, 
                         int originalSize) {
        this.gpuData = gpuData;
        this.cpuData = cpuData;
        this.gpuProcessingTime = gpuProcessingTime;
        this.cpuProcessingTime = cpuProcessingTime;
        this.originalSize = originalSize;
    }

    public byte[] getData() {
        return gpuData != null ? gpuData : cpuData;
    }

    public byte[] getGpuData() {
        return gpuData;
    }

    public byte[] getCpuData() {
        return cpuData;
    }

    public long getGpuProcessingTime() {
        return gpuProcessingTime;
    }

    public long getCpuProcessingTime() {
        return cpuProcessingTime;
    }
    
    public long getProcessingTime() {
        return gpuProcessingTime > 0 ? gpuProcessingTime : cpuProcessingTime;
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public double getSpeedup() {
        if (gpuProcessingTime == 0 || cpuProcessingTime == 0) {
            return 1.0;
        }
        return (double) cpuProcessingTime / gpuProcessingTime;
    }
}
