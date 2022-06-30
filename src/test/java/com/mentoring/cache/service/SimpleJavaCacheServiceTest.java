package com.mentoring.cache.service;

import com.mentoring.cache.data.CacheData;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

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
    @SneakyThrows
    @SuppressWarnings("unchecked")
    void simpleCacheServiceFrequencyTest() {
        // Given

        Field cacheField = testedCacheService.getClass().getDeclaredField("prioritySortedMap");
        cacheField.setAccessible(true);
        Map<Integer, Set<SimpleJavaCacheService.CacheDataWrapper<String, CacheData>>> innerPrioritySortedMap =
                (Map<Integer, Set<SimpleJavaCacheService.CacheDataWrapper<String, CacheData>>>) cacheField.get(testedCacheService);

        CacheData expected = new CacheData("Data3");

        testedCacheService.put("Data3", expected);

        // When

        CacheData actual = testedCacheService.get("Data3").get();

        // Then

        assertEquals(1, innerPrioritySortedMap.size());
        assertEquals(
                expected,
                innerPrioritySortedMap.get(1).stream()
                        .findFirst()
                        .get()
                        .getCacheData());
    }

    @Test
    @SneakyThrows
    void simpleCacheServiceEvictSizeTest() {
        // Given When

        IntStream.range(0, MAX_SIZE + 10)
                .mapToObj(num->  new CacheData("Data_" + num))
                .forEach(cacheData -> testedCacheService.put(cacheData.getString(), cacheData));

        // Then

        assertEquals(MAX_SIZE, innerCache.size());
    }


    @Test
    @SneakyThrows
    @Disabled // Hits the performance
    void simpleCacheServiceEvictTimeTest() {
        // Given

        IntStream.range(10, MAX_SIZE + 20)
                .mapToObj(num->  new CacheData("Data_" + num))
                .forEach(cacheData -> testedCacheService.put(cacheData.getString(), cacheData));

        // When

        Thread.sleep(7000);

        // Then

        assertEquals(0, innerCache.size());
    }
}