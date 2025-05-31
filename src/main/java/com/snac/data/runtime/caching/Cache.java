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
import java.util.stream.Collectors;
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
 *                 .objectsExpireAfter(5, TimeUnit.MINUTES)
 *                 .deleteObjectsWhenExpired(true)
 *                 .build();
 *
 *         imageCache.add("player_idle_image", new Image("resources/player_idle.png"));
 *
 *         Image playerIdle = imageCache.get("player_idle_image");
 * }</pre>
 *
 * @param <T> The type of objects to be stored in the cache
 */
@Getter
public class Cache<T> {
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected final LinkedHashSet<CachedObject<T>> cached = new LinkedHashSet<>();
    protected final List<CacheListener> listeners = Collections.synchronizedList(new ArrayList<>());
    protected final int expiresAfter;
    protected final TimeUnit expireTimeUnit;
    protected final boolean deleteAfterExpiration;
    protected final boolean temporalExpirationOnlyWhenUnused;
    protected final boolean oldIndexesExpire;
    protected final int indexExpireAfter;

    /**
     * Creates a new cache instance.
     * <p>
     * Please use {@link CacheBuilder} for constructing cache instances.
     *
     * @param expiresAfter                     The duration after which an object will expire. Disable with values like 0 or lower
     * @param expireTimeUnit                   The time unit corresponding to {@code expiresAfter}
     * @param deleteAfterExpiration            Whether expired objects should be automatically removed from the cache
     * @param temporalExpirationOnlyWhenUnused If true, the expiration time is dependent on the last time the object got used instead of the time it got added
     * @param oldIndexExpire                   Whether entries with outdated indexes should expire
     * @param indexExpireAfter                 The index threshold after which old entries should expire
     */
    protected Cache(int expiresAfter, TimeUnit expireTimeUnit, boolean deleteAfterExpiration,
                    boolean temporalExpirationOnlyWhenUnused, boolean oldIndexExpire, int indexExpireAfter) {
        this.expiresAfter = expiresAfter;
        this.expireTimeUnit = expireTimeUnit;
        this.deleteAfterExpiration = deleteAfterExpiration;
        this.temporalExpirationOnlyWhenUnused = temporalExpirationOnlyWhenUnused;
        this.oldIndexesExpire = oldIndexExpire;
        this.indexExpireAfter = indexExpireAfter;
    }

    /**
     * Used to register a {@link CacheListener} for this cache.
     *
     * @param listener The {@link CacheListener} to register
     */
    public void register(CacheListener listener) {
        listeners.add(listener);
    }

