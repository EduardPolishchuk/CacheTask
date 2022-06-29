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

    private static final int MAX_SIZE = 5;
    private static final long ACCESS_TIME = 5L; // seconds
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

    private final Map<String, CacheData> cacheMap;
    private final Map<String, Integer> counterMap;
    private final NavigableMap<LocalDateTime, CacheData> timerMap;
    private final Map<CacheData, LocalDateTime> reverseTimerMap;
    private final NavigableMap<Integer, LinkedHashSet<CacheData>> prioritySortedMap;
    private final AtomicInteger evictCounter;
    private final AtomicLong averageTime; // milliseconds

    public SimpleJavaCacheService() {
        this.cacheMap = new HashMap<>();
        this.counterMap = new HashMap<>();
        this.timerMap = new TreeMap<>();
        this.reverseTimerMap = new HashMap<>();
        this.prioritySortedMap = new TreeMap<>();
        this.evictCounter = new AtomicInteger(0);
        this.averageTime = new AtomicLong(-1);
    }

    @Nonnull
    public synchronized CacheData put(@Nonnull CacheData newValue) {
        long startTime = System.currentTimeMillis();

        String newValueString = newValue.getString();
        CacheData cacheData = cacheMap.get(newValueString);

        int frequency = 0;

        if (cacheData == null) {
            if (cacheMap.size() == MAX_SIZE) {
                evict();
            }

            cacheData = newValue;
            LocalDateTime accessTime = LocalDateTime.now().plusSeconds(ACCESS_TIME);

            timerMap.put(accessTime, cacheData);
            reverseTimerMap.put(cacheData, accessTime);
            cacheMap.put(newValueString, cacheData);
            counterMap.put(newValueString, frequency);
        }
        LinkedHashSet<CacheData> cacheDataList = prioritySortedMap.get(frequency);
        if (cacheDataList == null) {
            LinkedHashSet<CacheData> newCacheDataList = new LinkedHashSet<>();
            newCacheDataList.add(cacheData);
            prioritySortedMap.put(frequency, newCacheDataList);
        } else {
            cacheDataList.add(newValue);
        }

        calculateAndStoreAverageTime(System.currentTimeMillis() - startTime);

        logger.info(
                "Put item: {}, stats: average put time: {}, eviction counter: {}", newValue, averageTime, evictCounter
        );

        return newValue;
    }

    @Nonnull
    public synchronized Optional<CacheData> get(@Nonnull String key) {
        CacheData obtainedCacheData = cacheMap.get(key);
        if (obtainedCacheData == null) {
            return Optional.empty();
        }

        Integer frequencyFromCache = counterMap.remove(key);
        int newFrequency = frequencyFromCache + 1;
        counterMap.put(key, newFrequency);
        LocalDateTime removed = reverseTimerMap.remove(obtainedCacheData);
        LocalDateTime dateTime = LocalDateTime.now().plusSeconds(ACCESS_TIME);
        reverseTimerMap.put(obtainedCacheData, dateTime);
        LinkedHashSet<CacheData> listToRefresh = prioritySortedMap.get(frequencyFromCache);
        timerMap.remove(removed);
        timerMap.put(dateTime, obtainedCacheData);
        if (listToRefresh != null) {
            listToRefresh.remove(obtainedCacheData);
        }
        LinkedHashSet<CacheData> cacheDataList = prioritySortedMap.get(newFrequency);
        if (cacheDataList == null) {
            LinkedHashSet<CacheData> newCacheDataList = new LinkedHashSet<>();
            newCacheDataList.add(obtainedCacheData);
            prioritySortedMap.put(newFrequency, newCacheDataList);
        } else {
            cacheDataList.add(obtainedCacheData);
        }
        return Optional.ofNullable(cacheMap.get(key));
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
        Map.Entry<Integer, LinkedHashSet<CacheData>> entryToRemove = prioritySortedMap.pollFirstEntry();
        entryToRemove.getValue().stream()
                .findFirst()
                .ifPresent(cacheData -> {
                    String cacheDataString = cacheData.getString();

                    logger.info("Item: {}, was removed.", cacheData);
                    evictCounter.incrementAndGet();
                    cacheMap.remove(cacheDataString);
                    LocalDateTime removeTimer = reverseTimerMap.remove(cacheData);
                    timerMap.remove(removeTimer);

                });

    }

    static class PriorityCacheData {
        private final CacheData cacheData;
        private int frequency;

        public PriorityCacheData(CacheData cacheData, int frequency) {
            this.cacheData = cacheData;
            this.frequency = frequency;
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

        public int incrementAndGetFrequency() {
            return ++frequency;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PriorityCacheData that = (PriorityCacheData) o;
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
