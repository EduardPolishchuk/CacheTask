package com.mentoring.cache.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple java solution, Strategy: LFU
 * <p>
 * Requirements:
 * <p>
 * <p> * Max Size = 100 000;
 * <p> * Eviction policy;
 * <p> * Time-based on last access (5 seconds);
 * <p> * Removal listener;
 * <p> * Just add to log of removed entry;
 * <p> * Give statistic to user;
 * <p> * Average time spent for putting new values into the cache;
 * <p> * Number of cache evictions;
 * <p> * Support concurrency.
 */
public class SimpleJavaCacheService<K, V> {

    private static final Logger logger = LogManager.getLogger(SimpleJavaCacheService.class);

    private static final int DEFAULT_MAX_SIZE = 100_000;
    private static final long DEFAULT_ACCESS_TIME = 5L; // seconds
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

    private final ConcurrentMap<K, CacheDataWrapper<K, V>> cacheMap;
    private final NavigableMap<LocalDateTime, Set<CacheDataWrapper<K, V>>> timerMap;
    private final NavigableMap<Integer, Set<CacheDataWrapper<K, V>>> prioritySortedMap;
    private final AtomicInteger evictCounter;
    private final AtomicLong averageTime; // milliseconds
    private final int maxSize;
    private final long accessTime; // seconds

    public SimpleJavaCacheService() {
        this(DEFAULT_MAX_SIZE, DEFAULT_ACCESS_TIME);
    }

