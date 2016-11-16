package caches;

import implementations.CacheImplBasics;
import implementations.CacheImplWithLoader;
import implementations.CacheImplWithoutLoader;

import abstracts.AbstractCache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class Cache {

  private static AbstractCache cache;

  public static void create(String className, Map<String, Set<Long>> inputData,
      Map<String, Set<Long>> updateData) throws ExecutionException, InterruptedException {
    if (className.equals(CacheImplWithLoader.class.getName())) {
      cache = new CacheImplWithLoader(inputData, updateData);
    } else if (className.equals(CacheImplWithoutLoader.class.getName())) {
      cache = new CacheImplWithoutLoader(inputData, updateData);
    } else if (className.equals(CacheImplBasics.class.getName())) {
      cache = new CacheImplBasics(inputData, updateData);
    }
  }

  public static boolean hasValueForKey(String key, Long value) throws ExecutionException {
    return cache.hasValueForKey(key, value);
  }

  public static void stopRefresh() {
    cache.stopRefresh();
  }
}
