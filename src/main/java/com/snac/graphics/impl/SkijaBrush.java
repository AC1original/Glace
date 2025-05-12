package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import io.github.humbleui.skija.Canvas;

public record SkijaBrush(Canvas skijaCanvas, long window) implements Brush<Integer> {
    @Override
    public void drawRectangle(int x, int y, int width, int height) {
    }

    @Override
    public void drawImage(Integer image, int x, int y, int width, int height) {

    }
}
