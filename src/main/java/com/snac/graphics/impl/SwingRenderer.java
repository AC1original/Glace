package com.snac.graphics.impl;

import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.util.Loop;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Implementation of {@link Renderer} based on Swing. See {@link Renderer}-Interface for more information.
 * <p>
 * The easiest way to modify the rendering process is by using {@link #setPreRender(Runnable)}, {@link #setRenderLoopAction(BiConsumer)} or {@link #setPostRender(Runnable)}<br>
 * Otherwise, you can extend this class or write your own renderer with the {@link Renderer Renderer interface}
 * </p>
 */
@Getter
@Slf4j
public class SwingRenderer extends JPanel implements Renderer<BufferedImage> {
    @Nullable
    protected JFrame frame;
    @Nullable
    protected BufferStrategy bufferStrategy;
    @Setter
    protected volatile Canvas<BufferedImage> canvas;
    protected volatile int maxFps;
    protected volatile int fps;
    protected final ExecutorService executor;
    protected final Loop loop;
    protected SwingBrush brush;
    @Setter
    protected Runnable preRender;
    @Setter
    protected Runnable postRender;
    @Setter
    protected BiConsumer<Integer, Double> renderLoopAction;

    /**
     * Empty constructor. Creates a new SwingRenderer instance with default values
     */
    public SwingRenderer() {
        this(-1, null, Executors.newSingleThreadExecutor());
    }

    /**
     * Constructor... You know how those work - at least I hope so
     *
     * @param maxFPS   The maximum FPS the renderer should render on
     * @param canvas   Sets the {@link Canvas}. By setting this to {@code null} a new {@link Canvas} will be created
     * @param executor The executor this renderer should run on.
     *                 By setting this to {@code null} this renderer will use the thread the window is created on for the render-loop,
     *                 which is not recommended as this will block the entire thread.
     */
    public SwingRenderer(int maxFPS, @Nullable Canvas<BufferedImage> canvas, @Nullable ExecutorService executor) {
        this.canvas = canvas == null ? new Canvas<>() : canvas;
        this.maxFps = maxFPS <= 0 ? 60 : maxFPS;
        this.executor = executor;

        this.loop = Loop.builder()
                .runOnThread(executor == null)
                .threadName("Swing-Rendering")
                .build();

        preRender = () -> {
        };
        postRender = () -> log.info("Shutting down render loop");
        renderLoopAction = (fps, deltaTime) -> {
            this.fps = fps;
            render();
        };

        log.info("Initialized");
    }

    /**
     * Will automatically start the render-loop.
     * <p>See {@link Renderer#createWindow(int, int, String)} for more information</p>
     */
    @Override
    public void createWindow(int width, int height, String title) {
        if (this.frame != null) {
            log.warn("Could not create window, only one window per renderer is allowed.");
            return;
        }

        System.setProperty("sun.java2d.opengl", "true");

        this.frame = new JFrame();

        frame.setTitle(title);
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.add(this);
        frame.setVisible(true);
        frame.validate();
        frame.requestFocus();

        SwingUtilities.invokeLater(() -> {
            frame.createBufferStrategy(2);

            this.bufferStrategy = frame.getBufferStrategy();
            this.setDoubleBuffered(false);

            brush = new SwingBrush(this, bufferStrategy.getDrawGraphics());

            startRenderLoop();
        });


        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loop.stop();
                log.info("JFrame has been terminated");
            }
        });
    }

    protected void startRenderLoop() {
        if (!loop.isRunOnThread()) {
            executor.execute(() -> {
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

        log.info("Destroyed JFrame");
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

    @Override
    public double getDeltaTime() {
        return loop == null ? 0 : loop.getDeltaTime();
    }

    /**
     * See {@link Renderer#render()}
     */
    @Override
    public void render() {
        if (getFrame() == null
                || getFrame().getState() == JFrame.ICONIFIED
                || getCanvas() == null) {
            return;
        }

        if (getBufferStrategy() == null) {
            if (getFrame().isDisplayable() && getFrame().getWidth() > 0 && getFrame().getHeight() > 0) {
                getFrame().createBufferStrategy(2);
                bufferStrategy = getFrame().getBufferStrategy();
            } else {
                return;
            }
        }

        if (!bufferStrategy.contentsLost() && bufferStrategy.getDrawGraphics() != null) {
            do {
                Graphics2D g = null;
                try {
                    g = (Graphics2D) bufferStrategy.getDrawGraphics();
                    g.clearRect(0, 0, getWidth(), getHeight());

                    brush.setGraphics(g);
                    getCanvas().render(brush);

                } finally {
                    if (g != null) g.dispose();
                }

                Toolkit.getDefaultToolkit().sync();
                bufferStrategy.show();

            } while (bufferStrategy.contentsLost());
        }
    }

    /**
     * See {@link Renderer#getWindowWidth()}
     */
    @Override
    public int getWindowWidth() {
        return frame == null ? Integer.MAX_VALUE : frame.getWidth();
    }

    /**
     * See {@link Renderer#getWindowHeight()}
     */
    @Override
    public int getWindowHeight() {
        return frame == null ? Integer.MAX_VALUE : frame.getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        //Keep empty to prevent flickering
    }
}