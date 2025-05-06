package com.snac.graphics.impl;

import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.main.Loop;
import de.snac.Ez2Log;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

@Getter
public class DefaultRenderer implements Renderer {
    private long window = 0;
    private Canvas canvas;
    private boolean vsync = false;
    private int maxFps = 60;
    private int fps = 0;

    public DefaultRenderer() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        Ez2Log.info(this, "Initialized");
    }

    @Override
    public void createWindow(int width, int height, @NotNull String title) {
        if (window != 0) {
            Ez2Log.warn(this, "Could not create window, only one window is allowed.");
            return;
        }

        this.window = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Unable to create window");
        }

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        GLFW.glfwSwapInterval(vsync ? 1 : 0);

        Ez2Log.info(this, "Created window");

        startRenderLoop();
    }

    private void startRenderLoop() {
        var loop = new Loop()
                .runOnThread(true)
                .setThreadName("Glace-Rendering");

        loop.start(maxFps, fps -> {
            this.fps = fps;

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            render();
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();

            if (GLFW.glfwWindowShouldClose(window)) {
                GLFW.glfwDestroyWindow(window);
                GLFW.glfwTerminate();

                loop.stop();

                Ez2Log.info(this, "LWJGL has been terminated. Render loop stopped");
            }
        });
    }

    @Override
    public void moveWindow(int x, int y) {
        if (window == 0) {
            Ez2Log.warn(this, "Could not move window, window is not initialized");
            return;
        }
        GLFW.glfwSetWindowPos(window, x, y);
    }

    @Override
    public void resizeWindow(int width, int height) {
        if (window == 0) {
            Ez2Log.warn(this, "Could not resize window, window is not initialized");
            return;
        }
        GLFW.glfwSetWindowSize(window, width, height);
    }

    @Override
    public void destroyWindow() {
        if (window == 0) {
            Ez2Log.warn(this, "Could not destroy window, window is not initialized");
            return;
        }
        GLFW.glfwSetWindowShouldClose(window, true);
        window = 0;
    }


    @Override
    public void render() {

    }

    @Override
    public void setVSync(boolean vsync) {
        this.vsync = vsync;
        GLFW.glfwSwapInterval(vsync ? 1 : 0);
    }

    @Override
    public boolean isVSync() {
        return vsync;
    }

    @Override
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    @Override
    @Nullable
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public int getMaxFPS() {
        return maxFps;
    }

    @Override
    public void setMaxFPS(int fps) {
        this.maxFps = fps;
    }

    @Override
    public int getFPS() {
        return fps;
    }
}
