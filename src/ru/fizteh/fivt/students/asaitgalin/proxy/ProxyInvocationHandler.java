package ru.fizteh.fivt.students.asaitgalin.proxy;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ProxyInvocationHandler implements InvocationHandler {
    private Writer writer;
    private Object implementation;

    public ProxyInvocationHandler(Writer writer, Object implementation) {
        this.writer = writer;
        this.implementation = implementation;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object returnValue = null;

        JSONLogEntry entry = new JSONLogEntry();
        entry.writeTimestamp();
        entry.writeClass(implementation.getClass());
        entry.writeMethod(method);
        entry.writeArgs(args);

        try {
            returnValue = method.invoke(implementation, args);
            if (!method.getReturnType().getName().equals("void")) {
                entry.writeReturnValue(returnValue);
            }
        } catch (InvocationTargetException ite) {
            Throwable target = ite.getTargetException();
            entry.writeThrown(target);
            throw target;
        } catch (Exception e) {
            // Do nothing
        } finally {
            try {
                // Skip Object methods
                if (!method.getDeclaringClass().equals(Object.class)) {
                    writer.write(entry.toString() + "\n");
                }
            } catch (IOException ioe) {
                // Silent mode
            }
        }

        return returnValue;
    }

}
