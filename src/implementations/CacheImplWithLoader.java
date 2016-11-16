package implementations;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFutureTask;

import abstracts.AbstractCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class CacheImplWithLoader extends AbstractCache {

  private LoadingCache<String, Set<Long>> cache;
  private Timer timer;

  public CacheImplWithLoader(Map<String, Set<Long>> inputData, Map<String, Set<Long>> updateData)
      throws ExecutionException {
    super(inputData, updateData);
    cache = CacheBuilder.newBuilder().build(new CacheLoader<String, Set<Long>>() {

      @Override
      public Set<Long> load(String keys) throws Exception {
        System.err.println("tried to load single: " + keys);
        throw new RuntimeException();
      }

      @Override
      public Map<String, Set<Long>> loadAll(Iterable<? extends String> keys) throws Exception {
        System.err.println("load all");
        ListenableFutureTask<Map<String, Set<Long>>> task =
            ListenableFutureTask.create(new LoadData());
        task.run();
        try {
          return task.get(2500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
          return null;
        }
      }
    });
    Set<String> strings = new HashSet<String>();
    strings.add("key1");
    strings.add("key2");
    cache.getAll(strings);

    timer = new Timer();
    timer.schedule(new TimerTask() {

      @Override
      public void run() {
        System.err.println("update");
        ListenableFutureTask<Map<String, Set<Long>>> task =
            ListenableFutureTask.create(new UpdateData());
        task.run();
        try {
          long before = System.currentTimeMillis();
          cache.putAll(task.get(2500, TimeUnit.MILLISECONDS));
          System.err.println("update: " + (System.currentTimeMillis() - before));
        } catch (Exception e) {
          System.err.println("failed to load data");
        }
      }
    }, 5000, 5000);
  }

  @Override
  public boolean hasValueForKey(String key, Long value) {
    try {
      return cache.get(key).contains(value);
    } catch (ExecutionException e) {
      e.printStackTrace();
      return false;
    }
  }

  @Override
  public void stopRefresh() {
    timer.cancel();
  }


  private class UpdateData implements Callable<Map<String, Set<Long>>> {
    @Override
    public Map<String, Set<Long>> call() throws Exception {
      // simulate delay
      Thread.sleep(2000);
      return updateData;
    }
  }

  private class LoadData implements Callable<Map<String, Set<Long>>> {
    @Override
    public Map<String, Set<Long>> call() throws Exception {
      // simulate delay
      Thread.sleep(2000);
      return inputData;
    }
  }
}
