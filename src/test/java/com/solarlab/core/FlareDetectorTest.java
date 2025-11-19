package com.solarlab.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlareDetector.
 */
class FlareDetectorTest {
    
    private FlareDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FlareDetector();
    }

    @Test
    void testDetectNoFlares() throws Exception {
        BufferedImage darkImage = createTestImage(512, 512, Color.BLACK);
        byte[] imageData = imageToBytes(darkImage);
        
        List<FlareDetector.FlareEvent> flares = detector.detectFlares(imageData);
        
        assertTrue(flares.isEmpty());
    }

    @Test
    void testDetectSingleFlare() throws Exception {
        BufferedImage image = createTestImage(512, 512, Color.BLACK);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillOval(200, 200, 150, 150);
        g.dispose();
        
        byte[] imageData = imageToBytes(image);
        List<FlareDetector.FlareEvent> flares = detector.detectFlares(imageData);
        
        assertFalse(flares.isEmpty());
        assertTrue(flares.size() >= 1);
    }

    @Test
    void testDetectMultipleFlares() throws Exception {
        BufferedImage image = createTestImage(512, 512, Color.BLACK);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillOval(100, 100, 120, 120);
        g.fillOval(350, 350, 120, 120);
        g.dispose();
        
        byte[] imageData = imageToBytes(image);
        List<FlareDetector.FlareEvent> flares = detector.detectFlares(imageData);
        
        assertTrue(flares.size() >= 2);
    }

    @Test
    void testFlareEventProperties() throws Exception {
        BufferedImage image = createTestImage(512, 512, Color.BLACK);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillOval(200, 200, 150, 150);
        g.dispose();
        
        byte[] imageData = imageToBytes(image);
        List<FlareDetector.FlareEvent> flares = detector.detectFlares(imageData);
        
        if (!flares.isEmpty()) {
            FlareDetector.FlareEvent flare = flares.get(0);
            assertTrue(flare.x > 0);
            assertTrue(flare.y > 0);
            assertTrue(flare.size > 0);
            assertTrue(flare.intensity > 0);
        }
    }

    private BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private byte[] imageToBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
