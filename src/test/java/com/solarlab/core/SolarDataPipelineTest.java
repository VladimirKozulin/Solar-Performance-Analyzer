package com.solarlab.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SolarDataPipeline.
 */
class SolarDataPipelineTest {
    
    private SolarDataPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new SolarDataPipeline();
    }

    @AfterEach
    void tearDown() {
        if (pipeline != null) {
            pipeline.shutdown();
        }
    }

    @Test
    void testPipelineInitialization() {
        assertNotNull(pipeline);
        assertNotNull(pipeline.getMetrics());
        assertEquals(0, pipeline.getFrameCount());
    }

    @Test
    void testDataStreamCreation() {
        assertNotNull(pipeline.getDataStream());
    }

    @Test
    void testDataStreamEmitsData() {
        pipeline.start();
        
        StepVerifier.create(pipeline.getDataStream().take(1))
            .expectNextMatches(image -> 
                image != null && 
                (image.getGpuData() != null || image.getCpuData() != null)
            )
            .expectComplete()
            .verify(Duration.ofSeconds(30));
    }

    @Test
    void testMetricsCollection() {
        pipeline.start();
        
        StepVerifier.create(pipeline.getDataStream().take(2))
            .expectNextCount(2)
            .expectComplete()
            .verify(Duration.ofSeconds(30));
        
        assertTrue(pipeline.getFrameCount() >= 2);
        assertTrue(pipeline.getMetrics().getTotalFrames() >= 2);
    }
}
