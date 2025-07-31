package com.snac.graphics.impl;

import com.snac.graphics.ImageLoader;
import de.snac.Ez2Log;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;

/**
 * Image loader to load {@link BufferedImage Images} for {@link SwingRenderer} (or Swing in general).
 * Extends from {@link ImageLoader}
 */
public class SwingImageLoader extends ImageLoader<BufferedImage> {
    private static final BufferedImage FALLBACK_IMAGE;

    static {
        FALLBACK_IMAGE = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);

        FALLBACK_IMAGE.setRGB(0, 0, Color.PINK.getRGB());
        FALLBACK_IMAGE.setRGB(0, 1, Color.PINK.getRGB());
        FALLBACK_IMAGE.setRGB(1, 0, Color.BLACK.getRGB());
        FALLBACK_IMAGE.setRGB(1, 1, Color.BLACK.getRGB());
    }

    /**
     * Loads an image from the given path.
     *
     * @param path The path to the image resource
     * @return The loaded image
     */
    @Override
    public BufferedImage load(String path) {
        BufferedImage image;
        try(var stream = ImageLoader.class.getResourceAsStream(path)) {
            if (stream == null) throw new FileNotFoundException("Ressource not found:" + path);

            image = ImageIO.read(stream);
            Ez2Log.info(ImageLoader.class, "Loaded image from: " + path);
        } catch (Exception e) {
            Ez2Log.warn(ImageLoader.class, "Failed to load image from: '%s' | '%s'. Returned fallback image", path, e);
        }

        return null;
    }

    /**
     * Returns a fallback image to use when loading fails or no cached version is available.
     *
     * @return The fallback image
     */
    @Override
    public BufferedImage getFallback() {
        return FALLBACK_IMAGE;
    }
}
