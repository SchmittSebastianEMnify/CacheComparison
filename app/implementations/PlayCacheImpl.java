package implementations;

import com.google.common.util.concurrent.ListenableFutureTask;

import abstracts.AbstractCache;
import play.cache.Cache;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PlayCacheImpl extends AbstractCache {
  private Timer timer;
  private boolean update = false;
  
  private Cache cache;

  public PlayCacheImpl(Map<String, Set<Long>> inputData, Map<String, Set<Long>> updateData) throws InterruptedException {
    super(inputData,updateData);
    initCache(cache);
  
    // ignore updateData
    loadAll(inputData);
    
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
          loadAll(task.get(2500, TimeUnit.MILLISECONDS));
          if (update) {
            System.err.println("update: " + (System.currentTimeMillis() - before) + " ms");
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
  
  private void initCache(Cache cache2) {
    this.cache = cache2;
    
  }

  @SuppressWarnings("static-access")
  private void loadAll(Map<String,Set<Long>> data) {
    try {
    data.forEach((key,value) -> cache.set(key, value));
    } catch (NullPointerException n) {
      n.printStackTrace();
    }
  }

  @SuppressWarnings("static-access")
  @Override
  public boolean hasValueForKey(String key, Long value) {
    @SuppressWarnings("unchecked")
    Set<Long> values = (Set<Long>) cache.get(key);
    
    if (values==null) return false;
    return values.contains(value);
    
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
