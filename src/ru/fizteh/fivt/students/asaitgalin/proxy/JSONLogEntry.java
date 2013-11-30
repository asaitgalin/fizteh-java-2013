package ru.fizteh.fivt.students.asaitgalin.proxy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class JSONLogEntry {
    private JSONObject jsonObject = new JSONObject();
    private final IdentityHashMap<Object, Integer> addedObjects = new IdentityHashMap<>();

    public void writeTimestamp() {
        jsonObject.put("timestamp", System.currentTimeMillis());
    }

    public void writeClass(Class<?> clazz) {
        jsonObject.put("class", clazz.getName());
    }

    public void writeMethod(Method method) {
        jsonObject.put("method", method.getName());
    }

    public void writeArgs(Object[] args) {
        if (args == null) {
            jsonObject.put("arguments", new JSONArray());
        } else {
            jsonObject.put("arguments", processIterable(Arrays.asList(args)));
        }
        addedObjects.clear();
    }

    public void writeThrown(Throwable throwable) {
        jsonObject.put("thrown", throwable.toString());
    }

    public void writeReturnValue(Object result) {
        Object resultValue;
        if (result != null) {
            if (result instanceof Iterable) {
                resultValue = processIterable((Iterable) result);
            } else {
                resultValue = result;
            }
        } else {
            resultValue = JSONObject.NULL;
        }
        jsonObject = jsonObject.put("returnValue", resultValue);
        addedObjects.clear();
    }

    private JSONArray processIterable(Iterable iterable) {
        JSONArray result = new JSONArray();
        for (Object o : iterable) {
            if (o == null) {
                result.put(o);
            } else if (o.getClass().isArray()) {
                result.put(o.toString());
            } else if (o instanceof Iterable) {
                Iterable container = (Iterable) o;
                boolean isEmpty = !container.iterator().hasNext();
                if (addedObjects.containsKey(o) && !isEmpty) {
                    result.put("cyclic");
                    continue;
                }
                result.put(processIterable(container));
            }
            addedObjects.put(o, 0);
            result.put(o);
        }
        return result;
    }

    @Override
    public String toString() {
        return jsonObject.toString(2);
    }
}
