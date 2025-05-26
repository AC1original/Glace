package com.snac.data.runtime.caching;

import com.snac.util.Loop;
import de.snac.Ez2Log;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
public final class Cache<T> {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final LinkedHashSet<CachedObject<T>> cached = new LinkedHashSet<>();
    private final List<CacheListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final boolean expires;
    private final int expiresAfter;
    private final TimeUnit expireTimeUnit;
    private final boolean deleteAfterExpiration;
    private final boolean onlyExpireWhenUnused;
    private final boolean deleteOldIndexes;
    private final int deleteIndexAfter;

    private Cache(boolean expires,int expiresAfter,TimeUnit expireTimeUnit,boolean deleteAfterExpiration,
                  boolean onlyExpireWhenUnused, boolean deleteOldIndexes, int deleteIndexAfter) {
        this.expires = expires;
        this.expiresAfter = expiresAfter;
        this.expireTimeUnit = expireTimeUnit;
        this.deleteAfterExpiration = deleteAfterExpiration;
        this.onlyExpireWhenUnused = onlyExpireWhenUnused;
        this.deleteOldIndexes = deleteOldIndexes;
        this.deleteIndexAfter = deleteIndexAfter;
    }

    public void register(CacheListener listener) {
        listeners.add(listener);
    }

    public void unregister(CacheListener listener) {
        listeners.remove(listener);
    }

    private void tick() {
        if (isExpires()) {
            lock.readLock().lock();
            try {
                stream().filter(CachedObject::isExpired)
                        .filter(obj -> (System.currentTimeMillis() - (isOnlyExpireWhenUnused() ? obj.getLastUpdated() : obj.getTimeAdded()))
                                >= getExpireTimeUnit().toMillis(getExpiresAfter()))
                        .forEach(obj -> {
                            listeners.forEach(lstnr -> lstnr.onCachedObjectExpire(obj));
                            obj.expire();
                        });
            } finally {
                lock.readLock().unlock();
            }
        }

        if (isDeleteAfterExpiration()) {
            lock.readLock().lock();
            try {
                stream().filter(CachedObject::isExpired)
                        .forEach(this::remove);
            } finally {
                lock.readLock().unlock();
            }
        }

        if (isDeleteOldIndexes() && cached.size() > getDeleteIndexAfter()) {
            var it = cached.iterator();
            if (it.hasNext()) {
                remove(it.next());
            }
        }
    }


    public void add(String key, T object) {
        CachedObject<T> cachedObject = new CachedObject<>(key, object);
        lock.writeLock().lock();
        try {
            listeners.forEach(listener -> listener.onCachedObjectAdd(cachedObject));
            cached.add(cachedObject);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Nullable
    public T get(String key) {
        lock.readLock().lock();
        try {
            return stream().filter(obj -> obj.getKey().equals(key))
                    .findFirst()
                    .map(CachedObject::getObject)
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Nullable
    public String getKey(T object) {
        lock.readLock().lock();
        try {
            return stream().filter(obj -> obj.object.equals(object))
                    .map(CachedObject::getKey)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }


    public boolean contains(T object) {
        return getKey(object) != null;
    }

    public boolean contains(String key) {
        return get(key) != null;
    }

    public boolean contains(@NotNull CachedObject<?> cObject) {
        return contains(cObject.getKey());
    }

    public void remove(String key) {
        lock.writeLock().lock();
        try {
            stream().filter(obj -> obj.getKey().equals(key))
                    .forEach(obj -> {
                        listeners.forEach(lstnr -> lstnr.onCachedObjectRemove(obj));
                        cached.remove(obj);
                    });
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void remove(T object) {
        lock.readLock().lock();
        try {
            stream().filter(obj -> obj.object.equals(object))
                    .forEach(obj -> remove(obj.getKey()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void remove(CachedObject<?> cObject) {
        lock.writeLock().lock();
        try {
            listeners.forEach(lstnr -> lstnr.onCachedObjectRemove(cObject));
            cached.remove(cObject);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<CachedObject<T>> getCopyOfCached() {
        return new LinkedHashSet<>(cached);
    }

    public Stream<CachedObject<T>> stream() {
        lock.readLock().lock();
        try {
            return getCopyOfCached().stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void forEach(final Consumer<? super CachedObject<T>> action) {
        lock.readLock().lock();
        try {
            cached.forEach(action);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cached.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Getter
    public static class CachedObject<T> {
        private final String key;
        @Getter(AccessLevel.NONE)
        private final T object;
        private final long timeAdded;
        private long lastUpdated;
        private boolean expired;

        private CachedObject(final String key, final T object) {
            this.key = key;
            this.object = object;
            this.timeAdded = System.currentTimeMillis();
            this.lastUpdated = timeAdded;
        }

        public T getObject() {
            this.lastUpdated = System.currentTimeMillis();
            return object;
        }

        public void expire() {
            expired = true;
        }

        @Override
        public String toString() {
            return "CachedObject{" +
                    "key='" + key + '\'' +
                    ", object=" + object +
                    ", timeAdded=" + timeAdded +
                    ", lastUpdated=" + lastUpdated +
                    ", expired=" + expired +
                    '}';
        }
    }

    public static class CacheBuilder<T> {
        private static final Set<Cache<?>> caches = Collections.synchronizedSet(new HashSet<>());
        private boolean expires = false;
        private int expiresAfter = 1;
        private TimeUnit expireTimeUnit = TimeUnit.MINUTES;
        private boolean deleteAfterExpiration = false;
        private boolean onlyExpireWhenUnused = false;
        private boolean deleteOldIndexes = false;
        private int deleteIndexAfter = 100;

        static {
            tick();
        }

        private static void tick() {
            Loop.builder()
                    .runOnThread(true)
                    .threadName("Caching-Thread")
                    .build()
                    .start(20, (fps) -> {
                        synchronized (caches) {
                            caches.forEach(Cache::tick);
                        }
                    });
        }

        public CacheBuilder<T> objectsExpires(final boolean expires) {
            this.expires = expires;
            return this;
        }

        public CacheBuilder<T> objectsExpiresAfter(final int expiresAfter, final TimeUnit expireTimeUnit) {
            this.expiresAfter = expiresAfter;
            this.expireTimeUnit = expireTimeUnit;
            return this;
        }

        public CacheBuilder<T> deleteObjectsWhenExpired(final boolean deleteAfterExpiration) {
            this.deleteAfterExpiration = deleteAfterExpiration;
            return this;
        }

        public CacheBuilder<T> objectsOnlyExpiresWhenUnused(final boolean onlyExpireWhenUnused) {
            this.onlyExpireWhenUnused = onlyExpireWhenUnused;
            return this;
        }

        public CacheBuilder<T> deleteOldIndexes(final boolean deleteOldIndexes) {
            this.deleteOldIndexes = deleteOldIndexes;
            return this;
        }

        public CacheBuilder<T> deleteIndexAfter(final int deleteIndexAfter) {
            this.deleteIndexAfter = deleteIndexAfter;
            return this;
        }

        public final Cache<T> build() {
            Cache<T> cache = new Cache<T>(expires, expiresAfter, expireTimeUnit, deleteAfterExpiration, onlyExpireWhenUnused, deleteOldIndexes, deleteIndexAfter);
            caches.add(cache);
            Ez2Log.info(cache, "Initialized new cache.");
            return cache;
        }
    }
}
