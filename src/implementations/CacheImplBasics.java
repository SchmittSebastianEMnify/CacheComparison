package implementations;

import com.google.common.util.concurrent.ListenableFutureTask;

import abstracts.AbstractCache;

import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class CacheImplBasics extends AbstractCache {

  private ConcurrentHashMap<String, Set<Long>> cache;
  private Timer timer;
  private boolean update = false;

  public CacheImplBasics(Map<String, Set<Long>> inputData, Map<String, Set<Long>> updateData)
      throws ExecutionException, InterruptedException {
    super(inputData, updateData);
    cache = new ConcurrentHashMap<String, Set<Long>>();

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
          cache.putAll(task.get(2500, TimeUnit.MILLISECONDS));
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

  @Override
  public boolean hasValueForKey(String key, Long value) {
    return cache.get(key).contains(value);
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
