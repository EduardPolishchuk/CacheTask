package com.mentoring.cache;

import com.mentoring.cache.data.CacheData;
import com.mentoring.cache.service.GuavaCacheService;
import com.mentoring.cache.service.SimpleJavaCacheService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Demo {

    private static final Logger logger = LogManager.getLogger(Demo.class);

    public static void main(String[] args) {

        //################################# SIMPLE JAVA CACHE #################################

        SimpleJavaCacheService<String, CacheData> simpleCacheService = new SimpleJavaCacheService<>();
        simpleCacheService.put("SomeData1", new CacheData("SomeData1"));
        simpleCacheService.put("SomeData2", new CacheData("SomeData2"));
        simpleCacheService.put("SomeData3", new CacheData("SomeData3"));
        simpleCacheService.put("SomeData4", new CacheData("SomeData4"));
        simpleCacheService.put("SomeData5", new CacheData("SomeData5"));
        simpleCacheService.put("SomeData6", new CacheData("SomeData6"));
        simpleCacheService.put("SomeData7", new CacheData("SomeData7"));

        logger.info(simpleCacheService);

        simpleCacheService.get("SomeData1");
        simpleCacheService.get("SomeData2");
        simpleCacheService.get("SomeData3");
        simpleCacheService.get("SomeData4");
        simpleCacheService.get("SomeData5");
        simpleCacheService.get("SomeData6");
        simpleCacheService.get("SomeData7");

        logger.info(simpleCacheService);

        //################################# GUAVA CACHE #################################


        GuavaCacheService guavaCacheService = new GuavaCacheService();
        guavaCacheService.put( new CacheData("SomeData10"));
        guavaCacheService.put( new CacheData("SomeData20"));
        guavaCacheService.put( new CacheData("SomeData30"));
        guavaCacheService.put( new CacheData("SomeData40"));
        guavaCacheService.put( new CacheData("SomeData50"));
        guavaCacheService.put( new CacheData("SomeData60"));
        guavaCacheService.put( new CacheData("SomeData70"));

        logger.info(guavaCacheService);

        guavaCacheService.get("SomeData10");
        guavaCacheService.get("SomeData20");
        guavaCacheService.get("SomeData30");
        guavaCacheService.get("SomeData40");
        guavaCacheService.get("SomeData50");
        guavaCacheService.get("SomeData60");
        guavaCacheService.get("SomeData70");

        logger.info(guavaCacheService);
    }
}
