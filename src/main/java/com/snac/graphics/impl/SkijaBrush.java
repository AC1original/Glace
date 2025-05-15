package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;
import lombok.Getter;

import java.awt.*;
import java.awt.Color;

@Getter
public class SkijaBrush implements Brush<Image, Font> {
    private final Canvas skijaCanvas;
    private final long window;
    private final Paint paint;
    private Font font = new Font(Typeface.makeDefault(), 12);
    private final Rect iRect = new Rect(0, 0, 0, 0);
    private float lineStrength = 1f;
    private Color color = Color.BLACK;

    public SkijaBrush(Canvas skijaCanvas, long window) {
        this.skijaCanvas = skijaCanvas;
        this.window = window;
        this.paint = new Paint();
    }

    @Override
    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public void setColor(Color color) {
        this.paint.setColor(color.getRGB());
        this.color = color;
    }

    @Override
    public void drawRectangle(int x, int y, int width, int height, boolean filled) {
        paint.setStroke(!filled);
        var rect = iRect.withLeft(x).withTop(y).withRight(x + width).withBottom(y + height);
        skijaCanvas.drawRect(rect, paint);
    }

    @Override
    public void drawImage(Image image, int x, int y, int width, int height) {
        skijaCanvas.drawImageRect(image, Rect.makeXYWH(x, y, width, height));
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled) {
        drawArc(x, y, width, height, startAngle, arcAngle, filled, false);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled, boolean includeCenter) {
        paint.setStroke(!filled);
        skijaCanvas.drawArc(x, y, x + width, y +height, startAngle, arcAngle, includeCenter, paint);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        skijaCanvas.drawLine(x1, y1, x2, y2, paint);
    }

    @Override
    public void drawPolygon(Point[] points,  boolean filled) {
        paint.setStroke(!filled);

        var path = new Path();
        if (points.length > 0) {
            path.moveTo(points[0].x, points[0].y);
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].x, points[i].y);
            }
            path.closePath();
        }

        skijaCanvas.drawPath(path, paint);
    }

    @Override
    public void drawOval(int x, int y, int width, int height, boolean filled) {
        paint.setStroke(!filled);
        skijaCanvas.drawOval(Rect.makeXYWH(x, y, width, height), paint);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean filled) {
        paint.setStroke(!filled);
        skijaCanvas.drawRRect(RRect.makeXYWH(x, y, width, height, arcWidth, arcHeight), paint);
    }

    @Override
    public void drawText(String text, int x, int y) {
        short[] glyphs = font.getStringGlyphs(text);

        float[] xpos = new float[glyphs.length];
        float currentX = x;
        for (int i = 0; i < glyphs.length; i++) {
            xpos[i] = currentX;
            currentX += font.getWidths(new short[]{glyphs[i]})[0];
        }

        var blob = TextBlob.makeFromPosH(glyphs, xpos, y, font);

        skijaCanvas.drawTextBlob(blob, 0, 0, paint);
    }

    @Override
    public void drawPixel(int x, int y) {
        paint.setAntiAlias(false);
        skijaCanvas.drawPoint(x, y, paint);
        paint.setAntiAlias(true);
    }

    @Override
    public void drawPixel(Point location) {
        drawPixel(location.x, location.y);
    }

    @Override
    public void drawPixels(Point[] locations) {
        for (var loc : locations) {
            drawPixel(loc);
        }
    }

    @Override
    public void drawPixels(Point[] locations, int[] colors) {
        for (int i = 0; i < locations.length; i++) {
            if (colors.length > i) {
                paint.setColor(colors[i]);
                drawPixel(locations[i]);
            } else {
                paint.setColor(getColor().getRGB());
                drawPixel(locations[i]);
            }
        }
    }

    public void setLineStrength(float strength) {
        this.lineStrength = strength;
        paint.setStrokeWidth(lineStrength);
    }
}
