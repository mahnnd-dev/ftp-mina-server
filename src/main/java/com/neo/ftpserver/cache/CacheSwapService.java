package com.neo.ftpserver.cache;

import com.neo.ftpserver.intf.CacheService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public abstract class CacheSwapService<T> implements CacheService<T> {
    //** AtomicReference thuộc generic

    private final AtomicReference<ConcurrentHashMap<String, T>> activeCache = new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, T>> stagingCache = new AtomicReference<>(new ConcurrentHashMap<>());

    // Phương thức trừu tượng để lấy dữ liệu từ cơ sở dữ liệu
    protected abstract ConcurrentHashMap<String, T> fetchDataFromDB();

    @Override
    public ConcurrentHashMap<String, T> getCache() {
        return activeCache.get();
    }

    public T getObject(String key) {
        return activeCache.get().get(key);
    }

    public void cacheDataSync() {
        ConcurrentHashMap<String, T> currentStaging = stagingCache.get();
        try {
            Instant startTime = Instant.now();
            log.info(">> Start cache update for {}", this.getClass().getSimpleName());
            // Fetch new data
            ConcurrentHashMap<String, T> newData = fetchDataFromDB();
            // Validate before update
            if (newData == null || newData.isEmpty()) {
                int currentSize = activeCache.get().size();
                log.warn(">> Fetched data is empty/null, keeping current cache (size: {})", currentSize);
                return;
            }
            // Prepare staging buffer
            currentStaging.clear();
            currentStaging.putAll(newData);
            int newStagingSize = currentStaging.size();
            // ATOMIC SWAP - Double buffering magic happens here
            ConcurrentHashMap<String, T> previousActive = activeCache.getAndSet(currentStaging);
            stagingCache.set(previousActive);
            // Log with accurate sizes (captured before potential race conditions)
            long duration = Duration.between(startTime, Instant.now()).toMillis();
            log.info(">> Cache swapped successfully for {}: {} → {} items, time: {}ms", this.getClass().getSimpleName(), previousActive.size(), newStagingSize, duration);
        } catch (Exception e) {
            // Capture size before logging to avoid potential race
            int currentSize = activeCache.get().size();
            log.error(">> Cache update failed for {}, keeping current cache (size: {})", this.getClass().getSimpleName(), currentSize, e);
        }
    }

    // Thêm methods hữu ích
    public int getCacheSize() {
        return activeCache.get().size();
    }

    public boolean containsKey(String key) {
        return activeCache.get().containsKey(key);
    }

    // Method để force refresh cache
    @PostConstruct
    public void forceRefresh() {
        log.info(">> Force refresh cache for {}", this.getClass().getSimpleName());
        cacheDataSync();
    }
}