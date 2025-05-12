package com.snac.util;

import de.snac.Ez2Log;
import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Getter
public class Loop {
    private boolean running = false;
    private boolean paused = false;
    private boolean runOnThread = false;
    private String threadName = "";

    private ExecutorService executorService;

    public Loop runOnThread(boolean runOnThread) {
        this.runOnThread = runOnThread;
        return this;
    }

    public Loop setThreadName(String name) {
        this.threadName = name;
        return this;
    }

    public Loop start(final int TARGET_TPS, Consumer<Integer> action) {
        return start(TARGET_TPS, action, () -> {
        });
    }

    public Loop start(final int TARGET_TPS, Consumer<Integer> action, Runnable shutdownHook) {
        if (!running) {
            running = true;
        } else {
            return this;
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
            executorService.execute(runnable);
        } else {
            runnable.run();
        }
        return this;
    }

    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }
}
