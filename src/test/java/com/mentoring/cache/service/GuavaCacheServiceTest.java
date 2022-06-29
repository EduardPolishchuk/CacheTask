package com.mentoring.cache.service;

import com.google.common.cache.Cache;
import com.mentoring.cache.data.CacheData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuavaCacheServiceTest {


    private static GuavaCacheService testedCacheService;
    private static Cache<String, CacheData> innerCache;

    @BeforeEach
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void setUp() {
        testedCacheService = new GuavaCacheService();

        Field cacheField = testedCacheService.getClass().getDeclaredField("cache");
        cacheField.setAccessible(true);
        innerCache = (Cache<String, CacheData>) cacheField.get(testedCacheService);
    }

    @Test
    void cacheServicePutTest() {
        // Given
        CacheData data = new CacheData("Data1");

        // When

        testedCacheService.put(data);

        // Then

        assertEquals(1, innerCache.size());
        assertEquals(data, innerCache.getIfPresent("Data1"));
    }

    @Test
    void cacheServiceGetTest() {
        // Given
        CacheData data = new CacheData("Data2");
        innerCache.put("Data2",data);

        // When

        CacheData dataFromCache = testedCacheService.get("Data2").get();

        // Then

        assertEquals(data, dataFromCache);
    }
}