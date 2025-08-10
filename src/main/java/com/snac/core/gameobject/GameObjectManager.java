package com.snac.core.gameobject;

import com.snac.graphics.Renderer;
import com.snac.util.HitBox;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

@Slf4j
public class GameObjectManager {
    protected final Set<AbstractObjectBase<?>> gameObjects;
    protected final Set<AbstractObjectBase<?>> tickBuffer;
    protected final HitBox posFinderHitBox;
    protected final ReentrantReadWriteLock rwLock;
    @Getter
    protected final Renderer renderer;

    public GameObjectManager(Renderer renderer) {
        this.gameObjects = Collections.synchronizedSet(new HashSet<>());
        this.tickBuffer = new HashSet<>();
        this.rwLock = new ReentrantReadWriteLock();
        this.renderer = renderer;
        this.posFinderHitBox = new HitBox(0, 0, 1, 1);

        log.info("Initialized");
    }

    public GameObjectManager addGameObject(AbstractObjectBase<?> gameObject) {
        gameObjects.add(gameObject);
        gameObject.internalCreate(this);
        renderer.getCanvas().addRenderable(gameObject);

        log.info("Added new GameObject of type '{}' with UUID '{}'",
                gameObject.getClass().getSimpleName(),
                gameObject.getUuid());

        return this;
    }

    public GameObjectManager removeGameObject(AbstractObjectBase<?> gameObject) {
        gameObjects.remove(gameObject);
        gameObject.onDestroy();
        renderer.getCanvas().removeRenderable(gameObject);

        log.info("Removed GameObject of type '{}' with UUID '{}'",
                gameObject.getClass().getSimpleName(),
                gameObject.getUuid());

        return this;
    }

    public synchronized void tick(double deltaTime) {
        rwLock.readLock().lock();
        try {
            tickBuffer.clear();
            tickBuffer.addAll(gameObjects);
        } finally {
            rwLock.readLock().unlock();
        }

        for (AbstractObjectBase<?> gameObject : gameObjects) {
            gameObject.internalUpdate(deltaTime);
        }
    }

    public synchronized List<AbstractObjectBase<?>> getObjectsAt(int x, int y) {
        posFinderHitBox.setX(x);
        posFinderHitBox.setY(y);

        rwLock.readLock().lock();
        try {
            return gameObjects
                    .stream()
                    .filter(entity -> entity.getHitBox().intersects(posFinderHitBox))
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean containsGameObjectFromUUID(UUID uuid) {
        return getGameObjectFromUUID(uuid) != null;
    }

    public boolean containsGameObject(AbstractObjectBase<?> gameObject) {
        return gameObjects.contains(gameObject);
    }

    public List<UUID> getGameObjectUuids() {
        rwLock.readLock().lock();
        try {
            return gameObjects
                    .stream()
                    .map(AbstractObjectBase::getUuid)
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Stream<AbstractObjectBase<?>> streamObjects() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<AbstractObjectBase<?>>(gameObjects).stream();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Nullable
    public AbstractObjectBase<?> getGameObjectFromUUID(UUID uuid) {
        rwLock.readLock().lock();
        try {
            return gameObjects
                    .stream()
                    .filter(gO -> gO.getUuid().equals(uuid))
                    .findFirst()
                    .orElse(null);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean collides(AbstractObjectBase<?> gameObject) {
        rwLock.readLock().lock();
        try {
            return gameObjects
                    .stream()
                    .anyMatch(gO -> gO.getHitBox().intersects(gameObject.getHitBox()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<AbstractObjectBase<?>> getCollisions(AbstractObjectBase<?> gameObject) {
        if (!collides(gameObject)) {
            return Collections.emptyList();
        }

        rwLock.readLock().lock();
        try {
            return gameObjects
                    .stream()
                    .filter(gO -> gO.getHitBox().intersects(gameObject.getHitBox()))
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<AbstractObjectBase<?>> getGameObjects() {
        return List.copyOf(gameObjects);
    }
}
