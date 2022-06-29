package com.mentoring.cache.custom;

import com.mentoring.cache.data.CacheData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SimpleJavaCacheServiceTest {

    private static SimpleJavaCacheService testedCacheService;

    @BeforeEach
    void setUp() {
        testedCacheService = new SimpleJavaCacheService();

    }

    @Test
    void name() {
        testedCacheService.put(new CacheData("data1"));
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.get("data1");
        testedCacheService.put(new CacheData("data2"));
        testedCacheService.put(new CacheData("data3"));
        testedCacheService.get("data2");
        testedCacheService.put(new CacheData("data4"));
        testedCacheService.put(new CacheData("data5"));
    }

}