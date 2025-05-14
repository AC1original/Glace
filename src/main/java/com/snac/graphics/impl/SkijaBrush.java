package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Typeface;
import io.github.humbleui.types.Rect;
import lombok.Getter;

import java.awt.*;

@Getter
public class SkijaBrush implements Brush<Integer, Font> {
    private final Canvas skijaCanvas;
    private final long window;
    private final io.github.humbleui.skija.Paint paint;
    private Font font = new Font(Typeface.makeDefault(), 12);

    public SkijaBrush(Canvas skijaCanvas, long window) {
        this.skijaCanvas = skijaCanvas;
        this.window = window;
        this.paint = new io.github.humbleui.skija.Paint();
    }

    @Override
    public void setFont(Font font) {
        this.font = font;
    }

    @Override
    public void setColor(Color color) {
        this.paint.setColor(color.getRGB());
    }

    @Override
    public void drawRectangle(int x, int y, int width, int height, boolean filled) {
        skijaCanvas.drawRect(new Rect(x, y, width, height), paint);
    }

    @Override
    public void drawImage(Integer image, int x, int y, int width, int height) {

    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled) {

    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {

    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints, boolean filled) {

    }

    @Override
    public void drawOval(int x, int y, int width, int height, boolean filled) {

    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean filled) {

    }

    @Override
    public void drawText(String text, int x, int y) {

    }

    @Override
    public void drawPixel(int x, int y) {

    }

    @Override
    public void drawPixel(Point location) {

    }

    @Override
    public void drawPixels(Point[] locations) {

    }

    @Override
    public void drawPixels(Point[] locations, int[] colors) {

    }
}
