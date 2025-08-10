package com.snac.core.gameobject;

import com.snac.graphics.Renderer;
import com.snac.util.HitBox;
import com.snac.util.TryCatch;
import de.snac.Ez2Log;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
public class GameObjectManager {
    @Getter(AccessLevel.NONE)
    protected final Set<AbstractObjectBase<?>> gameObjects;
    protected final ReentrantReadWriteLock rwLock;
    protected final Renderer renderer;
    @Getter(AccessLevel.NONE)
    protected final HitBox posFinderHitBox;

    public GameObjectManager(Renderer renderer) {
        this.gameObjects = Collections.synchronizedSet(new HashSet<>());
        this.rwLock = new ReentrantReadWriteLock();
        this.renderer = renderer;
        this.posFinderHitBox = new HitBox(0, 0, 1, 1);

        Ez2Log.info(this, "Initialized");
    }

    public GameObjectManager addGameObject(AbstractObjectBase<?> gameObject) {
        try {
            rwLock.writeLock().lock();
            gameObjects.add(gameObject);
            gameObject.internalCreate(this);
            renderer.getCanvas().addRenderable(gameObject);

            Ez2Log.info(this, "Added new GameObject of type '%s' with UUID '%s'",
                    gameObject.getClass().getSimpleName(),
                    gameObject.getUuid());
        } finally {
            rwLock.writeLock().unlock();
        }
        return this;
    }

    public GameObjectManager removeGameObject(AbstractObjectBase<?> gameObject) {
        try {
            rwLock.writeLock().lock();
            gameObjects.remove(gameObject);
            gameObject.onDestroy();
            renderer.getCanvas().removeRenderable(gameObject);

            Ez2Log.info(this, "Removed GameObject of type '%s' with UUID '%s'",
                    gameObject.getClass().getSimpleName(),
                    gameObject.getUuid());
        } finally {
            rwLock.writeLock().unlock();
        }
        return this;
    }

    public void tick(double deltaTime) {
        rwLock.readLock().lock();
        try {
            for (AbstractObjectBase<?> gameObject : gameObjects) {
                gameObject.internalUpdate(deltaTime);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public synchronized List<AbstractObjectBase<?>> getObjectsAt(int x, int y) {
        posFinderHitBox.setX(x);
        posFinderHitBox.setY(y);
        return gameObjects
                .stream()
                .filter(entity -> entity.getHitBox().intersects(posFinderHitBox))
                .toList();
    }

    public boolean containsGameObjectFromUUID(UUID uuid) {
        return getGameObjectFromUUID(uuid) != null;
    }

    public boolean containsGameObject(AbstractObjectBase<?> gameObject) {
        try {
            rwLock.readLock().lock();
            return gameObjects.contains(gameObject);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<UUID> getGameObjectUuids() {
        return new ArrayList<>(gameObjects)
                .stream()
                .map(AbstractObjectBase::getUuid)
                .toList();
    }

    @Nullable
    public AbstractObjectBase<?> getGameObjectFromUUID(UUID uuid) {
        try {
            rwLock.readLock().lock();
            for (AbstractObjectBase<?> gameObject : gameObjects) {
                if (gameObject.getUuid().equals(uuid)) {
                    return gameObject;
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return null;
    }

    public boolean collides(AbstractObjectBase<?> gameObject) {
        try {
            rwLock.readLock().lock();
            for (AbstractObjectBase<?> gO : gameObjects) {
                if (gO.getHitBox().intersects(gameObject.getHitBox())) {
                    return true;
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return false;
    }

    public List<AbstractObjectBase<?>> getCollisions(AbstractObjectBase<?> gameObject) {
        if (!collides(gameObject)) {
            return Collections.emptyList();
        }

        final var collisions = new ArrayList<AbstractObjectBase<?>>();
        try {
            rwLock.readLock().lock();
            for (AbstractObjectBase<?> gO : gameObjects) {
                if (gO.getHitBox().intersects(gameObject.getHitBox())) {
                    collisions.add(gO);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return collisions;
    }

    public List<AbstractObjectBase<?>> getGameObjects() {
        return List.copyOf(gameObjects);
    }
}
