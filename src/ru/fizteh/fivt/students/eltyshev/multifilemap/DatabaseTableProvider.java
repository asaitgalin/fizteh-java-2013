package ru.fizteh.fivt.students.eltyshev.multifilemap;

import ru.fizteh.fivt.storage.strings.*;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseTableProvider implements TableProvider {
    private static final String CHECK_EXPRESSION = "[^0-9A-Za-zА-Яа-я-_\\(\\)\\[\\]\\{\\}]";

    HashMap<String, MultifileTable> tables = new HashMap<String, MultifileTable>();
    private String databaseDirectoryPath;
    private MultifileTable activeTable = null;

    public DatabaseTableProvider(String databaseDirectoryPath) {
        if (databaseDirectoryPath == null) {
            throw new IllegalArgumentException("database directory cannot be null");
        }
        this.databaseDirectoryPath = databaseDirectoryPath;
        File databaseDirectory = new File(databaseDirectoryPath);
        for (final File tableFile : databaseDirectory.listFiles()) {
            if (tableFile.isFile()) {
                continue;
            }
            MultifileTable table = new MultifileTable(databaseDirectoryPath, tableFile.getName());
            tables.put(table.getName(), table);
        }
    }

    public Table getTable(String name) throws IllegalArgumentException, IllegalStateException {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("table's name cannot be null");
        }
        MultifileTable table = tables.get(name);

        if (table == null) {
            return table;
        }

        if (activeTable != null && activeTable.getUncommitedChangesCount() > 0) {
            throw new IllegalStateException(String.format("%d unsaved changes", activeTable.getUncommitedChangesCount()));
        }

        activeTable = table;
        return table;
    }

    public Table createTable(String name) throws IllegalArgumentException, IllegalStateException {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("table's name cannot be null");
        }

        checkTableName(name);

        if (tables.containsKey(name)) {
            return null;
        }

        MultifileTable table = new MultifileTable(databaseDirectoryPath, name);
        tables.put(name, table);
        return table;
    }

    public void removeTable(String name) throws IllegalArgumentException, IllegalStateException {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("table's name cannot be null");
        }

        if (!tables.containsKey(name)) {
            throw new IllegalStateException(String.format("%s not exists", name));
        }

        tables.remove(name);

        File tableFile = new File(databaseDirectoryPath, name);
        MultifileMapUtils.deleteFile(tableFile);
    }

    private void checkTableName(String name) {
        Pattern pattern = Pattern.compile(CHECK_EXPRESSION);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            throw new IllegalArgumentException("bad symbol in table's name");
        }
    }
}
