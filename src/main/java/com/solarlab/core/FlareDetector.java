package com.solarlab.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Solar flare detection using brightness threshold analysis.
 */
public class FlareDetector {
    private static final Logger logger = LoggerFactory.getLogger(FlareDetector.class);
    private static final int BRIGHTNESS_THRESHOLD = 200;
    private static final int MIN_FLARE_SIZE = 100; // pixels

    public static class FlareEvent {
        public final int x;
        public final int y;
        public final int size;
        public final int intensity;

        public FlareEvent(int x, int y, int size, int intensity) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.intensity = intensity;
        }

        @Override
        public String toString() {
            return String.format("Flare at (%d,%d) size=%d intensity=%d", 
                x, y, size, intensity);
        }
    }

    public List<FlareEvent> detectFlares(byte[] imageData) {
        List<FlareEvent> flares = new ArrayList<>();
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                return flares;
            }

            int width = image.getWidth();
            int height = image.getHeight();
            boolean[][] visited = new boolean[width][height];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!visited[x][y] && isBright(image, x, y)) {
                        FlareEvent flare = analyzeRegion(image, x, y, visited);
                        if (flare != null && flare.size >= MIN_FLARE_SIZE) {
                            flares.add(flare);
                        }
                    }
                }
            }

            if (!flares.isEmpty()) {
                logger.info("Detected {} solar flares", flares.size());
            }

        } catch (Exception e) {
            logger.error("Flare detection failed", e);
        }

        return flares;
    }

    private boolean isBright(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int brightness = (r + g + b) / 3;
        return brightness > BRIGHTNESS_THRESHOLD;
    }

    private FlareEvent analyzeRegion(BufferedImage image, int startX, int startY, 
                                     boolean[][] visited) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        List<int[]> queue = new ArrayList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;
        
        int size = 0;
        int totalIntensity = 0;
        int centerX = 0;
        int centerY = 0;

        while (!queue.isEmpty()) {
            int[] pos = queue.remove(0);
            int x = pos[0];
            int y = pos[1];
            
            size++;
            int rgb = image.getRGB(x, y);
            int intensity = ((rgb >> 16) & 0xFF + (rgb >> 8) & 0xFF + (rgb & 0xFF)) / 3;
            totalIntensity += intensity;
            centerX += x;
            centerY += y;

            // Check neighbors
            int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height && 
                    !visited[nx][ny] && isBright(image, nx, ny)) {
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        if (size > 0) {
            return new FlareEvent(
                centerX / size, 
                centerY / size, 
                size, 
                totalIntensity / size
            );
        }

        return null;
    }
}
