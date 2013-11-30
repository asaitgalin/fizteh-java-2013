package ru.fizteh.fivt.students.asaitgalin.proxy;

import ru.fizteh.fivt.proxy.LoggingProxyFactory;

import java.io.Writer;
import java.lang.reflect.Proxy;

public class LoggingProxyFactoryImpl implements LoggingProxyFactory {

    @Override
    public Object wrap(Writer writer, Object implementation, Class<?> interfaceClass) {
        if (writer == null) {
            throw new IllegalArgumentException("proxy factory: writer is null");
        }
        if (implementation == null) {
            throw new IllegalArgumentException("proxy factory: implementation is null");
        }
        if (interfaceClass == null) {
            throw new IllegalArgumentException("proxy factory: interfaceClass is null");
        }
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("proxy factory: interfaceClass is not interface");
        }
        if (!interfaceClass.isInstance(implementation)) {
            throw new IllegalArgumentException("proxy factory: implementation does not implement interfaceClass");
        }
        return Proxy.newProxyInstance(implementation.getClass().getClassLoader(),
                new Class[]{interfaceClass}, new ProxyInvocationHandler(writer, implementation));
    }
}
