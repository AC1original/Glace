package com.snac.graphics.impl;

import com.snac.graphics.Brush;
import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.util.Loop;
import de.snac.Ez2Log;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Getter
public class SwingRenderer extends JPanel implements Renderer {
    @Nullable private JFrame frame;
    @Nullable private BufferStrategy bufferStrategy;
    private volatile Canvas canvas;
    private volatile int maxFps;
    private volatile int fps;
    private final ExecutorService executor;
    private final Loop loop;
    private Brush<?, ?> brush;

    public SwingRenderer() {
        this(-1, null, Executors.newSingleThreadExecutor());
    }

    public SwingRenderer(int maxFPS, @Nullable Canvas canvas, @Nullable ExecutorService executor) {
        this.canvas = canvas == null ? new Canvas() : canvas;
        this.maxFps = maxFPS <= 0 ? 60 : maxFPS;
        this.executor = executor;

        this.loop = Loop.builder()
                .runOnThread(executor == null)
                .threadName("Swing-Rendering")
                .build();

        Ez2Log.info(this, "Initialized");
    }

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

    private void startRenderLoop() {
        var renderRun = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                fps = integer;
                render();
            }
        };

        var shutdownRun = new Runnable() {
            @Override
            public void run() {
                Ez2Log.info(this, "Shutting down render loop");
            }
        };

        var exec = Executors.newSingleThreadExecutor();
        if (!loop.isRunOnThread()) {
            exec.execute(() -> {
                loop.start(() -> {}, maxFps, renderRun, shutdownRun);
            });
        } else {
            loop.start(() -> {}, maxFps, renderRun, shutdownRun);
        }
    }

    @Override
    public void moveWindow(int x, int y) {
        if (frame == null) return;

        frame.setLocation(x, y);
    }

    @Override
    public void resizeWindow(int width, int height) {
        if (frame == null) return;

        frame.setSize(width, height);
    }

    @Override
    public void destroyWindow() {
        if (frame == null) return;

        frame.dispose();
        frame.setVisible(false);
        this.frame = null;

        Ez2Log.info(this, "Destroyed JFrame");
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
