package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.util.Loop;
import de.snac.Ez2Log;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Implementation of {@link Renderer} based on Swing. See {@link Renderer}-Interface for more information.
 * <p>
 *     The easiest way to modify the rendering process is by using {@link #setPreRender(Runnable)}, {@link #setRenderLoopAction(BiConsumer)} or {@link #setPostRender(Runnable)}<br>
 *     Otherwise, you can just extend this class or write your own renderer with the {@link Renderer Renderer interface}
 * </p>
 */
@Getter
public class SwingRenderer extends JPanel implements Renderer {
    @Nullable protected JFrame frame;
    @Nullable protected BufferStrategy bufferStrategy;
    protected volatile Canvas canvas;
    protected volatile int maxFps;
    protected volatile int fps;
    protected final ExecutorService executor;
    protected final Loop loop;
    protected Brush<?, ?> brush;
    @Setter protected Runnable preRender;
    @Setter protected Runnable postRender;
    @Setter protected BiConsumer<Integer, Double> renderLoopAction;

    /**
     * Empty constructor. Creates a new SwingRenderer instance with default values
     */
    public SwingRenderer() {
        this(-1, null, Executors.newSingleThreadExecutor());
    }

    /**
     * Constructor... You know how those work - at least I hope so
     * @param maxFPS The maximum FPS the renderer should render on
     * @param canvas Sets the {@link Canvas}. By setting this to {@code null} a new {@link Canvas} will be created
     * @param executor The executor this renderer should run on.
     *                 By setting this to {@code null} this renderer will use the thread the window is created on for the render-loop,
     *                 which is not recommended as this will block the entire thread.
     */
    public SwingRenderer(int maxFPS, @Nullable Canvas canvas, @Nullable ExecutorService executor) {
        this.canvas = canvas == null ? new Canvas() : canvas;
        this.maxFps = maxFPS <= 0 ? 60 : maxFPS;
        this.executor = executor;

        this.loop = Loop.builder()
                .runOnThread(executor == null)
                .threadName("Swing-Rendering")
                .build();

        preRender = () -> {};
        postRender = () -> Ez2Log.info(this, "Shutting down render loop");
        renderLoopAction = (fps, deltaTime) -> {
          this.fps = fps;
          render();
        };

        Ez2Log.info(this, "Initialized");
    }

    /**
     * Will automatically start the render-loop.
     * <p>See {@link Renderer#createWindow(int, int, String)} for more information</p>
     */
    @Override
    public void createWindow(int width, int height, String title) {
        if (this.frame != null) {
            Ez2Log.warn(this, "Could not create window, only one window per renderer is allowed.");
            return;
        }
        this.frame = new JFrame();

        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        frame.setVisible(true);
        frame.createBufferStrategy(2);

        this.bufferStrategy = frame.getBufferStrategy();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loop.stop();
                Ez2Log.info(SwingRenderer.class, "JFrame has been terminated");
            }
        });

        brush = new SwingBrush(bufferStrategy.getDrawGraphics());

        startRenderLoop();
    }

    protected void startRenderLoop() {
        var exec = Executors.newSingleThreadExecutor();
        if (!loop.isRunOnThread()) {
            exec.execute(() -> {
                loop.start(preRender, maxFps, renderLoopAction, postRender);
            });
        } else {
            loop.start(preRender, maxFps, renderLoopAction, postRender);
        }
    }

    /**
     * See {@link Renderer#moveWindow(int, int)}
     */
    @Override
    public void moveWindow(int x, int y) {
        if (frame == null) return;

        frame.setLocation(x, y);
    }

    /**
     * See {@link Renderer#resizeWindow(int, int)}
     */
    @Override
    public void resizeWindow(int width, int height) {
        if (frame == null) return;

        frame.setSize(width, height);
    }

    /**
     * See {@link Renderer#destroyWindow()}
     */
    @Override
    public void destroyWindow() {
        if (frame == null) return;

        frame.dispose();
        frame.setVisible(false);
        this.frame = null;

        Ez2Log.info(this, "Destroyed JFrame");
    }

    /**
     * See {@link Renderer#setCanvas(Canvas)}
     */
    @Override
    public void setCanvas(Canvas canvas) {
        this.canvas = canvas;
    }

    /**
     * See {@link Renderer#getCanvas()}
     */
    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * See {@link Renderer#getMaxFPS()}
     */
    @Override
    public int getMaxFPS() {
        return maxFps;
    }

    /**
     * See {@link Renderer#setMaxFPS(int)}
     */
    @Override
    public void setMaxFPS(int fps) {
        this.maxFps = fps;
    }

    /**
     * See {@link Renderer#getFPS()}
     */
    @Override
    public int getFPS() {
        return fps;
    }

    /**
     * See {@link Renderer#render()}
     */
    @Override
    public synchronized void render() {
        if (getFrame() == null || getCanvas() == null || getBufferStrategy() == null) {
            return;
        }
        do {
            do {
                var graphics = getBufferStrategy().getDrawGraphics();
                getCanvas().render(brush);
                graphics.dispose();

            } while (getBufferStrategy().contentsRestored());
            getBufferStrategy().show();

        } while (getBufferStrategy().contentsLost());
    }
}
