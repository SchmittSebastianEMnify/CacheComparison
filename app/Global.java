import implementations.CacheImplBasics;
import implementations.CacheImplWithLoader;
import implementations.CacheImplWithoutLoader;
import implementations.EhCacheImpl;
import implementations.PlayCacheImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.cache.NamedCache;
import play.libs.Akka;
import caches.Cache;

import com.typesafe.config.Config;

/**
 * Hello world!
 *
 */
public class Global extends GlobalSettings
{
  private final static int KEY_COUNT = 15000;
  private final static long INPUT_VALUE_COUNT = 1;
  private final static long UPDATE_VALUE_COUNT = 1000;
  private final static int CHECK_CONCURRENT_COUNT = 1000;

  private static Map<String, Set<Long>> inputData;
  private static Map<String, Set<Long>> updateData;
  
  @Override
  public void onStart(Application app) {
    
    Config config = Akka.system().settings().config();
       
    System.out.println( "Hello World!" );
    
    prepareData();
    try {
    checkCache(CacheImplWithLoader.class);

    checkCache(CacheImplWithoutLoader.class);

    checkCache(CacheImplBasics.class);
    
    checkCache(PlayCacheImpl.class);
    
    checkCache(EhCacheImpl.class);
    
    } catch (InterruptedException | ExecutionException e) {
      Logger.error(e.getMessage());
      e.printStackTrace();
    }
  
    
   
    
  }
  
//  private play.cache.Cache getCache(@NamedCache("permission-cache") play.cache.Cache cache) {
//    return cache;
//  }
//  private play.cache.Cache getUserCache(@NamedCache("user-cache") play.cache.Cache cache) {
//    return cache;
//  }
  
  private static void prepareData() {
    System.out.println("prepare data");
    inputData = new ConcurrentHashMap<String, Set<Long>>();
    for (int i = 1; i <= KEY_COUNT; i++) {
      HashSet<Long> set = new HashSet<Long>();
      for (Long j = 1L; j <= INPUT_VALUE_COUNT; j++) {
        set.add(j);
      }
      inputData.put("key" + i, set);
    }

    updateData = new ConcurrentHashMap<String, Set<Long>>();
    for (int i = 1; i <= KEY_COUNT; i++) {
      HashSet<Long> set = new HashSet<Long>();
      for (Long j = 1L; j <= UPDATE_VALUE_COUNT; j++) {
        set.add(j);
      }
      updateData.put("key" + i, set);
    }

    System.out.println("prepared data: input " + inputData.size() + " : "
        + inputData.get("key1").size() + ", update " + updateData.size() + " : "
        + updateData.get("key1").size());
  }

  private static void checkCache(Class<?> clazz) throws ExecutionException, InterruptedException {
    System.out.println("----" + clazz.getSimpleName() + "----");

    long before = System.currentTimeMillis();
    Cache.create(clazz.getName(), inputData, updateData);
    System.out.println("Creation: " + (System.currentTimeMillis() - before));

    System.out.println("Lookup");
    before = System.currentTimeMillis();
    checkValues();
    System.out.println("Lookup: " + (System.currentTimeMillis() - before));

    Thread.sleep(3500);

    System.out.println("Lookup while refreshing");
    before = System.currentTimeMillis();
    checkValues();
    System.out.println("Lookup while refreshing: " + (System.currentTimeMillis() - before));

    Thread.sleep(2000);

    System.out.println("Lookup after refreshed");
    before = System.currentTimeMillis();
    checkValues();
    System.out.println("Lookup after refreshed: " + (System.currentTimeMillis() - before));

    Thread.sleep(5000);

    checkConcurrent();

    checkConcurrent();

    Cache.stopRefresh();

    Thread.sleep(5000);

    checkConcurrent();

    checkConcurrent();
  }

  private static void checkValues() throws ExecutionException {
    System.out.println(Cache.hasValueForKey("key1", 1L));
    System.out.println(Cache.hasValueForKey("key2", 1L));
    System.out.println(Cache.hasValueForKey("key2", 2L));
    System.out.println(Cache.hasValueForKey("key" + KEY_COUNT, UPDATE_VALUE_COUNT));
  }

  private static void checkValuesSilent() throws ExecutionException {
    if (!Cache.hasValueForKey("key1", 1L) || !Cache.hasValueForKey("key2", 1L)
        || !Cache.hasValueForKey("key2", 2L)
        || !Cache.hasValueForKey("key" + KEY_COUNT, UPDATE_VALUE_COUNT)) {
      System.err.println("failed to retrieve all values");
    }
  }

  private static void checkConcurrent() throws InterruptedException {
    System.out.println("Concurrent Lookup - Thread-Count: " + CHECK_CONCURRENT_COUNT);
    CountDownLatch latch = new CountDownLatch(CHECK_CONCURRENT_COUNT);
    CountDownLatch startLatch = new CountDownLatch(CHECK_CONCURRENT_COUNT);
    List<Thread> threads = new ArrayList<Thread>();
    for (int i = 0; i < CHECK_CONCURRENT_COUNT; i++) {
      Thread thread = new Thread() {
        @Override
        public void run() {
          startLatch.countDown();
          try {
            startLatch.await();
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
          try {
            checkValuesSilent();
          } catch (ExecutionException e) {
            e.printStackTrace();
          }
          latch.countDown();
        }
      };
      threads.add(thread);
    }
    for (Thread thread : threads) {
      thread.start();
    }
    startLatch.await();
    long before = System.currentTimeMillis();
    latch.await();
    System.out.println("Concurrent Lookup: " + (System.currentTimeMillis() - before));
  }
  
  
  public Map<String,Set<Long>> generateMap(List<Perm> list) {
    Map<String,Set<Long>> map = new HashMap<>();
    list.forEach(perm -> map.put(perm.name, perm.rids));
    return map;
  }
  
  public List<Perm> generateList(int x, int y) {
    List<Perm> pList = new ArrayList<>();
    for(int i=0;i<x;i++) {
      Perm p = new Perm();
      p.name = String.valueOf(i);
      for(int j=1;j<y+1;j++) {
        p.rids.add(Long.valueOf(j));
      }
      pList.add(p);
    }
    return pList;
  }
  
  public class Perm {
    public String name;
    public Set<Long> rids = new HashSet<>();
  }
}
