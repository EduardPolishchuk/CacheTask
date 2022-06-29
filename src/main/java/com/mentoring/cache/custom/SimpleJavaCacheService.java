package com.mentoring.cache.custom;

import com.mentoring.cache.data.CacheData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
public class SimpleJavaCacheService {

    private static final Logger logger = LogManager.getLogger(SimpleJavaCacheService.class);

    private static final int MAX_SIZE = 100_000;
    private static final long ACCESS_TIME = 5L; // seconds
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

    private final Map<String, CacheDataWrapper> cacheMap;
    private final NavigableMap<LocalDateTime, LinkedHashSet<CacheDataWrapper>> timerMap;
    private final NavigableMap<Integer, LinkedHashSet<CacheDataWrapper>> prioritySortedMap;
    private final AtomicInteger evictCounter;
    private final AtomicLong averageTime; // milliseconds

    public SimpleJavaCacheService() {
        this.cacheMap = new HashMap<>();
        this.timerMap = new TreeMap<>();
        this.prioritySortedMap = new TreeMap<>();
        this.evictCounter = new AtomicInteger(0);
        this.averageTime = new AtomicLong(-1);
        EXECUTOR_SERVICE.execute(() -> {
            while (true){
                try {
                    System.out.println("Running");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Nonnull
    public synchronized CacheData put(@Nonnull CacheData newValue) {
        long startTime = System.currentTimeMillis();

        String newValueString = newValue.getString();
        CacheDataWrapper cacheDataWrapper = cacheMap.get(newValueString);

        if (cacheDataWrapper == null) {
            if (cacheMap.size() == MAX_SIZE) {
                evict();
            }

            int frequency = 0;
            LocalDateTime accessTime = LocalDateTime.now().plusSeconds(ACCESS_TIME);
            CacheDataWrapper newCacheDataWrapper = new CacheDataWrapper(newValue, frequency, accessTime);
            addToMap(accessTime, newCacheDataWrapper, timerMap);
            cacheMap.put(newValueString, newCacheDataWrapper);
            addToMap(frequency, newCacheDataWrapper, prioritySortedMap);
        }

        calculateAndStoreAverageTime(System.currentTimeMillis() - startTime);

        logger.info(
                "Put item: {}, stats: average put time: {}, eviction counter: {}", newValue, averageTime, evictCounter
        );

        return newValue;
    }

    @Nonnull
    public synchronized Optional<CacheData> get(@Nonnull String key) {
        CacheDataWrapper cacheDataWrapper = cacheMap.get(key);
        if (cacheDataWrapper == null) {
            return Optional.empty();
        }

        removeFromMap(cacheDataWrapper.getFrequency(), cacheDataWrapper, prioritySortedMap);
        addToMap(cacheDataWrapper.incrementAndGetFrequency(), cacheDataWrapper, prioritySortedMap);

        removeFromMap(cacheDataWrapper.getAccessTimer(), cacheDataWrapper, timerMap);
        addToMap(cacheDataWrapper.updateAndGetAccessTimer(LocalDateTime.now().plusSeconds(ACCESS_TIME)), cacheDataWrapper, timerMap);

        return Optional.ofNullable(cacheDataWrapper.getCacheData());
    }

    private <K, M extends Map<K, LinkedHashSet<CacheDataWrapper>>> void addToMap(K key, CacheDataWrapper value, M map) {
        LinkedHashSet<CacheDataWrapper> cacheDataList = map.get(key);
        if (cacheDataList == null) {
            LinkedHashSet<CacheDataWrapper> newCacheDataList = new LinkedHashSet<>();
            newCacheDataList.add(value);
            map.put(key, newCacheDataList);
        } else {
            cacheDataList.add(value);
        }
    }

    private <K, M extends Map<K, LinkedHashSet<CacheDataWrapper>>> void removeFromMap(K key, CacheDataWrapper value, M map) {
        LinkedHashSet<CacheDataWrapper> cacheDataList = map.get(key);
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
        Map.Entry<Integer, LinkedHashSet<CacheDataWrapper>> entryToRemove = prioritySortedMap.pollFirstEntry();
        entryToRemove.getValue().stream()
                .findFirst()
                .ifPresent(cacheDataWrapper -> {


                    logger.info("Item: {}, was removed.", cacheDataWrapper.getCacheData());
                    evictCounter.incrementAndGet();
                    cacheMap.remove(cacheDataWrapper.getCacheData().getString());
                    removeFromMap(cacheDataWrapper.getFrequency(), cacheDataWrapper, prioritySortedMap);
                    removeFromMap(cacheDataWrapper.getAccessTimer(), cacheDataWrapper, timerMap);
                });

    }

    static class CacheDataWrapper {
        @Nonnull
        private final CacheData cacheData;
        private int frequency;
        @Nonnull
        private LocalDateTime accessTimer;

        public CacheDataWrapper(CacheData cacheData, int frequency, LocalDateTime accessTimer) {
            this.cacheData = cacheData;
            this.frequency = frequency;
            this.accessTimer = accessTimer;
        }

        public CacheData getCacheData() {
            return cacheData;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        public LocalDateTime getAccessTimer() {
            return accessTimer;
        }

        public void setAccessTimer(LocalDateTime accessTimer) {
            this.accessTimer = accessTimer;
        }

        public int incrementAndGetFrequency() {
            return ++frequency;
        }

        public LocalDateTime updateAndGetAccessTimer(LocalDateTime accessTimer) {
            this.accessTimer = accessTimer;
            return accessTimer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheDataWrapper that = (CacheDataWrapper) o;
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
