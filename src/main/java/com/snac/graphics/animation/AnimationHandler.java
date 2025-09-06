package com.snac.graphics.animation;

import com.snac.graphics.Canvas;
import com.snac.graphics.Renderer;
import com.snac.util.TryCatch;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class AnimationHandler<I> {
    private final List<Animation<I>> animations;
    @Getter
    @Setter
    private Canvas<?, ?> canvas;
    private final ReentrantReadWriteLock lock;

    public AnimationHandler(Renderer<?, ?> renderer) {
        this.animations = Collections.synchronizedList(new ArrayList<>());
        this.canvas = renderer.getCanvas();
        this.lock = new ReentrantReadWriteLock();

        log.info("Initialized");
    }

    public void play(Animation<I> animation) {
        if (!animation.checkValidation()) {
            log.warn("Couldn't play animation {}. Animation validation failed!", animation.getClass().getSimpleName());
        }
        animations.add(animation);
        canvas.addRenderable(animation);
        animation.onPlay();
        log.info("Animation {} started", animation.getClass().getSimpleName());
    }

    public void stopByClass(Class<? extends Animation<I>> animationClass) {
        List<Animation<I>> snapshot;
        synchronized (animations) {
            snapshot = new ArrayList<>(animations);
            snapshot.stream()
                    .filter(animation -> animation.getClass().equals(animationClass))
                    .forEach(this::stop);
        }
    }

    public void stop(Animation<I> animation) {
        if (animations.contains(animation)) {
            animations.remove(animation);
            canvas.removeRenderable(animation);
            animation.onStop();
        }
        log.info("Animation {} stopped", animation.getClass().getSimpleName());
    }

    public List<Animation<?>> getAnimations() {
        return List.copyOf(animations);
    }

    public void tick() {
        lock.readLock().lock();
        TryCatch.tryFinally(() -> {
            animations.forEach(Animation::updateIndex);
        }, () -> lock.readLock().unlock());
    }
}
