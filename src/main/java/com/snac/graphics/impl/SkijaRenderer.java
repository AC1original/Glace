package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.util.Loop;
import de.snac.Ez2Log;
import io.github.humbleui.skija.*;
import io.github.humbleui.skija.impl.Stats;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.concurrent.ExecutorService;

@Getter
public class SkijaRenderer implements Renderer {
    private long window = 0;
    private volatile Canvas canvas;
    private volatile boolean vsync = false;
    private volatile int maxFps;
    private volatile int fps = 0;
    private int width;
    private int height;
    private float dpi = 1f;
    private Brush<?, ?> brush;
    private DirectContext context;
    private BackendRenderTarget renderTarget;
    private Surface surface;
    private io.github.humbleui.skija.Canvas skijaCanvas;
    @Nullable
    private final ExecutorService executor;

    public SkijaRenderer() {
        this(0, null, null);
    }

    public SkijaRenderer(int maxFPS, @Nullable Canvas canvas, @Nullable ExecutorService executor) {
        GLFWErrorCallback.createPrint(System.err).set();

        this.canvas = canvas == null ? new Canvas.DefaultCanvas() : canvas;
        this.maxFps = maxFPS <= 0 ? 60 : maxFPS;
        this.executor = executor;

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        Ez2Log.info(this, "Initialized");
    }

    private void updateDimensions() {
        int[] width = new int[1];
        int[] height = new int[1];
        glfwGetFramebufferSize(window, width, height);

        float[] xScale = new float[1];
        float[] yScale = new float[1];
        glfwGetWindowContentScale(window, xScale, yScale);
        assert xScale[0] == yScale[0] : "Horizontal dpi=" + xScale[0] + ", vertical dpi=" + yScale[0];

        this.width = (int) (width[0] / xScale[0]);
        this.height = (int) (height[0] / yScale[0]);
        this.dpi = xScale[0];
    }

    @Override
    public void createWindow(int width, int height, @NotNull String title) {
        if (window != 0) {
            Ez2Log.warn(this, "Could not create window, only one window per renderer is allowed.");
            return;
        }

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        this.window = glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Unable to create window");
        }

        updateDimensions();
        Ez2Log.info(this, "Created window. Starting render loop");

        startRenderLoop();
    }

    private synchronized void startRenderLoop() {
        var loop = Loop.builder()
                .runOnThread(executor == null)
                .threadName("Glace-Rendering")
                .specificExecutor(executor)
                .build();

        loop.start(() -> {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            glfwSwapInterval(vsync ? 1 : 0);
            glfwShowWindow(window);

            context = DirectContext.makeGL();
            glfwSetWindowSizeCallback(window, (window, width, height) -> {
                updateDimensions();
                initSkija();
                render();
            });
            Ez2Log.info(this, "Initialized Skija");
            initSkija();

        }, maxFps, fps -> {
            this.fps = fps;

            render();
            glfwPollEvents();

            if (glfwWindowShouldClose(window)) {
                loop.stop();
                Ez2Log.info(this, "LWJGL has been terminated");
            }
        }, () -> {
            glfwFreeCallbacks(window);
            glfwDestroyWindow(window);
            glfwTerminate();
            GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
            if (errorCallback != null) errorCallback.free();

            if (context != null) {
                context.close();
            }

            Ez2Log.info(this, "Shutting down render loop");
        });
    }

    private void initSkija() {
        Stats.enabled = true;

        if (surface != null) {
            surface.close();
        }
        if (renderTarget != null) {
            renderTarget.close();
        }

        renderTarget = BackendRenderTarget.makeGL(
                (int) (width * dpi),
                (int) (height * dpi),
                /*samples*/0,
                /*stencil*/8,
                /*fbId*/0,
                FramebufferFormat.GR_GL_RGBA8);

        surface = Surface.makeFromBackendRenderTarget(
                context,
                renderTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.getDisplayP3(),
                new SurfaceProps(PixelGeometry.RGB_H));

        skijaCanvas = surface.getCanvas();
        brush = new SkijaBrush(skijaCanvas, window);
    }

    @Override
    public void moveWindow(int x, int y) {
        glfwSetWindowPos(window, x, y);
    }

    @Override
    public void resizeWindow(int width, int height) {
        glfwSetWindowSize(window, width, height);
    }

    @Override
    public void destroyWindow() {
        if (window == 0) {
            Ez2Log.warn(this, "Could not destroy window, window is not initialized");
            return;
        }
        glfwSetWindowShouldClose(window, true);
        window = 0;
    }


    @Override
    public void render() {
        if (getCanvas() == null) return;
        skijaCanvas.clear(0xFFFFFFFF);
        getCanvas().render(brush);
        surface.flushAndSubmit();
        glfwSwapBuffers(window);
    }

    @Override
    public void setVSync(boolean vsync) {
        this.vsync = vsync;
        glfwSwapInterval(vsync ? 1 : 0);
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