    SimpleJavaCacheService(int maxSize, long accessTime) {
        this.cacheMap = new ConcurrentHashMap<>();
        this.timerMap = new ConcurrentSkipListMap<>();
        this.prioritySortedMap = new ConcurrentSkipListMap<>();
        this.evictCounter = new AtomicInteger(0);
        this.averageTime = new AtomicLong(-1);
        this.maxSize = maxSize;
        this.accessTime = accessTime;

        EXECUTOR_SERVICE.execute(() -> {
            while (true) {
                try {
                    if (refresh()) {
                        continue;
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Nonnull
    public V put(@Nonnull K key,
                 @Nonnull V newValue) {
        long startTime = System.currentTimeMillis();

        CacheDataWrapper<K, V> cacheDataWrapper = cacheMap.get(key);

        if (cacheDataWrapper == null) {
            if (cacheMap.size() == maxSize) {
                evict();
            }

            int frequency = 0;
            LocalDateTime accessTime = LocalDateTime.now().plusSeconds(this.accessTime);
            CacheDataWrapper<K, V> newCacheDataWrapper = new CacheDataWrapper<>(newValue, frequency, accessTime, key);
            cacheMap.put(key, newCacheDataWrapper);
            addToMap(accessTime, newCacheDataWrapper, timerMap);
            addToMap(frequency, newCacheDataWrapper, prioritySortedMap);
        }

        calculateAndStoreAverageTime(System.currentTimeMillis() - startTime);

        logger.info(
                "Put item: {}, stats: average put time: {}, eviction counter: {}", newValue, averageTime, evictCounter
        );

        return newValue;
    }

    @Nonnull
    public synchronized Optional<V> get(@Nonnull K key) {
        CacheDataWrapper<K, V> cacheDataWrapper = cacheMap.get(key);
        if (cacheDataWrapper == null) {
            return Optional.empty();
        }

        removeFromMap(cacheDataWrapper.getFrequency(), cacheDataWrapper, prioritySortedMap);
        addToMap(cacheDataWrapper.incrementAndGetFrequency(), cacheDataWrapper, prioritySortedMap);

        removeFromMap(cacheDataWrapper.getAccessTimer(), cacheDataWrapper, timerMap);
        addToMap(cacheDataWrapper.updateAndGetAccessTimer(LocalDateTime.now().plusSeconds(accessTime)), cacheDataWrapper, timerMap);

        return Optional.ofNullable(cacheDataWrapper.getCacheData());
    }

    private <E, M extends Map<E, Set<CacheDataWrapper<K, V>>>> void addToMap(E key, CacheDataWrapper<K, V> value, M map) {
        if (key == null || value == null || map == null) {
            return;
        }

        Set<CacheDataWrapper<K, V>> cacheDataList = map.get(key);
        if (cacheDataList == null) {
            Set<CacheDataWrapper<K, V>> newCacheDataList = ConcurrentHashMap.newKeySet();
            newCacheDataList.add(value);
            map.put(key, newCacheDataList);
        } else {
            cacheDataList.add(value);
        }
    }

    private <E, M extends Map<E, Set<CacheDataWrapper<K, V>>>> void removeFromMap(E key, CacheDataWrapper<K, V> value, M map) {
        if (key == null || value == null || map == null) {
            return;
        }

        Set<CacheDataWrapper<K, V>> cacheDataList = map.get(key);
        if (cacheDataList != null) {
            cacheDataList.remove(value);
            if (cacheDataList.size() == 0) {
                map.remove(key);
            }
        }
    }

    private void calculateAndStoreAverageTime(long time) {
        this.averageTime.accumulateAndGet(
                time,
                (oldValue, newValue) -> oldValue == -1
                        ? newValue
                        : (oldValue + newValue) / 2
        );
    }

    private void evict() {
        Map.Entry<Integer, Set<CacheDataWrapper<K, V>>> entryToRemove = prioritySortedMap.pollFirstEntry();
        entryToRemove.getValue().stream()
                .findFirst()
                .ifPresent(cacheDataWrapper -> {


                    logger.info("Item: {}, was removed.", cacheDataWrapper.getCacheData());
                    evictCounter.incrementAndGet();
                    cacheMap.remove(cacheDataWrapper.getKey());
                    removeFromMap(cacheDataWrapper.getFrequency(), cacheDataWrapper, prioritySortedMap);
                    removeFromMap(cacheDataWrapper.getAccessTimer(), cacheDataWrapper, timerMap);
                });

    }

    private boolean refresh() {
        Map.Entry<LocalDateTime, Set<CacheDataWrapper<K, V>>> entryToRemove =
                timerMap.firstEntry();

        if (entryToRemove == null || entryToRemove.getKey().isAfter(LocalDateTime.now())) {
            return false;
        }

        for (CacheDataWrapper<K, V> cacheDataWrapper : entryToRemove.getValue()) {
            removeFromMap(cacheDataWrapper.getFrequency(), cacheDataWrapper, prioritySortedMap);
            removeFromMap(cacheDataWrapper.getAccessTimer(), cacheDataWrapper, timerMap);
            cacheMap.remove(cacheDataWrapper.getKey());
        }

        return timerMap.remove(entryToRemove.getKey()) != null;
    }

    @Override
    public String toString() {
        return "SimpleJavaCacheService{" +
                "cacheMap=" + cacheMap +
                ", evictCounter=" + evictCounter +
                ", averageTime=" + averageTime +
                '}';
    }

    static class CacheDataWrapper<K, V> {
        @Nonnull
        private final V cacheData;
        @Nonnull
        private final AtomicInteger frequency;
        @Nonnull
        private volatile LocalDateTime accessTimer;
        @Nonnull
        private final K key;

        public CacheDataWrapper(@Nonnull V cacheData,
                                int frequency,
                                @Nonnull LocalDateTime accessTimer,
                                @Nonnull K key) {
            this.cacheData = cacheData;
            this.frequency = new AtomicInteger(frequency);
            this.accessTimer = accessTimer;
            this.key = key;
        }

        public V getCacheData() {
            return cacheData;
        }

        public int getFrequency() {
            return frequency.get();
        }

        @Nonnull
        public K getKey() {
            return key;
        }

        public LocalDateTime getAccessTimer() {
            return accessTimer;
        }

        public int incrementAndGetFrequency() {
            return frequency.incrementAndGet();
        }

        public LocalDateTime updateAndGetAccessTimer(LocalDateTime accessTimer) {
            this.accessTimer = accessTimer;
            return accessTimer;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheDataWrapper<K, V> that = (CacheDataWrapper<K, V>) o;
            return Objects.equals(cacheData, that.cacheData);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cacheData);
        }

        @Override
        public String toString() {
            return "PriorityCacheData{" +
                    "cacheData=" + cacheData +
                    ", frequency=" + frequency +
                    '}';
        }
    }
}
