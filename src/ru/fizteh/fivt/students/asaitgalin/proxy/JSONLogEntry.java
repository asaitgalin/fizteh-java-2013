package ru.fizteh.fivt.students.asaitgalin.proxy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.IdentityHashMap;

public class JSONLogEntry {
    private JSONObject jsonObject;
    private IdentityHashMap<Object, Integer> addedObjects;

    public JSONLogEntry() {
        jsonObject = new JSONObject();
    }

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
            jsonObject.put("arguments", new JSONArray(Arrays.asList(args)));
        }
        addedObjects.clear();
    }

    public void writeThrown(Throwable throwable) {
        jsonObject.put("thrown", throwable.toString());
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
        Object resultValue = result;
        if (resultValue != null) {
            if (result instanceof Iterable) {
                resultValue = processIterable((Iterable) result);
            }
        } else {
            resultValue = JSONObject.NULL;
        }
        jsonObject.put("returnValue", resultValue);
    }

    @Override
    public String toString() {
        return jsonObject.toString(2);
    }
}
