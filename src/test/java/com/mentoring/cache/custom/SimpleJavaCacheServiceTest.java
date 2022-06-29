package com.mentoring.cache.custom;

import com.mentoring.cache.data.CacheData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimpleJavaCacheServiceTest {

    private static final int MAX_SIZE = 3;
    private static final long ACCESS_TIME = 5L;
    private static SimpleJavaCacheService<String, CacheData> testedCacheService;
    private static Map<String, SimpleJavaCacheService.CacheDataWrapper<String, CacheData>> innerCache;

    @BeforeEach
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void setUp() {
        testedCacheService = new SimpleJavaCacheService<>(MAX_SIZE, ACCESS_TIME);

        Field cacheField = testedCacheService.getClass().getDeclaredField("cacheMap");
        cacheField.setAccessible(true);
        innerCache = (Map<String, SimpleJavaCacheService.CacheDataWrapper<String, CacheData>>) cacheField.get(testedCacheService);
    }

    @Test
    void simpleCacheServicePutTest() {
        // Given

        CacheData data1 = new CacheData("Data1");

        // When

        testedCacheService.put("Data1", data1);

        // Then

        assertEquals(1, innerCache.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void simpleCacheServiceGetTest() {
        // Given

        CacheData expected = new CacheData("Data2");
        SimpleJavaCacheService.CacheDataWrapper<String, CacheData> wrapperMock =
                (SimpleJavaCacheService.CacheDataWrapper<String, CacheData>) mock(SimpleJavaCacheService.CacheDataWrapper.class);
        when(wrapperMock.getCacheData()).thenReturn(expected);

        innerCache.put("Data2", wrapperMock);

        // When

        CacheData actual = testedCacheService.get("Data2").get();

        // Then

        assertEquals(expected, actual);
    }

    @Test
    void simpleCacheServiceFrequencyTest() {
        // Given

        CacheData expected = new CacheData("Data3");

        testedCacheService.put("Data3", expected);

        // When

        CacheData actual = testedCacheService.get("Data3").get();

        // Then

        assertEquals(expected, actual);
    }
}