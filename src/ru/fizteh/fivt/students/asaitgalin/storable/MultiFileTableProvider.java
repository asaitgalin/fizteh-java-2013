package ru.fizteh.fivt.students.asaitgalin.storable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.Table;

import ru.fizteh.fivt.students.asaitgalin.storable.extensions.ExtendedTable;
import ru.fizteh.fivt.students.asaitgalin.storable.extensions.ExtendedTableProvider;
import ru.fizteh.fivt.students.asaitgalin.storable.xml.XMLReader;
import ru.fizteh.fivt.students.asaitgalin.storable.xml.XMLWriter;
import ru.fizteh.fivt.students.asaitgalin.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiFileTableProvider implements ExtendedTableProvider, AutoCloseable {
    private static final String TABLE_NAME_FORMAT = "[A-Za-zА-Яа-я0-9]+";

    private final ReadWriteLock tableProviderTransactionLock = new ReentrantReadWriteLock(true);
    private File dbDirectory;
    private Map<String, MultiFileTable> tableMap = new HashMap<>();
    private AtomicBoolean isClosed;

    public MultiFileTableProvider(File dbDirectory) throws IOException {
        if (dbDirectory == null) {
            throw new IllegalArgumentException("provider, constructor: bad db directory name");
        }
        if (!dbDirectory.isDirectory()) {
            throw new IllegalArgumentException("provider, constructor: name is not a directory");
        }
        for (File f : dbDirectory.listFiles()) {
            if (f.isFile()) {
                continue;
            }
            MultiFileTable table = new MultiFileTable(f, f.getName(), this);
            table.load();
            tableMap.put(table.getName(), table);
        }
        this.dbDirectory = dbDirectory;
        this.isClosed = new AtomicBoolean(false);

    }

    @Override
    public ExtendedTable getTable(String name) {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("provider, get: bad table name");
        }
        if (!name.matches(TABLE_NAME_FORMAT)) {
            throw new IllegalArgumentException("provider, get: table name contains bad symbols");
        }
        try {
            tableProviderTransactionLock.readLock().lock();
            MultiFileTable table = tableMap.get(name);
            if (table.isClosed()) {
                MultiFileTable other = new MultiFileTable(table);
                tableMap.put(name, other);
                return other;
            }
            return table;
        } finally {
            tableProviderTransactionLock.readLock().unlock();
        }
    }

    @Override
    public Table createTable(String name, List<Class<?>> columnTypes) throws IOException {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("provider, create: invalid name");
        }
        if (!name.matches(TABLE_NAME_FORMAT)) {
            throw new RuntimeException("provider, create: incorrect table name");
        }
        if (columnTypes == null || columnTypes.isEmpty()) {
            throw new IllegalArgumentException("provider, create: columnTypes are null or empty");
        }
        for (Class<?> cl : columnTypes) {
            if (cl == null || MultiFileTableTypes.getNameByClass(cl) == null) {
                throw new IllegalArgumentException("provider, create: invalid column type");
            }
        }
        try {
            tableProviderTransactionLock.writeLock().lock();
            File tableDir = new File(dbDirectory, name);
            if (tableDir.exists()) {
                return null;
            }
            if (!tableDir.mkdir()) {
                throw new IOException("provider, create: failed to create table directory");
            }
            MultiFileTable table = new MultiFileTable(tableDir, name, this, columnTypes);
            tableMap.put(table.getName(), table);
            return table;
        } finally {
            tableProviderTransactionLock.writeLock().unlock();
        }
    }

    @Override
    public void removeTable(String name) throws IOException {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("provider, remove: bad table name");
        }
        try {
            tableProviderTransactionLock.writeLock().lock();
            File tableDir = new File(dbDirectory, name);
            if (!tableDir.exists()) {
                throw new IllegalStateException("provider, remove: table does not exist");
            }
            tableMap.remove(name);
            FileUtils.deleteRecursively(tableDir);
        } finally {
            tableProviderTransactionLock.writeLock().unlock();
        }
    }

    @Override
    public Storeable deserialize(Table table, String value) throws ParseException {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        if (table == null || value == null) {
            throw new IllegalArgumentException("provider: table or value is null");
        }
        Storeable row = createFor(table);
        XMLReader reader = new XMLReader(value);
        for (int i = 0; i < table.getColumnsCount(); ++i) {
            row.setColumnAt(i, reader.readValue(table.getColumnType(i)));
        }
        reader.close();
        return row;
    }

    @Override
    public String serialize(Table table, Storeable value) throws ColumnFormatException {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        if (table == null || value == null) {
            throw new IllegalArgumentException("provider: table or value is null");
        }
        XMLWriter writer = new XMLWriter();
        try {
            for (int i = 0; i < table.getColumnsCount(); ++i) {
                writer.writeValue(value.getColumnAt(i));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("provider: storable does not match table", e);
        }
        writer.close();
        return writer.getString();
    }

    @Override
    public Storeable createFor(Table table) {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        List<Class<?>> columnTypes = new ArrayList<>();
        for (int i = 0; i < table.getColumnsCount(); ++i) {
            columnTypes.add(table.getColumnType(i));
        }
        return new MultiFileTableRow(columnTypes);
    }

    @Override
    public Storeable createFor(Table table, List<?> values) throws ColumnFormatException, IndexOutOfBoundsException {
        if (isClosed.get()) {
            throw new IllegalStateException("table provider is closed");
        }
        List<Class<?>> columnTypes = new ArrayList<>();
        for (int i = 0; i < table.getColumnsCount(); ++i) {
            columnTypes.add(table.getColumnType(i));
        }
        MultiFileTableRow row = new MultiFileTableRow(columnTypes);
        row.setAllColumns(values);
        return row;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), dbDirectory.getAbsolutePath());
    }

    @Override
    public void close() throws Exception {
        for (String key : tableMap.keySet()) {
            tableMap.get(key).close();
        }
        isClosed.set(true);
    }
}
