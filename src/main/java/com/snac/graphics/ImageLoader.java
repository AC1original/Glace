package com.snac.graphics;

import com.snac.data.runtime.caching.Cache;
import de.snac.Ez2Log;

import java.util.concurrent.TimeUnit;

public abstract class ImageLoader<I> {
    protected final Cache<I> cachedImages;

    public ImageLoader() {
        this(new Cache.CacheBuilder<I>()
                .objectsExpireAfter(7, TimeUnit.MINUTES)
                .temporalExpirationOnlyWhenUnused(true)
                .deleteObjectsWhenExpired(true)
                .build());
    }

    public ImageLoader(Cache<I> imageCache) {
        this.cachedImages = imageCache;
    }

    public I getCachedOrLoad(String path) {
        return getCachedOrLoad(path, String.valueOf(path.hashCode()));
    }

    public I getCachedOrLoad(I image, String name) {
        if (cachedImages.contains(name)) {
            return cachedImages.get(name);
        }
        return cache(image, name);
    }

    public I getCachedOrLoad(String path, String name) {
        if (cachedImages.contains(name)) {
            return cachedImages.get(name);
        }
        return cache(load(path), name);
    }

    public I getCached(String name) {
        if (cachedImages.contains(name)) {
            return cachedImages.get(name);
        }
        return getFallback();
    }

    public I cache(I image, String name) {
        cachedImages.add(name, image);
        Ez2Log.info(this, "Cached image '" + name + "'");
        return image;
    }

    public abstract I load(String path);

    public abstract I getFallback();
}
