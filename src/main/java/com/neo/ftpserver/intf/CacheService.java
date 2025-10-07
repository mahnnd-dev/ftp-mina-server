package com.neo.ftpserver.intf;

import java.util.concurrent.ConcurrentMap;

public interface CacheService<T> {
    ConcurrentMap<String, T> getCache();

    void cacheDataSync();
}
