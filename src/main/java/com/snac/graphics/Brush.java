package com.snac.graphics;

import java.awt.*;

public interface Brush<I, F> {

    void setFont(F font);

    void setColor(Color color);

    void drawRectangle(int x, int y, int width, int height, boolean filled);

    void drawImage(I image, int x, int y, int width, int height);

    void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle, boolean filled);

    void drawLine(int x1, int y1, int x2, int y2);

    void drawPolygon(Point[] points, boolean filled);

    void drawOval(int x, int y, int width, int height, boolean filled);

    void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, boolean filled);

    void drawText(String text, int x, int y);

    void drawPixel(int x, int y);
    void drawPixel(Point location);

    void drawPixels(Point[] locations);
    void drawPixels(Point[] locations, int[] colors);

    float getSize();
    void setSize(float size);
}
