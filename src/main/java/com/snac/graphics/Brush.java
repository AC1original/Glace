package com.snac.graphics;

public interface Brush<I> {

    void drawRectangle(int x, int y, int width, int height);

    void drawImage(I image, int x, int y, int width, int height);
}
