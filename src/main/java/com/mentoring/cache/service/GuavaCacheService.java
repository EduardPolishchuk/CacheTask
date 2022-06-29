package com.mentoring.cache.service;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mentoring.cache.data.CacheData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Guava solution, Strategy: LRU
 *
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
public class GuavaCacheService {

    private static final Logger logger = LogManager.getLogger(GuavaCacheService.class);

    private final Cache<String, CacheData> cache;
    private static final int MAX_SIZE = 100_000;
    private static final long ACCESS_TIME = 5L; // seconds

    public GuavaCacheService() {

        RemovalListener<String, CacheData> listener = n -> {
            if (n.wasEvicted()) {
                logger.info("Item: {}, was removed.", n.getValue());
            }
        };

        cache = CacheBuilder.newBuilder()
                .removalListener(listener)
                .maximumSize(MAX_SIZE)
                .expireAfterAccess(ACCESS_TIME, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    @Nonnull
    public CacheData put(@Nonnull CacheData newValue) {
        logger.info("Put item: {}, stats: {}", newValue, cache.stats());

        cache.put(newValue.getString(), newValue);

        return newValue;
    }

    @Nonnull
    public Optional<CacheData> get(@Nonnull String key) {

        return Optional.ofNullable(cache.getIfPresent(key));
    }
}
