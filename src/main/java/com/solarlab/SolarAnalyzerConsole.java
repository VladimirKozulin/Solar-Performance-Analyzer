package com.solarlab;

import com.solarlab.core.ProcessedImage;
import com.solarlab.core.SolarDataPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Консольная версия анализатора солнечных данных.
 * 
 * Демонстрирует:
 * - Загрузку реальных данных с NASA SOHO
 * - Сравнение производительности GPU vs CPU
 * - Реактивную обработку данных
 * - Метрики производительности в реальном времени
 * 
 * Console version of Solar Analyzer for testing without JavaFX.
 */
public class SolarAnalyzerConsole {
    private static final Logger logger = LoggerFactory.getLogger(SolarAnalyzerConsole.class);
    
    public static void main(String[] args) {
        // Вывод информации о системе / System information
        logger.info("=".repeat(80));
        logger.info("Solar Performance Analyzer - Console Mode");
        logger.info("=".repeat(80));
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        logger.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
        logger.info("=".repeat(80));
        
        // Инициализация конвейера обработки / Initialize processing pipeline
        SolarDataPipeline pipeline = new SolarDataPipeline();
        CountDownLatch latch = new CountDownLatch(10); // Обработать 10 изображений / Process 10 images
        
        logger.info("Starting data pipeline...");
        logger.info("Will process 10 images and show performance comparison");
        logger.info("-".repeat(80));
        
        pipeline.getDataStream()
            .take(10)
            .subscribe(
                image -> {
                    displayResults(image, pipeline);
                    latch.countDown();
                },
                error -> {
                    logger.error("Error in pipeline", error);
                    latch.countDown();
                },
                () -> logger.info("Pipeline completed")
            );
        
        pipeline.start();
        
        try {
            // Wait for 10 images or 2 minutes
            if (!latch.await(2, TimeUnit.MINUTES)) {
                logger.warn("Timeout waiting for images");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted", e);
        }
        
        displayFinalStats(pipeline);
        pipeline.shutdown();
        
        logger.info("=".repeat(80));
        logger.info("Application finished. Press Ctrl+C to exit.");
        logger.info("=".repeat(80));
    }
    
    /**
     * Отображение результатов обработки одного кадра.
     * Display results for a single frame.
     */
    private static void displayResults(ProcessedImage image, SolarDataPipeline pipeline) {
        long frame = pipeline.getFrameCount();
        // Конвертация наносекунд в миллисекунды / Convert nanoseconds to milliseconds
        double gpuTime = image.getGpuProcessingTime() / 1_000_000.0;
        double cpuTime = image.getCpuProcessingTime() / 1_000_000.0;
        double speedup = cpuTime > 0 ? cpuTime / gpuTime : 1.0;
        
        logger.info("");
        logger.info("Frame #{}", frame);
        logger.info("  GPU Processing: {} ms", String.format(Locale.US, "%.2f", gpuTime));
        logger.info("  CPU Processing: {} ms", String.format(Locale.US, "%.2f", cpuTime));
        logger.info("  Speedup: {}x", String.format(Locale.US, "%.1f", speedup));
        logger.info("  Image Size: {} KB", image.getOriginalSize() / 1024);
        logger.info("-".repeat(80));
    }
    
    /**
     * Отображение финальной статистики производительности.
     * Display final performance statistics.
     */
    private static void displayFinalStats(SolarDataPipeline pipeline) {
        var metrics = pipeline.getMetrics();
        
        logger.info("");
        logger.info("=".repeat(80));
        logger.info("FINAL PERFORMANCE STATISTICS");
        logger.info("=".repeat(80));
        logger.info("Total Frames Processed: {}", metrics.getTotalFrames());
        logger.info("Total Data Downloaded: {} MB", String.format(Locale.US, "%.2f", metrics.getTotalBytes() / 1024.0 / 1024.0));
        logger.info("");
        logger.info("GPU Performance:");
        logger.info("  Average Latency: {} ms", String.format(Locale.US, "%.2f", metrics.getGpuAverageLatency()));
        logger.info("");
        logger.info("CPU Performance:");
        logger.info("  Average Latency: {} ms", String.format(Locale.US, "%.2f", metrics.getCpuAverageLatency()));
        logger.info("");
        logger.info("Overall Speedup: {}x", String.format(Locale.US, "%.1f", metrics.getSpeedup()));
        logger.info("Throughput: {} frames/sec", String.format(Locale.US, "%.2f", metrics.getThroughput()));
        logger.info("=".repeat(80));
    }
}
