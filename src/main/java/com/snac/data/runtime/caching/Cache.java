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
import java.util.stream.Stream;

/**
 * A generic cache for storing objects of type {@code T}.
 * <p>
 * Unlike a standard list, this cache allows you to define custom logic for handling stored data,
 * helping to reduce memory usage by discarding or transforming unnecessary entries.
 * This is especially useful when working with large datasets or memory-sensitive applications.
 * <p>
 * Only important or frequently accessed data is retained in RAM, improving performance and efficiency.
 * <p>
 * Example how to use:
 * <pre>{@code
 *         Cache<Image> imageCache = new Cache.CacheBuilder<Image>()
 *                 .objectsExpire(true)
 *                 .objectsExpireAfter(5, TimeUnit.MINUTES)
 *                 .deleteObjectsWhenExpired(true)
 *                 .build();
 *
 *         imageCache.add("player_idle_image", new Image("resources/player_idle.png"));
 *
 *         Image playerIdle = imageCache.get("player_idle_image");
 * }</pre>
 *
 * @param <T> the type of objects to be stored in the cache
 */
@Getter
public class Cache<T> {
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final LinkedHashSet<CachedObject<T>> cached = new LinkedHashSet<>();
    protected final List<CacheListener> listeners = Collections.synchronizedList(new ArrayList<>());
    protected final boolean expires;
    protected final int expiresAfter;
    protected final TimeUnit expireTimeUnit;
    protected final boolean deleteAfterExpiration;
    protected final boolean onlyExpireWhenUnused;
    protected final boolean deleteOldIndexes;
    protected final int deleteIndexAfter;

    /**
     * Creates a new cache instance.
     * <p>
     * Please use {@link CacheBuilder} for constructing cache instances.
     *
     * @param expires Whether cached objects should expire automatically
     * @param expiresAfter The duration after which an object may expire
     * @param expireTimeUnit The time unit corresponding to {@code expiresAfter}
     * @param deleteAfterExpiration Whether expired objects should be automatically removed from the cache
     * @param onlyExpireWhenUnused If true, the expiration time is dependent on the last time the object got used instead of the time it got added
     * @param deleteOldIndexes Whether entries with outdated indices should be removed
     * @param deleteIndexAfter The index threshold after which old entries should be deleted
     */
    protected Cache(boolean expires, int expiresAfter, TimeUnit expireTimeUnit, boolean deleteAfterExpiration,
                  boolean onlyExpireWhenUnused, boolean deleteOldIndexes, int deleteIndexAfter) {
        this.expires = expires;
        this.expiresAfter = expiresAfter;
        this.expireTimeUnit = expireTimeUnit;
        this.deleteAfterExpiration = deleteAfterExpiration;
        this.onlyExpireWhenUnused = onlyExpireWhenUnused;
        this.deleteOldIndexes = deleteOldIndexes;
        this.deleteIndexAfter = deleteIndexAfter;
    }

    /**
     * Used to register a {@link CacheListener} for this cache.
     * @param listener The {@link CacheListener} to register
     */
    public void register(CacheListener listener) {
        listeners.add(listener);
    }

    /**
     * Used to remove a {@link CacheListener} from this cache.
     * @param listener The {@link CacheListener} to remove
     */
    public void unregister(CacheListener listener) {
        listeners.remove(listener);
    }

