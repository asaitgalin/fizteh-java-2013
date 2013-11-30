package ru.fizteh.fivt.students.asaitgalin.storable;

import ru.fizteh.fivt.storage.structured.ColumnFormatException;
import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.storage.structured.TableProvider;
import ru.fizteh.fivt.students.asaitgalin.multifilehashmap.container.TableContainer;
import ru.fizteh.fivt.students.asaitgalin.storable.extensions.ExtendedTable;
import ru.fizteh.fivt.students.asaitgalin.storable.values.TableValuePackerStorable;
import ru.fizteh.fivt.students.asaitgalin.storable.values.TableValueUnpackerStorable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MultiFileTable implements ExtendedTable, AutoCloseable {
    // Underlying container
    private TableContainer<Storeable> container;

    private String name;
    private List<Class<?>> columnTypes;
    private File tableDir;
    private AtomicBoolean isClosed;

    public MultiFileTable(File tableDir, String name, TableProvider provider) {
        this.name = name;
        this.tableDir = tableDir;
        MultiFileTableSignatureWorker worker = new MultiFileTableSignatureWorker(tableDir);
        columnTypes = worker.readColumnTypes();
        this.container = new TableContainer<>(tableDir, new TableValuePackerStorable(this, provider),
                new TableValueUnpackerStorable(this, provider));
        this.isClosed = new AtomicBoolean(false);
    }

    public MultiFileTable(File tableDir, String name, TableProvider provider, List<Class<?>> columnTypes) {
        this.name = name;
        this.columnTypes = columnTypes;
        this.tableDir = tableDir;
        this.container = new TableContainer<>(tableDir, new TableValuePackerStorable(this, provider),
                new TableValueUnpackerStorable(this, provider));
        MultiFileTableSignatureWorker worker = new MultiFileTableSignatureWorker(tableDir);
        worker.writeColumnTypes(columnTypes);
        this.isClosed = new AtomicBoolean(false);
    }

    public MultiFileTable(MultiFileTable srcTable) {
        this.container = srcTable.container;
        this.name = srcTable.name;
        this.columnTypes = srcTable.columnTypes;
        this.tableDir = srcTable.tableDir;
        this.isClosed = new AtomicBoolean(false);
    }

    @Override
    public int getChangesCount() {
        return container.containerGetChangesCount();
    }

    @Override
    public String getName() {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        return name;
    }

    @Override
    public Storeable get(String key) {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        if (key == null) {
            throw new IllegalArgumentException("get: key is null");
        }
        return container.containerGetValue(key);
    }

    @Override
    public Storeable put(String key, Storeable value) throws ColumnFormatException {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        if (key == null || value == null) {
            throw new IllegalArgumentException("put: key or value is null");
        }
        if (key.matches("\\s*") || key.split("\\s+").length != 1) {
            throw new IllegalArgumentException("put: key or value is empty");
        }
        checkValue(value);
        return container.containerPutValue(key, value);
    }

    @Override
    public Storeable remove(String key) {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        if (key == null) {
            throw new IllegalArgumentException("remove: key is null");
        }
        return container.containerRemoveValue(key);
    }

    @Override
    public int size() {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        return container.containerGetSize();
    }

    @Override
    public int commit() throws IOException {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        return container.containerCommit();
    }

    @Override
    public int rollback() {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        return container.containerRollback();
    }

    @Override
    public int getColumnsCount() {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        return columnTypes.size();
    }

    @Override
    public Class<?> getColumnType(int columnIndex) throws IndexOutOfBoundsException {
        if (isClosed.get()) {
            throw new IllegalStateException("table is closed");
        }
        if (columnIndex < 0 || columnIndex >= columnTypes.size()) {
            throw new IndexOutOfBoundsException(String.format("table, getColumnType: index %d out of bounds",
                    columnIndex));
        }
        return columnTypes.get(columnIndex);
    }

    public void load() throws IOException {
        container.containerLoad();
    }

    private void tryToGetValue(Storeable st, int index, Class<?> type) throws ColumnFormatException {
        switch (type.getSimpleName()) {
            case "Integer":
                st.getIntAt(index);
                break;
            case "Long":
                st.getLongAt(index);
                break;
            case "Byte":
                st.getByteAt(index);
                break;
            case "Float":
                st.getFloatAt(index);
                break;
            case "Double":
                st.getDoubleAt(index);
                break;
            case "Boolean":
                st.getBooleanAt(index);
                break;
            case "String":
                st.getStringAt(index);
                break;
            default:
                throw new ColumnFormatException("table: wrong storable columns");
        }
    }

    private void checkValue(Storeable st) throws ColumnFormatException {
        int counter = 0;
        try {
            for (; counter < columnTypes.size(); ++counter) {
                tryToGetValue(st, counter, columnTypes.get(counter));
            }
            try {
                st.getColumnAt(counter);
                throw new ColumnFormatException("table: wrong storable columns");
            } catch (IndexOutOfBoundsException e) {
                // Check if st has more columns. If we caught this, it means that it has the same columns count.
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ColumnFormatException("table: wrong storable columns");
        }
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), tableDir.getAbsolutePath());
    }

    @Override
    public void close() throws Exception {
        rollback();
        isClosed.set(true);
    }
}
