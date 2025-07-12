package com.snac.graphics;

/**
 * This interface provides the structure for a Renderer. <br>
 * A Renderer is meant to only handle one window.
 * If you want to have more windows, you need to have more Renderer(-Instances)
 * <p>Use this interface if you want to create your own Glace-renderer.</p>
 * <p>
 *     Short explanation how rendering works here:<br>
 *     {@link Renderer} renders the current set {@link Canvas}.
 *     This Canvas renders every {@link Renderable} it contains.
 * </p>
 */
public interface Renderer {

    /**
     * Creates a new window.
     * @param width The window width
     * @param height The window height
     * @param title The windows title
     */
    void createWindow(int width, int height, String title);

    /**
     * Move the window to a specific position
     * @param x The new window X-Position
     * @param y The new window Y-Position
     */
    void moveWindow(int x, int y);

    /**
     * Resize the window.
     * @param width The new window width
     * @param height The new window height
     */
    void resizeWindow(int width, int height);

    /**
     * Destroy the current window
     */
    void destroyWindow();

    /**
     * Set a new {@link Canvas}
     * @param canvas The new canvas
     */
    void setCanvas(Canvas canvas);

    /**
     * @return Current canvas the renderer uses
     */
    Canvas getCanvas();

    /**
     * @return Current maximum FPS the renderer is running on
     */
    int getMaxFPS();

    /**
     * Sets the maximum fps. This value must be higher than 0
     * @param fps The new maximum fps
     */
    void setMaxFPS(int fps);

    /**
     * @return The current FPS
     */
    int getFPS();

    /**
     * This method renders the current Canvas
     */
    void render();
}
