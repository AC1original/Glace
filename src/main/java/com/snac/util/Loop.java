package com.snac.util;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * This class is used to create (Game-)Loops with a specific tps frequency
 */
@Getter
@Slf4j
public class Loop {
    protected boolean running = false;
    protected boolean paused = false;
    protected final boolean runOnThread;
    protected final String threadName;
    protected ExecutorService executorService;
    protected final List<BiConsumer<Integer, Double>> joinedActions;
    protected final ReentrantReadWriteLock rwLock;
    protected double deltaTime = 0;

    /**
     * There are two ways to create a new Loop instance:
     * <p>
     *  1. Use this contractor
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
    protected Loop(boolean runOnThread, @Nullable String threadName) {
        this.runOnThread = runOnThread;
        this.threadName = threadName == null ? "" : threadName;
        this.joinedActions = Collections.synchronizedList(new ArrayList<>());
        this.rwLock = new ReentrantReadWriteLock();
    }

    /**
     * Shorter version of {@link #start(Runnable, int, BiConsumer, Runnable)}
     * @param TARGET_TPS The maximum tps this loop should tick. If you're confused or something just type 60 but NEVER 0 or lower
     * @param action This consumer gets called every tick with the current fps and delta time
     */
    public void start(int TARGET_TPS, BiConsumer<Integer, Double> action) {
        start(() -> {}, TARGET_TPS, action, () -> {});
    }

    /**
     * Everything ready? To start the loop, you'll need to call this method.
     * <p>
     * Example how to use:
     * <pre>{@code
     * var loop = Loop.builder().runOnThread(true).build();
     *
     * loop.start(() -> System.out.println("This gets called before loop"),
     *          60,
     *          (currentFPS, deltaTime) -> render(),
     *          () -> System.out.println("This gets called after loop"));
     * }</pre>
     * @param preRun This runnable gets called before the loop starts.
     *               Used to run something on the same thread as this loop.
     *               IDK if this is useful.
     * @param TARGET_TPS The maximum tps this loop should tick. If you're confused or something just type 60 but NEVER 0 or lower
     * @param action This consumer gets called every tick with the current fps and delta time
     * @param shutdownHook This runnable gets called when this loop stops.
     */
    public void start(Runnable preRun, int TARGET_TPS, BiConsumer<Integer, Double> action, Runnable shutdownHook) {
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
                    double deltaTime = elapsed / 1_000_000_000.0;
                    this.deltaTime = deltaTime;

                    if (System.currentTimeMillis() - secCount >= 1_000) {
                        secCount = System.currentTimeMillis();
                        tps = tCount;
                        tCount = 0;
                    }

                    if (elapsed >= tickTime) {
                        tCount++;
                        action.accept(tps, deltaTime);
                        try {
                            rwLock.readLock().lock();
                            for (var eAction : joinedActions) {
                                eAction.accept(tps, deltaTime);
                            }
                        } finally {
                            rwLock.readLock().unlock();
                        }
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
                log.error("Error in loop: {}", e.toString());
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
     * This methode joins the loop running on a specific Loop-instance.
     * The {@link BiConsumer} gets called same as the consumer in the {@link #start(int, BiConsumer) start method}.
     * @param action This consumer gets called every tick with the current fps and delta time
     */
    public void join(BiConsumer<Integer, Double> action) {
        joinedActions.add(action);
        log.info("Action joined");
    }

    /**
     * You can stop the loop at any time with this methode.
     * To start the loop again,
     * you have to call {@link #start(Runnable, int, BiConsumer, Runnable)} or {@link #start(int, BiConsumer)}
     */
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }

        log.info("Stopped");
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
