package abstracts;

import java.util.Map;
import java.util.Set;

public abstract class AbstractCache {

  protected final Map<String, Set<Long>> inputData;
  protected final Map<String, Set<Long>> updateData;

  public AbstractCache(Map<String, Set<Long>> inputData, Map<String, Set<Long>> updateData) {
    this.inputData = inputData;
    this.updateData = updateData;
  }

  public abstract boolean hasValueForKey(String key, Long value);

  public abstract void stopRefresh();

}
