package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import lombok.Getter;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Implementation of {@link Brush}. This Brush is for {@link SwingRenderer Renderer} based on Swing.<br>
 * Since an instance of this class is created by the renderer being used
 * and managed together with its {@link com.snac.graphics.Canvas Canvas},
 * you normally don't need to create an instance yourself - unless you're writing your own renderer.
 */
@Getter
public class SwingBrush implements Brush<BufferedImage, Font> {
    protected final Graphics2D graphics;
    protected final Graphics2D original;
    protected float size = 1;

    /**
     * As said before: Since an instance of this class is created by the renderer being used
     * and managed together with its {@link com.snac.graphics.Canvas},
     * you normally don't need to create an instance yourself - unless you're writing your own renderer.
     *
     * @param graphics The graphics object the brush draws with
     */
    public SwingBrush(Graphics graphics) {
        this.graphics = (Graphics2D) graphics;
        this.original = (Graphics2D) graphics.create();
    }

    /**
     * See {@link Brush#setFont(Object)}
     */
    @Override
    public void setFont(Font font) {
        graphics.setFont(font);
    }

    /**
     * See {@link Brush#setColor(Color)}
     */
    @Override
    public void setColor(Color color) {
        graphics.setColor(color);
    }

    /**
     * See {@link Brush#drawRectangle(int, int, int, int, boolean)}
     */
    @Override
    public void drawRectangle(int x, int y, int width, int height, boolean filled) {
        if (filled) {
            graphics.fillRect(x, y, width, height);
        } else {
            graphics.drawRect(x, y, width, height);
        }
    }

    /**
     * See {@link Brush#drawImage(Object, int, int, int, int)} and {@link SkijaImageLoader}
     */
    @Override
    public void drawImage(BufferedImage image, int x, int y, int width, int height) {
        graphics.drawImage(image, x, y, width, height, null);
    }

    /**
     * See {@link Brush#drawArc(int, int, int, int, int, int, boolean)}
     */
    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled) {
        if (filled) {
            graphics.fillArc(x, y, width, height, startAngle, arcAngle);
        } else {
            graphics.drawArc(x, y, width, height, startAngle, arcAngle);
        }
    }

    /**
     * See {@link Brush#drawLine(int, int, int, int)}
     */
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        graphics.drawLine(x1, y1, x2, y2);
    }

    /**
     * See {@link Brush#drawPolygon(Point[], boolean)}
     */
    @Override
    public void drawPolygon(Point[] points, boolean filled) {
        int[] xPoints = new int[points.length];
        int[] yPoints = new int[points.length];

        for (int i = 0; i < points.length; i++) {
            xPoints[i] = points[i].x;
            yPoints[i] = points[i].y;
        }

        if (filled) {
            graphics.fillPolygon(xPoints, yPoints, points.length);
        } else {
            graphics.drawPolygon(xPoints, yPoints, points.length);
        }
    }

    /**
     * See {@link Brush#drawOval(int, int, int, int, boolean)}
     */
    @Override
    public void drawOval(int x, int y, int width, int height, boolean filled) {
        if (filled) {
            graphics.fillOval(x, y, width, height);
        } else  {
            graphics.drawOval(x, y, width, height);
        }
    }

    /**
     * See {@link Brush#drawRoundRect(int, int, int, int, int, int, boolean)}
     */
    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean filled) {
        if (filled) {
            graphics.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
        } else {
            graphics.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
        }
    }

    /**
     * See {@link Brush#drawText(String, int, int)}
     */
    @Override
    public void drawText(String text, int x, int y) {
        graphics.drawString(text, x, y);
    }

    /**
     * See {@link Brush#drawPixel(int, int)}
     */
    @Override
    public void drawPixel(int x, int y) {
        graphics.drawRect(x, y, 1, 1);
    }

    /**
     * See {@link Brush#drawPixel(Point)}
     */
    @Override
    public void drawPixel(Point location) {
        this.drawPixel(location.x, location.y);
    }

    /**
     * See {@link Brush#drawPixels(Point[])} )}
     */
    @Override
    public void drawPixels(Point[] locations) {
        for (var point : locations) {
            this.drawPixel(point);
        }
    }

    /**
     * See {@link Brush#drawPixels(Point[], int[])}
     */
    @Override
    public void drawPixels(Point[] locations, int[] colors) {
        for (int i = 0; i < locations.length; i++) {
            if (colors.length > i) {
                graphics.setColor(new Color(colors[i], true));
                drawPixel(locations[i]);
            } else {
                drawPixel(locations[i]);
            }
        }
    }

    /**
     * See {@link Brush#getSize()}
     */
    @Override
    public float getSize() {
        return size;
    }

    /**
     * See {@link Brush#setSize(float)}
     */
    @Override
    public void setSize(float size) {
        this.size = size;
        graphics.setStroke(new BasicStroke(size));
    }

    /**
     * See {@link Brush#reset()}
     */
    @Override
    public void reset() {
        graphics.setColor(Color.BLACK);
        graphics.setTransform(original.getTransform());
        graphics.setPaint(original.getPaint());
        graphics.setFont(original.getFont());
        graphics.setStroke(original.getStroke());
        graphics.setComposite(original.getComposite());
        graphics.setRenderingHints(original.getRenderingHints());
    }
}
