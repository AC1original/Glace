package com.snac.util;

import de.snac.Ez2Log;
import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * This class is used to create (Game-)Loops with a specific tps frequency
 */
@Getter
public class Loop {
    private boolean running = false;
    private boolean paused = false;
    private final boolean runOnThread;
    private final String threadName;
    private ExecutorService executorService;

    /**
     * There are two ways to create a new Loop instance:
     * <br>1. Use this contractor
     * <br>2. Use the builder generated from lombok (thank you lombok <3) For example:
     * <pre>{@code
     * var loop = Loop.builder().runOnThread(true).build();
     * }</pre>
     *
     * @param runOnThread If set to {@code true} this Loop will use its own thread.
     *                    Otherwise, the thread started on (Not recommended as it will block the entire thread)
     * @param threadName Sets the name for the generated thread. This only makes sense if runOnThread-parameter is set to {@code true}
     */
    @Builder
    public Loop(boolean runOnThread, @Nullable String threadName) {
        this.runOnThread = runOnThread;
        this.threadName = threadName == null ? "" : threadName;
    }

    /**
     * Shorter version of {@link #start(Runnable, int, Consumer, Runnable)}
     * @param TARGET_TPS The maximum tps this loop should tick. If you're confused or something just type 60 but NEVER 0 or lower
     * @param action This consumer gets called every tick with the current fps
     */
    public void start(final int TARGET_TPS, Consumer<Integer> action) {
        start(() -> {}, TARGET_TPS, action, () -> {});
    }

    /**
     * Everything ready? To start the loop, you'll need to call this method.
     * <br>Example how to use:
     * <pre>{@code
     * var loop = Loop.builder().runOnThread(true).build();
     *
     * loop.start(() -> System.out.println("This gets called before loop"),
     *          60,
     *          (currentFPS) -> render(),
     *          () -> System.out.println("This gets called after loop"));
     * }</pre>
     * @param preRun This runnable gets called before the loop starts.
     *               Used to run something on the same thread as this loop.
     *               IDK if this is useful.
     * @param TARGET_TPS The maximum tps this loop should tick. If you're confused or something just type 60 but NEVER 0 or lower
     * @param action This consumer gets called every tick with the current fps
     * @param shutdownHook This runnable gets called when this loop stops.
     */
    public void start(final Runnable preRun, final int TARGET_TPS, final Consumer<Integer> action, final Runnable shutdownHook) {
        if (!running) {
            running = true;
        } else {
            return;
        }

        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r, threadName);
                if (!threadName.isBlank()) {
                    thread.setName(threadName);
                }
                return thread;
            });
        }

        Runnable runnable = () -> {
            try {
                long secCount = System.currentTimeMillis();
                int tps = 0;
                int tCount = 0;

                long lastTime = System.nanoTime();

                while (running) {
                    if (paused) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }

                    final long tickTime = 1_000_000_000 / TARGET_TPS;
                    long now = System.nanoTime();
                    long elapsed = now - lastTime;

                    if (System.currentTimeMillis() - secCount >= 1_000) {
                        secCount = System.currentTimeMillis();
                        tps = tCount;
                        tCount = 0;
                    }

                    if (elapsed >= tickTime) {
                        tCount++;
                        action.accept(tps);
                        lastTime += tickTime;
                    } else {
                        long sleepNanos = tickTime - elapsed;
                        try {
                            Thread.sleep(sleepNanos / 1_000_000, (int) (sleepNanos % 1_000_000));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                }
                shutdownHook.run();
            } catch (Exception e) {
                Ez2Log.error(this, "Error in loop", e);
                stop();
            }
        };

        if (runOnThread) {
            executorService.execute(preRun);
            executorService.execute(runnable);
        } else {
            preRun.run();
            runnable.run();
        }
    }

    /**
     * You can stop the loop at any time with this methode.
     * To start the loop again,
     * you have to call {@link #start(Runnable, int, Consumer, Runnable)} or {@link #start(int, Consumer)}
     */
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * Pauses the loop.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Unpauses the loop. This can take up to 20 ms.
     */
    public void resume() {
        paused = false;
    }
}