    /**
     * Used to remove a {@link CacheListener} from this cache.
     *
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
        if (getExpiresAfter() > 0) {
            lock.readLock().lock();
            try {
                cached.stream().filter(c -> !c.isExpired())
                        .filter(obj -> (System.currentTimeMillis() - (isTemporalExpirationOnlyWhenUnused() ? obj.getLastUpdated() : obj.getTimeAdded()))
                                >= getExpireTimeUnit().toMillis(getExpiresAfter()))
                        .forEach(CachedObject::expire);
            } finally {
                lock.readLock().unlock();
            }
        }

        if (isDeleteAfterExpiration()) {
            List<CachedObject<?>> toRemove;
            lock.readLock().lock();
            try {
                toRemove = cached.stream()
                        .filter(CachedObject::isExpired)
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }

            toRemove.forEach(this::remove);
        }


        var ueIndexes = cached.stream().filter(obj -> !obj.isExpired()).count();
        if (isOldIndexesExpire() && ueIndexes > getIndexExpireAfter()) {
            lock.readLock().lock();
            try {
                for (var delIndex = ueIndexes - getIndexExpireAfter(); delIndex > 0; delIndex-- ) {
                    cached.stream()
                            .filter(obj -> !obj.isExpired())
                            .findFirst()
                            .ifPresent(CachedObject::expire);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
    }

    /**
     * Add an object to the cache.
     * <p>
     * All registered {@link CacheListener} will be notified
     * if an object is getting added via {@link CacheListener#onCachedObjectAdd(CachedObject)}
     * </p>
     *
     * @param key    The unique key the object gets saved on. If the key isn't unique, objects with the same key will be overridden
     * @param object The object you want to add
     */
    public void add(String key, T object) {
        CachedObject<T> cachedObject = new CachedObject<>(this, key, object);
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
     *
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
     *
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
     *
     * @param object The object to check if it exists
     * @return {@code true} if the object is in the cache, otherwise {@code false}
     */
    public boolean contains(T object) {
        return getKey(object) != null;
    }

    /**
     * Checks if the cache contains a specific key.
     *
     * @param key The key to check if it exists
     * @return {@code true} if the key is in the cache, otherwise {@code false}
     */
    public boolean contains(String key) {
        return get(key) != null;
    }

    /**
     * Checks if the cache contains a specific {@link CachedObject}
     *
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
     *
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
     *
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
     *
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
     *     Accessing the cached object via {@link CachedObject#getObject()} updates its
     *     last usage timestamp.
     *     This may prevent expiration if the cache is configured
     *     via {@link CacheBuilder#temporalExpirationOnlyWhenUnused(boolean)} to keep recently used entries.
     *   </li>
     *   <li>
     *     The returned stream is based on a snapshot copy of the current cache contents.
     *     Later modifications to the cache will not affect the stream.
     *   </li>
     *   <li>Also see {@link CachedObject} for more information</li>
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
     * This class stores any relevant data for cached objects. It is for internal use in Caches.
     * <p>
     * If you want to access the stored object without possible impacts on the cache functionality,
     * you should access the stored object via the {@link #object public variable} instead of the getter.
     * For example when using {@link Cache#stream()}
     * </p>
     *
     * @param <T> The type of the object the class instance stores
     */
    @Getter
    public static class CachedObject<T> {
        protected final Cache<?> cache;
        protected final String key;
        @Getter(AccessLevel.NONE)
        public final T object;
        protected final long timeAdded;
        protected long lastUpdated;
        protected boolean expired;

        /**
         * This class is for internal use only. You don't need to create instances except you're building a cache class by your own
         *
         * @param cache  The parent cache
         * @param key    The key of the stored object
         * @param object The object to store
         */
        protected CachedObject(Cache<?> cache, String key, T object) {
            this.cache = cache;
            this.key = key;
            this.object = object;
            this.timeAdded = System.currentTimeMillis();
            this.lastUpdated = timeAdded;
        }

        /**
         * @return The object stored and set the {@link #lastUpdated} time to the current time.
         */
        public T getObject() {
            this.lastUpdated = System.currentTimeMillis();
            return object;
        }

        /**
         * By calling this, the stored object will count as expired and will maybe be deleted by the cache stored in.
         */
        protected void expire() {
            cache.listeners.forEach(lstnr -> lstnr.onCachedObjectExpire(this));
            expired = true;
        }

        /**
         * See {@link Object#toString()}
         */
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

    /**
     * This class is to build a {@link Cache} instance.
     *
     * @param <T> The type of objects to be stored in the cache
     */
    public static class CacheBuilder<T> {
        protected static final Set<Cache<?>> caches = Collections.synchronizedSet(new HashSet<>());
        protected int expiresAfter = -1;
        protected TimeUnit expireTimeUnit = TimeUnit.MINUTES;
        protected boolean deleteAfterExpiration = false;
        protected boolean temporalExpirationOnlyWhenUnused = false;
        protected int indexExpireAfter = -1;

        static {
            tick();
        }

        /**
         * This method is called once by a static initializer block to initialize the loop
         * and uses the {@link Loop} class to tick every created cache 20-times a second.
         * This is necessary to provide full cache functionality.
         */
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

        /**
         * Sets the time objects should expire automatically.
         * Use 0 or lower as {@code expiresAfter} value to disable this feature.
         *
         * @param expiresAfter   The duration after which an object may expire
         * @param expireTimeUnit The time unit corresponding to {@code expiresAfter}
         * @return this {@link CacheBuilder} instance for method chaining
         */
        public CacheBuilder<T> objectsExpireAfter(final int expiresAfter, final TimeUnit expireTimeUnit) {
            this.expiresAfter = expiresAfter;
            this.expireTimeUnit = expireTimeUnit;
            return this;
        }

        /**
         * Whether expired objects should be automatically removed from the cache.
         *
         * @param deleteAfterExpiration Set to {@code true} expired objects will be deleted automatically
         * @return this {@link CacheBuilder} instance for method chaining
         */
        public CacheBuilder<T> deleteObjectsWhenExpired(final boolean deleteAfterExpiration) {
            this.deleteAfterExpiration = deleteAfterExpiration;
            return this;
        }

        /**
         * Configures whether temporal expiration should be based on object usage rather than insertion time.
         * <p>
         * If {@code true}, cached objects will expire based on the time they were last accessed,
         * instead of the time they were added / the time set via {@link #objectsExpireAfter(int, TimeUnit)}.
         * <p>
         * This setting has no effect for index-based expiration via {@link #indexExpireAfter(int)}.
         * <p>
         * For more details on when objects are considered "used", see {@link Cache#stream()} and {@link CachedObject}.
         *
         * @param temporalExpirationOnlyWhenUnused {@code true} to enable usage-based expiration;
         *                                         {@code false} to use insertion-based expiration
         * @return this {@link CacheBuilder} instance for method chaining
         */
        public CacheBuilder<T> temporalExpirationOnlyWhenUnused(final boolean temporalExpirationOnlyWhenUnused) {
            this.temporalExpirationOnlyWhenUnused = temporalExpirationOnlyWhenUnused;
            return this;
        }

        /**
         * Sets the index threshold for automatic expiration of older cached entries.
         * <p>
         * If the given {@code indexExpireAfter} is greater than {@code 0}, older cached entries
         * will expire automatically to prevent the cache to have more entries than this value sets.
         * If the value is {@code 0} or less, index-based expiration is disabled.
         *
         * @param indexExpireAfter the number of recent indices to keep; older entries will expire automatically.
         *                         A value of {@code 0} or less disables this behavior.
         * @return this {@link CacheBuilder} instance for method chaining
         */
        public CacheBuilder<T> indexExpireAfter(final int indexExpireAfter) {
            this.indexExpireAfter = indexExpireAfter;
            return this;
        }

        /**
         * @return The {@link Cache} instance you build with this Builder.
         */
        public final Cache<T> build() {
            Cache<T> cache = new Cache<T>(
                    expiresAfter,
                    expireTimeUnit,
                    deleteAfterExpiration,
                    temporalExpirationOnlyWhenUnused,
                    indexExpireAfter > 0,
                    indexExpireAfter);
            caches.add(cache);
            Ez2Log.info(cache, "Initialized new cache");
            return cache;
        }
    }
}
