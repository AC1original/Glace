package com.snac.core;

import com.snac.core.gameobject.GameObjectManager;
import com.snac.graphics.ImageLoader;
import com.snac.graphics.Renderer;
import com.snac.graphics.impl.SwingImageLoader;
import com.snac.graphics.impl.SwingRenderer;
import com.snac.util.Loop;
import de.snac.Ez2Log;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public final class Glace {
    public static final Glace INSTANCE = new Glace();

    private ImageLoader<?> imageLoader;
    private Renderer renderer;
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
        Ez2Log.info(this, "Initialized");
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
        loop.start(() -> Ez2Log.info(this, "Starting game loop"),
                20,
                (fps, deltaTime) -> {
                    tick(deltaTime);
                    this.currentGameLoopFPS = fps;
                },
                () -> {
                    Ez2Log.warn(this, "Game loop stopped. On purpose, bug or just skill issue?");
                    shutdownHooks.forEach(Runnable::run);
                });
    }

    public long getRuntime(ChronoUnit unit) {
        return unit.between(startTime, LocalDateTime.now());
    }
}
