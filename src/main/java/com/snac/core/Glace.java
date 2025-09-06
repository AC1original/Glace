package com.snac.core;

import com.snac.core.gameobject.GameObjectManager;
import com.snac.graphics.ImageLoader;
import com.snac.graphics.Renderer;
import com.snac.graphics.impl.SwingImageLoader;
import com.snac.graphics.impl.SwingRenderer;
import com.snac.util.Loop;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Slf4j
public final class Glace {
    public static final Glace INSTANCE = new Glace();

    private ImageLoader<?> imageLoader;
    private Renderer<?, ?> renderer;
    private GameObjectManager objectManager;

    private final Loop loop;
    private final LocalDateTime startTime;
    @Setter(AccessLevel.NONE)
    private int currentGameLoopFPS = 0;
    private final Set<Runnable> shutdownHooks;

    public void init() {
        imageLoader = new SwingImageLoader();
        renderer = new SwingRenderer();
        objectManager = new GameObjectManager(renderer);

        startGameLoop();
        log.info("Initialized");
    }

    private Glace() {
        startTime = LocalDateTime.now();
        shutdownHooks = Collections.synchronizedSet(new HashSet<>());

        loop = Loop.builder()
                .runOnThread(true)
                .threadName("Glace-main")
                .build();
    }

    public void tick(double deltaTime) {
        objectManager.tick(deltaTime);
    }

    private void startGameLoop() {
        loop.start(() -> log.info("Starting game loop"),
                20,
                (fps, deltaTime) -> {
                    tick(deltaTime);
                    this.currentGameLoopFPS = fps;
                },
                () -> {
                    log.warn("Game loop stopped. On purpose? Bug? Or just skill issue?");
                    shutdownHooks.forEach(Runnable::run);
                });
    }

    public long getRuntime(ChronoUnit unit) {
        return unit.between(startTime, LocalDateTime.now());
    }
}
