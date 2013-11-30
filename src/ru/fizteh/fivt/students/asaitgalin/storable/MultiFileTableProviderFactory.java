package ru.fizteh.fivt.students.asaitgalin.storable;

import ru.fizteh.fivt.students.asaitgalin.storable.extensions.ExtendedTableProvider;
import ru.fizteh.fivt.students.asaitgalin.storable.extensions.ExtendedTableProviderFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MultiFileTableProviderFactory implements ExtendedTableProviderFactory, AutoCloseable {
    private List<MultiFileTableProvider> providers = new ArrayList<>();
    private boolean isClosed = false;

    @Override
    public ExtendedTableProvider create(String dir) throws IOException {
        if (isClosed) {
            throw new IllegalStateException("table provider factory is closed");
        }
        if (dir == null || dir.trim().isEmpty()) {
            throw new IllegalArgumentException("factory, create: directory name is invalid");
        }
        File dbDir = new File(dir);
        if (!dbDir.exists()) {
            if (!dbDir.mkdir()) {
                throw new IOException("factory, create: table provider unavailable");
            }
        } else {
            if (!dbDir.isDirectory()) {
                throw new IllegalArgumentException("factory, create: provided name is not directory");
            }
        }
        MultiFileTableProvider provider = new MultiFileTableProvider(new File(dir));
        providers.add(provider);
        return provider;
    }

    @Override
    public void close() throws Exception {
        for (MultiFileTableProvider provider : providers) {
            provider.close();
        }
        isClosed = true;
    }

}
