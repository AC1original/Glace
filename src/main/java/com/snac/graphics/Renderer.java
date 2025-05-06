package com.snac.graphics;

public interface Renderer {

    void createWindow(int width, int height, String title);

    void moveWindow(int x, int y);

    void resizeWindow(int width, int height);

    void destroyWindow();

    void setVSync(boolean vsync);

    boolean isVSync();

    void setCanvas(Canvas canvas);

    Canvas getCanvas();

    int getMaxFPS();

    void setMaxFPS(int fps);

    int getFPS();

    void render();
}