    /**
     * This method is called by a static {@link CacheBuilder} method,
     * which calls every tick method from every created cache 20-times a second.
     * <p>The method takes care of the cache logic. So everything works fine</p>
     * All registered {@link CacheListener} will be notified
     * if an object expires via {@link CacheListener#onCachedObjectExpire(CachedObject)}
     */
    protected void tick() {
        if (isExpires()) {
            lock.readLock().lock();
            try {
                cached.stream().filter(CachedObject::isExpired)
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
                cached.stream().filter(CachedObject::isExpired)
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

    /**
     * Add an object to the cache.
     * <p>
     * All registered {@link CacheListener} will be notified
     * if an object is getting added via {@link CacheListener#onCachedObjectAdd(CachedObject)}
     * </p>
     * @param key The unique key the object gets saved on. If the key isn't unique, objects with the same key will be overridden
     * @param object The object you want to add
     */
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

    /**
     * Get a cached object from its key.
     * @param key The key of the object
     * @return {@code null} if this key/object doesn't exist, otherwise the object of the key
     */
    @Nullable
    public T get(String key) {
        lock.readLock().lock();
        try {
            return cached.stream().filter(obj -> obj.getKey().equals(key))
                    .findFirst()
                    .map(CachedObject::getObject)
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get a key based on the object of this key.
     * @param object The object you want to know the key from
     * @return {@code null} if this object doesn't exist, otherwise the key of the object
     */
    @Nullable
    public String getKey(T object) {
        lock.readLock().lock();
        try {
            return cached.stream().filter(obj -> obj.object.equals(object))
                    .map(CachedObject::getKey)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if the cache contains a specific object.
     * @param object The object to check if it exists
     * @return {@code true} if the object is in the cache, otherwise {@code false}
     */
    public boolean contains(T object) {
        return getKey(object) != null;
    }

    /**
     * Checks if the cache contains a specific key.
     * @param key The key to check if it exists
     * @return {@code true} if the key is in the cache, otherwise {@code false}
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Checks if the cache contains a specific {@link CachedObject}
     * @param cObject The {@link CachedObject} to check if it exists
     * @return {@code true} if the {@link CachedObject} is in the cache, otherwise {@code false}
     */
    public boolean contains(@NotNull CachedObject<?> cObject) {
        return contains(cObject.getKey());
    }

    /**
     * Removes an object based on its key. Will do nothing if the given key doesn't exist.
     * <p>
     * All registered {@link CacheListener} will be notified
     * if an object is getting removed via {@link CacheListener#onCachedObjectRemove(CachedObject)}
     * </p>
     * @param key The key of the object to be removed from the cache
     */
    public void remove(String key) {
        lock.writeLock().lock();
        try {
            cached.stream().filter(obj -> obj.getKey().equals(key))
                    .forEach(obj -> {
                        listeners.forEach(lstnr -> lstnr.onCachedObjectRemove(obj));
                        cached.remove(obj);
                    });
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a specific object from the cache. Will do nothing if the object doesn't exist.
     * <p>
     * All registered {@link CacheListener} will be notified
     * if an object is getting removed via {@link CacheListener#onCachedObjectRemove(CachedObject)}
     * </p>
     * @param object The object to be removed from the cache
     */
    public void remove(T object) {
        lock.readLock().lock();
        try {
            cached.stream().filter(obj -> obj.object.equals(object))
                    .forEach(obj -> remove(obj.getKey()));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes a specific {@link CachedObject} from the cache.
     * Will do nothing if the {@link CachedObject} doesn't exist.
     * <p>
     * All registered {@link CacheListener} will be notified
     * if an {@link CachedObject} is getting removed via {@link CacheListener#onCachedObjectRemove(CachedObject)}
     * </p>
     * @param cObject The {@link CachedObject} to be removed from the cache
     */
    public void remove(CachedObject<?> cObject) {
        lock.writeLock().lock();
        try {
            listeners.forEach(lstnr -> lstnr.onCachedObjectRemove(cObject));
            cached.remove(cObject);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a {@link Stream} of all currently cached {@link CachedObject} instances.
     * <p>
     * This method allows you to inspect the cache contents, e.g., for debugging,
     * statistics, or searching for specific entries.
     * </p>
     *
     * <h3>Important Notes:</h3>
     * <ul>
     *   <li>
     *     The returned {@link CachedObject} instances are the actual cache entries,
     *     not copies.
     *     Therefore, manual modifications — such as calling {@link CachedObject#expire()} —
     *     are strongly discouraged as they directly affect the cache.
     *   </li>
     *   <li>
     *     Accessing the cached object via {@link CachedObject#getObject()} updates its
     *     last usage timestamp.
     *     This may prevent expiration if the cache is configured
     *     via {@link CacheBuilder#objectsOnlyExpireWhenUnused(boolean)} to keep recently used entries.
     *   </li>
     *   <li>
     *     The returned stream is based on a snapshot copy of the current cache contents.
     *     Subsequent modifications to the cache will not affect the stream.
     *   </li>
     * </ul>
     *
     * @return A snapshot stream of currently cached {@link CachedObject} entries
     */
    public Stream<CachedObject<T>> stream() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cached).stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Clears every {@link CachedObject} from this Cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            cached.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * This class stores any relevant data for cached objects. It is for internal use in Caches
     * @param <T> The type of the object the class instance stores
     */
    @Getter
    public static class CachedObject<T> {
        protected final String key;
        @Getter(AccessLevel.NONE)
        protected final T object;
        protected final long timeAdded;
        protected long lastUpdated;
        protected boolean expired;

        /**
         * This class is for internal use only. You don't need to create instances except you're building a cache class by your own
         * @param key The key of the stored object
         * @param object The object to store
         */
        protected CachedObject(final String key, final T object) {
            this.key = key;
            this.object = object;
            this.timeAdded = System.currentTimeMillis();
            this.lastUpdated = timeAdded;
        }

        /**
         * @return The object stored and sets the {@link #lastUpdated} time to the current time.
         */
        protected T getObject() {
            this.lastUpdated = System.currentTimeMillis();
            return object;
        }

        /**
         * By calling this, the stored object will count as expired and will maybe be deleted by the cache stored in.
         */
        protected void expire() {
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
        protected static final Set<Cache<?>> caches = Collections.synchronizedSet(new HashSet<>());
        protected boolean expires = false;
        protected int expiresAfter = 1;
        protected TimeUnit expireTimeUnit = TimeUnit.MINUTES;
        protected boolean deleteAfterExpiration = false;
        protected boolean onlyExpireWhenUnused = false;
        protected boolean deleteOldIndexes = false;
        protected int deleteIndexAfter = 100;

        static {
            tick();
        }

        protected static void tick() {
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

        public CacheBuilder<T> objectsExpire(final boolean expires) {
            this.expires = expires;
            return this;
        }

        public CacheBuilder<T> objectsExpireAfter(final int expiresAfter, final TimeUnit expireTimeUnit) {
            this.expiresAfter = expiresAfter;
            this.expireTimeUnit = expireTimeUnit;
            return this;
        }

        public CacheBuilder<T> deleteObjectsWhenExpired(final boolean deleteAfterExpiration) {
            this.deleteAfterExpiration = deleteAfterExpiration;
            return this;
        }

        public CacheBuilder<T> objectsOnlyExpireWhenUnused(final boolean onlyExpireWhenUnused) {
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
            Ez2Log.info(cache, "Initialized new cache");
            return cache;
        }
    }
}
