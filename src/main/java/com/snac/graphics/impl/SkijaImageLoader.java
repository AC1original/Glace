package com.snac.graphics.impl;

import com.snac.graphics.ImageLoader;
import de.snac.Ez2Log;
import io.github.humbleui.skija.*;

public class SkijaImageLoader extends ImageLoader<Image> {
    private static final Image FALLBACK_IMAGE;

    static {
        var surface = Surface.makeRaster(ImageInfo.makeN32Premul(2, 2));
        var canvas = surface.getCanvas();
        var paint = new Paint();

        paint.setAntiAlias(false);

        paint.setColor(new java.awt.Color(0, 0, 0).getRGB());
        canvas.drawPoint(0, 0, paint);
        canvas.drawPoint(1, 1, paint);

        paint.setColor(new java.awt.Color(255, 0, 255).getRGB());
        canvas.drawPoint(0, 1, paint);
        canvas.drawPoint(1, 0, paint);

        FALLBACK_IMAGE = surface.makeImageSnapshot();

        surface.close();
        canvas.close();
        paint.close();
    }

    @Override
    public Image load(String path) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                Ez2Log.warn(this, "Image not found in classpath: " + path + ". Returned fallback image.");
                return FALLBACK_IMAGE;
            }

            byte[] data = stream.readAllBytes();
            return Image.makeDeferredFromEncodedBytes(data);
        } catch (Exception e) {
            Ez2Log.warn(this, "Failed to load image from " + path + ". Returned fallback image.");
            return FALLBACK_IMAGE;
        }
    }

    @Override
    public Image getFallback() {
        return FALLBACK_IMAGE;
    }
}
