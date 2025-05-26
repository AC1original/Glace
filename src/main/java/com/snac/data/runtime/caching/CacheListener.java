package com.snac.data.runtime.caching;

public interface CacheListener {

    void onCachedObjectExpire(Cache.CachedObject<?> object);
    void onCachedObjectRemove(Cache.CachedObject<?> object);
    void onCachedObjectAdd(Cache.CachedObject<?> object);
}
