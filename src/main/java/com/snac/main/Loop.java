package com.snac.main;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

@Getter
public class Loop {
    private boolean running = false;
    private boolean paused = false;
    private boolean runOnThread = false;
    private String threadName = "";

    public Loop runOnThread(boolean runOnThread) {
        this.runOnThread = runOnThread;
        return this;
    }

    public Loop setThreadName(String name) {
        this.threadName = name;
        return this;
    }

    public Loop start(final int TARGET_TPS, Consumer<Integer> action) {
        if (!running) {
            running = true;
        } else {
            return this;
        }

        Runnable runnable = () -> {

            long secCount = System.currentTimeMillis();
            int tps = 0;
            int tCount = 0;

            long lastTime = System.nanoTime();

            while (true) {
                if (!running) {
                    break;
                }

                if (paused) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
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
                        Thread.sleep(sleepNanos / 1_000_000, (int)(sleepNanos % 1_000_000));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        if (runOnThread) {
            ThreadFactory namedThreadFactory = (r) -> {
                Thread thread = new Thread(r, threadName);
                if (!threadName.isBlank()) {
                    thread.setName(threadName);
                }
                return thread;
            };
            Executors.newSingleThreadExecutor(namedThreadFactory).execute(runnable);
        } else {
            runnable.run();
        }
        return this;
    }

    public void stop() {
        running = false;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }
}
