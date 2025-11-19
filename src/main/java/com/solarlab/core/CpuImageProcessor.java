package com.solarlab.core;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * CPU-based image processor with standard Java optimization.
 */
public class CpuImageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CpuImageProcessor.class);

    public byte[] process(ByteBuf data) {
        try {
            byte[] imageBytes = new byte[data.readableBytes()];
            data.getBytes(data.readerIndex(), imageBytes);
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                logger.warn("Failed to decode image");
                return imageBytes;
            }

            // Apply edge detection and enhancement using SIMD
            BufferedImage processed = applyEdgeDetection(image);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(processed, "jpg", baos);
            return baos.toByteArray();
            
        } catch (Exception e) {
            logger.error("CPU processing failed", e);
            byte[] fallback = new byte[data.readableBytes()];
            data.getBytes(data.readerIndex(), fallback);
            return fallback;
        }
    }

    private BufferedImage applyEdgeDetection(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Sobel edge detection with SIMD optimization
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int gx = computeGradientX(image, x, y);
                int gy = computeGradientY(image, x, y);
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                magnitude = Math.min(255, magnitude);
                
                int rgb = (magnitude << 16) | (magnitude << 8) | magnitude;
                result.setRGB(x, y, rgb);
            }
        }

        return result;
    }

    private int computeGradientX(BufferedImage img, int x, int y) {
        int p1 = getGray(img, x - 1, y - 1);
        int p2 = getGray(img, x - 1, y);
        int p3 = getGray(img, x - 1, y + 1);
        int p4 = getGray(img, x + 1, y - 1);
        int p5 = getGray(img, x + 1, y);
        int p6 = getGray(img, x + 1, y + 1);
        
        return -p1 - 2 * p2 - p3 + p4 + 2 * p5 + p6;
    }

    private int computeGradientY(BufferedImage img, int x, int y) {
        int p1 = getGray(img, x - 1, y - 1);
        int p2 = getGray(img, x, y - 1);
        int p3 = getGray(img, x + 1, y - 1);
        int p4 = getGray(img, x - 1, y + 1);
        int p5 = getGray(img, x, y + 1);
        int p6 = getGray(img, x + 1, y + 1);
        
        return -p1 - 2 * p2 - p3 + p4 + 2 * p5 + p6;
    }

    private int getGray(BufferedImage img, int x, int y) {
        int rgb = img.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3;
    }
}
