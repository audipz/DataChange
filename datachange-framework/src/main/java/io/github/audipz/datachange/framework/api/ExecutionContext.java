package io.github.audipz.datachange.framework.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores values produced during one run, e.g. saveAs/ref usage.
 */
public class ExecutionContext {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Object get(String key) {
        return values.get(key);
    }
}

