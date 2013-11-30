package ru.fizteh.fivt.students.asaitgalin.storable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum MultiFileTableTypes {
    INTEGER("int", Integer.class) {
        @Override
        public Object parseValue(String s) {
            return Integer.parseInt(s);
        }
    },
    LONG("long", Long.class) {
        @Override
        public Object parseValue(String s) {
            return Long.parseLong(s);
        }
    },
    BYTE("byte", Byte.class) {
        @Override
        public Object parseValue(String s) {
            return Byte.parseByte(s);
        }
    },
    FLOAT("float", Float.class) {
        @Override
        public Object parseValue(String s) {
            return Float.parseFloat(s);
        }
    },
    DOUBLE("double", Double.class) {
        @Override
        public Object parseValue(String s) {
            return Double.parseDouble(s);
        }
    },
    BOOLEAN("boolean", Boolean.class) {
        @Override
        public Object parseValue(String s) {
            return Boolean.parseBoolean(s);
        }
    },
    STRING("String", String.class) {
        @Override
        public Object parseValue(String s) {
            return s;
        }
    };

    private final String name;
    private final Class<?> clazz;

    private MultiFileTableTypes(String name, Class<?> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    private static final Map<String, MultiFileTableTypes> NAME_TO_TYPE;
    private static final Map<Class<?>, MultiFileTableTypes> CLASS_TO_TYPE;

    static {
        Map<String, MultiFileTableTypes> tmpNameToClass = new HashMap<>();
        Map<Class<?>, MultiFileTableTypes> tmpClassToName = new HashMap<>();
        for (MultiFileTableTypes type : values()) {
            tmpNameToClass.put(type.name, type);
            tmpClassToName.put(type.clazz, type);
        }
        NAME_TO_TYPE = Collections.unmodifiableMap(tmpNameToClass);
        CLASS_TO_TYPE = Collections.unmodifiableMap(tmpClassToName);
    }

    public static String getNameByClass(Class<?> clazz) {
        MultiFileTableTypes types = CLASS_TO_TYPE.get(clazz);
        if (types == null) {
            throw new IllegalArgumentException("types: unknown type class");
        }
        return types.name;
    }

    public abstract Object parseValue(String s);

    public static Object parseValueWithClass(String s, Class<?> expectedClass) {
        MultiFileTableTypes types = CLASS_TO_TYPE.get(expectedClass);
        if (types == null) {
            throw new IllegalArgumentException("types: unknown type");
        }
        return types.parseValue(s);
    }

    public static Class<?> getClassByName(String name) {
        MultiFileTableTypes types = NAME_TO_TYPE.get(name);
        if (types == null) {
            throw new IllegalArgumentException("types: unknown type name");
        }
        return types.clazz;
    }

}
