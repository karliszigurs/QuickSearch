package com.zigurs.karlis.utils.search.cache;

public interface CacheStatistics {

    long getHits();

    long getMisses();

    long getUncacheable();

    long getEvictions();

    boolean isEnabled();

    long getSize();

}
