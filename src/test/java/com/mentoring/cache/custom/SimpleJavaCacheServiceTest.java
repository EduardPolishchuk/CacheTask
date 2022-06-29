package com.mentoring.cache.custom;

import com.mentoring.cache.data.CacheData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleJavaCacheServiceTest {

    private static SimpleJavaCacheService<String, CacheData> testedCacheService;

    @BeforeEach
    void setUp() {
        testedCacheService = new SimpleJavaCacheService<>();

    }

    @Test
    void name() {
        testedCacheService.put("data1", new CacheData("data1"));
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.put("data2", new CacheData("data2"));
        testedCacheService.put("data3", new CacheData("data3"));
        testedCacheService.get("data2");
        testedCacheService.put("data4", new CacheData("data4"));
        testedCacheService.put("data5", new CacheData("data5"));
    }

}