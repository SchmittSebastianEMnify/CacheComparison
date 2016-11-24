package implementations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import abstracts.AbstractCache;

import com.google.common.util.concurrent.ListenableFutureTask;


public class EhCacheImpl extends AbstractCache {
  private Cache cache;
  private Timer timer;
  private boolean update = false;
  
  public EhCacheImpl(Map<String, Set<Long>> inputData, Map<String, Set<Long>> updateData) throws InterruptedException {
    super(inputData, updateData);
    CacheManager cm = CacheManager.create();
    cm.addCache(new Cache("sample-cache", 50000, false, false, 500, 200));
    cache = cm.getCache("sample-cache");
    //cache.initialise();
    loadMaptoCache(inputData);
    
    CountDownLatch latch = new CountDownLatch(1);
    timer = new Timer();
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        ListenableFutureTask<Map<String, Set<Long>>> task =
            ListenableFutureTask.create(new LoadUpdateData());
        task.run();
        try {
          long before = System.currentTimeMillis();
          loadMaptoCache(task.get(2500, TimeUnit.MILLISECONDS));
          if (update) {
            System.err.println("update: " + (System.currentTimeMillis() - before));
          } else {
            update = true;
            latch.countDown();
          }
        } catch (Exception e) {
          System.err.println("failed to load data");
        }
      }
    }, 0, 5000);

    latch.await(2500, TimeUnit.MILLISECONDS);
    
  }
  
  private void loadMaptoCache(Map<String, Set<Long>> map) {
    Collection<Element> coll = new ArrayList<>();
    map.forEach((key,val)-> coll.add(new Element(key,val)));
    cache.putAll(coll);
  }

  @Override
  public boolean hasValueForKey(String key, Long value) {
    Element element = cache.get(key);
    if (element==null) return false;
    
    return ((Set<Long>) element.getObjectValue()).stream().anyMatch(cacheLong -> value.equals(cacheLong));
  }

  @Override
  public void stopRefresh() {
    timer.cancel();
  }

  private class LoadUpdateData implements Callable<Map<String, Set<Long>>> {
    @Override
    public Map<String, Set<Long>> call() throws Exception {
      Map<String, Set<Long>> map;
      if (update) {
        System.err.println("update");
        map = updateData;
      } else {
        System.err.println("load all");
        map = inputData;
      }
      // simulate delay
      Thread.sleep(2000);
      return map;
    }
  }

}
