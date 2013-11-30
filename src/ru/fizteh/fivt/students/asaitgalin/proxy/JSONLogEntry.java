package ru.fizteh.fivt.students.asaitgalin.proxy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class JSONLogEntry {
    private JSONObject jsonObject;
    private final IdentityHashMap<Object, Integer> addedObjects;

    public JSONLogEntry() {
        addedObjects = new IdentityHashMap<>();
        jsonObject = new JSONObject();
    }

    public void writeTimestamp() {
        jsonObject = jsonObject.put("timestamp", System.currentTimeMillis());
    }

    public void writeClass(Class<?> clazz) {
        jsonObject = jsonObject.put("class", clazz.getName());
    }

    public void writeMethod(Method method) {
        jsonObject = jsonObject.put("method", method.getName());
    }

    public void writeArgs(Object[] args) {
        if (args == null) {
            jsonObject = jsonObject.put("arguments", new JSONArray());
            return;
        }
        jsonObject = jsonObject.put("arguments", new JSONArray(Arrays.asList(args)));
        addedObjects.clear();
    }

    public void writeThrown(Throwable throwable) {
        jsonObject = jsonObject.put("thrown", throwable.toString());
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
    }

    @Override
    public String toString() {
        return jsonObject.toString(2);
    }
}
