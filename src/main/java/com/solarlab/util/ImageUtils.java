package com.solarlab.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Utility methods for image processing and conversion.
 */
public class ImageUtils {

    public static BufferedImage bytesToImage(byte[] imageData) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(imageData));
    }

    public static byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    public static BufferedImage createHeatMap(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage heatMap = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int gray = getGrayValue(rgb);
                Color heatColor = getHeatMapColor(gray);
                heatMap.setRGB(x, y, heatColor.getRGB());
            }
        }

        return heatMap;
    }

    private static int getGrayValue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3;
    }

    private static Color getHeatMapColor(int value) {
        // Map 0-255 to heat map colors (blue -> green -> yellow -> red)
        float ratio = value / 255.0f;
        
        if (ratio < 0.25f) {
            // Blue to Cyan
            float t = ratio * 4;
            return new Color(0, (int)(t * 255), 255);
        } else if (ratio < 0.5f) {
            // Cyan to Green
            float t = (ratio - 0.25f) * 4;
            return new Color(0, 255, (int)((1 - t) * 255));
        } else if (ratio < 0.75f) {
            // Green to Yellow
            float t = (ratio - 0.5f) * 4;
            return new Color((int)(t * 255), 255, 0);
        } else {
            // Yellow to Red
            float t = (ratio - 0.75f) * 4;
            return new Color(255, (int)((1 - t) * 255), 0);
        }
    }

    public static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, source.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }

    public static int calculateBrightness(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalBrightness = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                totalBrightness += getGrayValue(rgb);
            }
        }

        return (int) (totalBrightness / (width * height));
    }
}
