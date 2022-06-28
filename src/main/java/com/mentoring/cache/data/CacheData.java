package com.mentoring.cache.data;

import java.util.Objects;

public class CacheData {

    private final String string;

    public CacheData(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheData cacheData = (CacheData) o;
        return string.equals(cacheData.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(string);
    }

    @Override
    public String toString() {
        return "CacheData{" +
                "string='" + string + '\'' +
                '}';
    }
}
