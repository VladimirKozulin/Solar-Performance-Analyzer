package com.solarlab.core;

import com.solarlab.benchmark.PerformanceMetrics;
import com.solarlab.gpu.GpuImageProcessor;
import com.solarlab.netty.NettyImageDownloader;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance reactive pipeline for solar data processing.
 * Implements zero-copy data flow with GPU acceleration.
 */
public class SolarDataPipeline {
    private static final Logger logger = LoggerFactory.getLogger(SolarDataPipeline.class);
    
    private static final String PRIMARY_URL = "https://sohowww.nascom.nasa.gov/data/realtime/eit_195/1024/latest.jpg";
    private static final String FALLBACK_URL = "https://soho.nascom.nasa.gov/data/realtime/eit_195/1024/latest.jpg";
    private static final Duration UPDATE_INTERVAL = Duration.ofSeconds(5);
    
    private final NettyImageDownloader downloader;
    private final GpuImageProcessor gpuProcessor;
    private final CpuImageProcessor cpuProcessor;
    private final PerformanceMetrics metrics;
    
    private final AtomicLong frameCounter = new AtomicLong(0);
    private volatile boolean running = false;
    
    private Flux<ProcessedImage> dataStream;

    public SolarDataPipeline() {
        this.downloader = new NettyImageDownloader();
        this.gpuProcessor = new GpuImageProcessor();
        this.cpuProcessor = new CpuImageProcessor();
        this.metrics = new PerformanceMetrics();
        
        initializePipeline();
    }

    private void initializePipeline() {
        dataStream = Flux.interval(Duration.ZERO, UPDATE_INTERVAL)
            .flatMap(tick -> downloadImage())
            .flatMap(this::processImage)
            .doOnNext(image -> {
                long frame = frameCounter.incrementAndGet();
                if (frame % 10 == 0) {
                    logger.info("Processed {} frames. Avg latency: {} ms", 
                        frame, metrics.getAverageLatency());
                }
            })
            .doOnError(error -> logger.error("Pipeline error", error))
            .retry()
            .share()
            .subscribeOn(Schedulers.parallel());
    }

    private Mono<ByteBuf> downloadImage() {
        return downloader.download(PRIMARY_URL)
            .onErrorResume(error -> {
                logger.warn("Primary URL failed, trying fallback: {}", error.getMessage());
                return downloader.download(FALLBACK_URL);
            })
            .timeout(Duration.ofSeconds(10))
            .doOnSuccess(buf -> metrics.recordDownload(buf.readableBytes()));
    }

    private Mono<ProcessedImage> processImage(ByteBuf imageData) {
        long startTime = System.nanoTime();
        
        return Mono.zip(
            processWithGpu(imageData),
            processWithCpu(imageData)
        )
        .map(tuple -> {
            ProcessedImage gpuResult = tuple.getT1();
            ProcessedImage cpuResult = tuple.getT2();
            
            long totalTime = System.nanoTime() - startTime;
            metrics.recordProcessing(totalTime);
            
            return new ProcessedImage(
                gpuResult.getData(),
                cpuResult.getData(),
                gpuResult.getProcessingTime(),
                cpuResult.getProcessingTime(),
                imageData.readableBytes()
            );
        })
        .doFinally(signal -> imageData.release());
    }

    private Mono<ProcessedImage> processWithGpu(ByteBuf data) {
        return Mono.fromCallable(() -> {
            long start = System.nanoTime();
            byte[] result = gpuProcessor.process(data);
            long duration = System.nanoTime() - start;
            metrics.recordGpuProcessing(duration);
            return new ProcessedImage(result, null, duration, 0, data.readableBytes());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<ProcessedImage> processWithCpu(ByteBuf data) {
        return Mono.fromCallable(() -> {
            long start = System.nanoTime();
            byte[] result = cpuProcessor.process(data);
            long duration = System.nanoTime() - start;
            metrics.recordCpuProcessing(duration);
            return new ProcessedImage(null, result, 0, duration, data.readableBytes());
        }).subscribeOn(Schedulers.parallel());
    }

    public void start() {
        if (!running) {
            running = true;
            dataStream.subscribe();
            logger.info("Solar data pipeline started");
        }
    }

    public void shutdown() {
        running = false;
        downloader.shutdown();
        gpuProcessor.shutdown();
        logger.info("Solar data pipeline stopped");
    }

    public Flux<ProcessedImage> getDataStream() {
        return dataStream;
    }

    public PerformanceMetrics getMetrics() {
        return metrics;
    }

    public long getFrameCount() {
        return frameCounter.get();
    }
}
